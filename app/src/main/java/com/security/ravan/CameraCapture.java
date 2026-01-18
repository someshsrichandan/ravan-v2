package com.security.ravan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CameraCapture {
    private static final String TAG = "CameraCapture";

    private Context context;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private byte[] capturedImageData;
    private CountDownLatch captureLatch;
    private String lastError;

    public CameraCapture(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

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
                    } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                        facingStr = "External";
                    }
                }

                // Get largest JPEG size
                android.util.Size[] jpegSizes = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

                int width = 640, height = 480;
                if (jpegSizes != null && jpegSizes.length > 0) {
                    // Get medium size for faster capture
                    android.util.Size size = chooseBestSize(jpegSizes);
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

    private android.util.Size chooseBestSize(android.util.Size[] sizes) {
        // Sort by area (ascending)
        List<android.util.Size> sizeList = Arrays.asList(sizes);
        Collections.sort(sizeList, (a, b) -> {
            int areaA = a.getWidth() * a.getHeight();
            int areaB = b.getWidth() * b.getHeight();
            return Integer.compare(areaA, areaB);
        });

        // Choose a medium-sized image (around 1-2 MP for faster capture)
        for (android.util.Size size : sizeList) {
            int area = size.getWidth() * size.getHeight();
            if (area >= 640 * 480 && area <= 1920 * 1080) {
                return size;
            }
        }

        // Fallback to smallest available
        return sizeList.get(0);
    }

    public String getCameraIdByFacing(int facingType) {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == facingType) {
                    return cameraId;
                }
            }
            // Return first camera if no match
            if (cameraIds.length > 0) {
                return cameraIds[0];
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting camera by facing", e);
        }
        return null;
    }

    public byte[] capturePhoto(String cameraId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                lastError = "Camera permission not granted";
                return null;
            }
        }

        capturedImageData = null;
        lastError = null;
        captureLatch = new CountDownLatch(1);

        startBackgroundThread();

        try {
            openCameraAndCapture(cameraId);

            // Wait for capture to complete (max 10 seconds)
            boolean completed = captureLatch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                lastError = "Capture timeout";
            }
        } catch (Exception e) {
            lastError = "Capture error: " + e.getMessage();
            Log.e(TAG, "Error capturing photo", e);
        } finally {
            closeCamera();
            stopBackgroundThread();
        }

        return capturedImageData;
    }

    public String capturePhotoBase64(String cameraId) {
        byte[] data = capturePhoto(cameraId);
        if (data != null) {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        }
        return null;
    }

    public String getLastError() {
        return lastError;
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
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

    private void openCameraAndCapture(String cameraId) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            android.util.Size[] jpegSizes = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);

            android.util.Size size = chooseBestSize(jpegSizes);
            int width = size.getWidth();
            int height = size.getHeight();

            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        capturedImageData = new byte[buffer.remaining()];
                        buffer.get(capturedImageData);
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    captureLatch.countDown();
                }
            }, backgroundHandler);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    lastError = "Camera permission not granted";
                    captureLatch.countDown();
                    return;
                }
            }

            final CountDownLatch openLatch = new CountDownLatch(1);

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    openLatch.countDown();
                    createCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                    lastError = "Camera disconnected";
                    captureLatch.countDown();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    lastError = "Camera error: " + error;
                    captureLatch.countDown();
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            lastError = "Camera access error: " + e.getMessage();
            captureLatch.countDown();
        } catch (SecurityException e) {
            lastError = "Camera permission denied";
            captureLatch.countDown();
        }
    }

    private void createCaptureSession() {
        try {
            Surface surface = imageReader.getSurface();

            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            capturePhoto();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            lastError = "Failed to configure capture session";
                            captureLatch.countDown();
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            lastError = "Session creation error: " + e.getMessage();
            captureLatch.countDown();
        }
    }

    private void capturePhoto() {
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            // Auto-focus and auto-exposure
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            captureSession.capture(captureBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            // Image will be available in ImageReader callback
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            lastError = "Capture error: " + e.getMessage();
            captureLatch.countDown();
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
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
}
