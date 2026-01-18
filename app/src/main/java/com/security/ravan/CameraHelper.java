package com.security.ravan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Simple camera capture helper - captures photos synchronously
 */
public class CameraHelper {
    private static final String TAG = "CameraHelper";

    private Context context;
    private CameraManager cameraManager;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private CameraCaptureSession captureSession;

    private byte[] capturedImageData;
    private String lastError;
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CountDownLatch captureLatch;

    public CameraHelper(Context context) {
        this.context = context.getApplicationContext();
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
                try {
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

                    Size[] sizes = null;
                    try {
                        sizes = characteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                .getOutputSizes(ImageFormat.JPEG);
                    } catch (Exception e) {
                    }

                    int width = 640, height = 480;
                    if (sizes != null && sizes.length > 0) {
                        // Pick a medium size
                        for (Size size : sizes) {
                            if (size.getWidth() >= 640 && size.getWidth() <= 1280) {
                                width = size.getWidth();
                                height = size.getHeight();
                                break;
                            }
                        }
                    }

                    cameras.add(new CameraInfo(cameraId, facingStr, width, height));
                } catch (Exception e) {
                    Log.e(TAG, "Error getting camera info for " + cameraId, e);
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting camera list", e);
        }
        return cameras;
    }

    public byte[] capturePhoto(String cameraId) {
        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                lastError = "Camera permission not granted";
                return null;
            }
        }

        capturedImageData = null;
        lastError = null;

        try {
            startBackgroundThread();

            if (!cameraOpenCloseLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                lastError = "Camera lock timeout";
                return null;
            }

            captureLatch = new CountDownLatch(1);

            openCameraAndCapture(cameraId);

            // Wait for capture to complete
            if (!captureLatch.await(15, TimeUnit.SECONDS)) {
                lastError = "Capture timeout";
            }

        } catch (InterruptedException e) {
            lastError = "Interrupted: " + e.getMessage();
        } catch (Exception e) {
            lastError = "Error: " + e.getMessage();
            Log.e(TAG, "Capture error", e);
        } finally {
            closeCamera();
            cameraOpenCloseLock.release();
            stopBackgroundThread();
        }

        return capturedImageData;
    }

    public String getLastError() {
        return lastError;
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraThread");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join(1000);
            } catch (InterruptedException e) {
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void openCameraAndCapture(String cameraId) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            Size[] sizes = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);

            // Pick a reasonable size
            int width = 640, height = 480;
            if (sizes != null && sizes.length > 0) {
                for (Size size : sizes) {
                    if (size.getWidth() >= 640 && size.getWidth() <= 1280) {
                        width = size.getWidth();
                        height = size.getHeight();
                        break;
                    }
                }
                if (width == 640 && sizes.length > 0) {
                    // Use smallest if no match
                    Size smallest = sizes[sizes.length - 1];
                    for (Size size : sizes) {
                        if (size.getWidth() * size.getHeight() < smallest.getWidth() * smallest.getHeight()) {
                            smallest = size;
                        }
                    }
                    width = smallest.getWidth();
                    height = smallest.getHeight();
                }
            }

            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        capturedImageData = new byte[buffer.remaining()];
                        buffer.get(capturedImageData);
                        Log.d(TAG, "Image captured: " + capturedImageData.length + " bytes");
                    }
                } catch (Exception e) {
                    lastError = "Image read error: " + e.getMessage();
                    Log.e(TAG, "Error reading image", e);
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

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "Camera opened");
                    cameraDevice = camera;
                    createCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(TAG, "Camera disconnected");
                    camera.close();
                    cameraDevice = null;
                    lastError = "Camera disconnected";
                    captureLatch.countDown();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "Camera error: " + error);
                    camera.close();
                    cameraDevice = null;
                    lastError = "Camera error code: " + error;
                    captureLatch.countDown();
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            lastError = "Camera access error: " + e.getMessage();
            Log.e(TAG, "Camera access error", e);
            captureLatch.countDown();
        } catch (SecurityException e) {
            lastError = "Permission denied";
            captureLatch.countDown();
        } catch (Exception e) {
            lastError = "Error: " + e.getMessage();
            Log.e(TAG, "Error opening camera", e);
            captureLatch.countDown();
        }
    }

    private void createCaptureSession() {
        try {
            if (cameraDevice == null || imageReader == null) {
                lastError = "Camera not ready";
                captureLatch.countDown();
                return;
            }

            Surface surface = imageReader.getSurface();

            cameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            Log.d(TAG, "Session configured");
                            captureSession = session;
                            takePhoto();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Session configuration failed");
                            lastError = "Session configuration failed";
                            captureLatch.countDown();
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            lastError = "Session error: " + e.getMessage();
            Log.e(TAG, "Session error", e);
            captureLatch.countDown();
        } catch (Exception e) {
            lastError = "Error: " + e.getMessage();
            captureLatch.countDown();
        }
    }

    private void takePhoto() {
        try {
            if (cameraDevice == null || captureSession == null) {
                lastError = "Not ready for capture";
                captureLatch.countDown();
                return;
            }

            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());

            // Basic settings
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            captureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "Capture completed");
                    // Image will be available in ImageReader callback
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            lastError = "Capture error: " + e.getMessage();
            Log.e(TAG, "Capture error", e);
            captureLatch.countDown();
        } catch (Exception e) {
            lastError = "Error: " + e.getMessage();
            captureLatch.countDown();
        }
    }

    private void closeCamera() {
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
        } catch (Exception e) {
        }

        try {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception e) {
        }

        try {
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception e) {
        }
    }
}
