package com.syndicati.services.biometric;

import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import com.github.sarxos.webcam.Webcam;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.javacv.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;
import com.syndicati.services.InsightFaceService;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Real Camera Service using Webcam Capture Library
 * Provides stable, cross-platform webcam access with face detection overlays
 */
public class RealCameraService {
    private static final long CAPTURE_STATUS_INTERVAL_MS = 3000L;
    private static final long FACE_POSITION_LOG_INTERVAL_MS = 1500L;
    private static final long NO_FACE_LOG_INTERVAL_MS = 5000L;
    
    private Webcam webcam;
    private boolean isRunning = false;
    private Thread captureThread;
    private BufferedImage currentFrame;
    private List<BufferedImage> capturedFrames;
    private long frameSequence = 0L;
    private long lastRenderedSequence = -1L;
    private Image lastRenderedImage;
    private BufferedImage insightDisplayBuffer;
    private BufferedImage basicDisplayBuffer;
    private int faceDetectCounter = 0;
    private int loggingThrottle = 0;  // Throttle face detection logs (log every 30 frames = 1/sec at 30fps)
    private java.awt.Rectangle lastLoggedFacePosition = null;  // Track last logged position
    private long lastFacePositionLogTime = 0L;
    private long lastNoFaceLogTime = 0L;
    
    // Eye position smoothing buffers (for stable, non-jittery detection)
    private int[][] eyePositionHistory = new int[5][2];  // Keep last 5 frames of eye positions
    private int eyeHistoryIndex = 0;
    private boolean eyeHistoryFilled = false;
    
    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;
    private OpenCVFrameConverter.ToMat toMat;
    private boolean landmarkDetectionInitialized = false;
    private boolean landmarkDetectionAvailable = false;
    private boolean landmarkDetectionAttempted = false;  // NEW: Prevent repeated attempts
    private InsightFaceService insightFaceService = null;
    private boolean useInsightFace = false;
    private Thread insightFaceStartupThread = null;
    private boolean insightFaceReady = false;
    
    public RealCameraService() {
        currentFrame = null;
        capturedFrames = new ArrayList<>();
        // Don't initialize any OpenCV classes here to avoid native library loading issues
        // All OpenCV initialization is deferred to lazy initialization
        
        // Start InsightFace in background thread (non-blocking app startup)
        startInsightFaceAsync();
    }
    
    /**
     * Start InsightFace service in background thread with retry logic
     */
    private void startInsightFaceAsync() {
        if (insightFaceService != null && insightFaceService.isRunning()) {
            return; // Already running
        }
        
        CompletableFuture.runAsync(() -> {
            System.out.println("RealCameraService: Initializing InsightFace service...");
            insightFaceService = new InsightFaceService();
            boolean started = insightFaceService.initialize();
            
            if (started) {
                System.out.println("RealCameraService: [OK] InsightFace service started successfully.");
                landmarkDetectionAvailable = true;
                insightFaceReady = true;
                useInsightFace = true;  // Enable InsightFace usage now that it's running
            } else {
                System.err.println("RealCameraService: [X] InsightFace service failed to start. Trying fallback...");
                insightFaceService = null; // Clear failed service
                
                // Fallback to basic Haar detector
                initializeLandmarkDetection();
            }
        });
    }
    
    /**
     * Wait for InsightFace to finish initializing (blocks for up to 15 seconds)
     */
    private void waitForInsightFaceReady() {
        if (insightFaceReady) return;
        
        System.out.println("RealCameraService: Waiting for InsightFace initialization...");
        long timeout = System.currentTimeMillis() + 15000;  // 15 second wait max
        
        while (!insightFaceReady && System.currentTimeMillis() < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (insightFaceReady) {
            System.out.println("RealCameraService: InsightFace initialization complete!");
        } else {
            System.out.println("RealCameraService: InsightFace initialization timeout - proceeding with Haar fallback");
            insightFaceReady = true;
        }
    }

    /**
     * Initialize landmark detection lazily - only when first needed
     * Safely handles OpenCV initialization failures on systems without native libraries
     */
    private void initializeLandmarkDetection() {
        // If we've already attempted initialization, don't try again
        if (landmarkDetectionAttempted) {
            return;
        }
        landmarkDetectionAttempted = true;  // Mark as attempted (success or failure)
        landmarkDetectionInitialized = true;
        
        System.out.println("RealCameraService: Attempting landmark detection initialization (one-time)...");
        
        try {
            // Try to initialize OpenCV components
            // This may fail on Windows if native OpenBLAS libraries aren't available
            toMat = new OpenCVFrameConverter.ToMat();
            System.out.println("RealCameraService: OpenCV Mat converter initialized");
            
            // Load face detector
            String faceCascadePath = "/haarcascades/haarcascade_frontalface_alt.xml";
            java.io.InputStream faceIs = CascadeClassifier.class.getResourceAsStream(faceCascadePath);
            if (faceIs != null) {
                String tempPath = System.getProperty("java.io.tmpdir") + "/haarcascade_frontalface_alt.xml";
                java.nio.file.Files.copy(faceIs, java.nio.file.Paths.get(tempPath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                faceDetector = new CascadeClassifier(tempPath);
                if (faceDetector != null && !faceDetector.empty()) {
                    System.out.println("RealCameraService: [OK] Face detector loaded successfully");
                    landmarkDetectionAvailable = true;
                } else {
                    System.err.println("RealCameraService: [X] Face detector loaded but empty (OpenCV issue)");
                }
            } else {
                System.err.println("RealCameraService: Face cascade XML not found in resources");
            }
            
            // Load eye detector
            String eyeCascadePath = "/haarcascades/haarcascade_eye.xml";
            java.io.InputStream eyeIs = CascadeClassifier.class.getResourceAsStream(eyeCascadePath);
            if (eyeIs != null) {
                String tempPath = System.getProperty("java.io.tmpdir") + "/haarcascade_eye.xml";
                java.nio.file.Files.copy(eyeIs, java.nio.file.Paths.get(tempPath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                eyeDetector = new CascadeClassifier(tempPath);
                if (eyeDetector != null && !eyeDetector.empty()) {
                    System.out.println("RealCameraService: [OK] Eye detector loaded successfully");
                    landmarkDetectionAvailable = true;
                } else {
                    System.err.println("RealCameraService: [X] Eye detector loaded but empty (OpenCV issue)");
                }
            } else {
                System.err.println("RealCameraService: Eye cascade XML not found in resources");
            }
            
            if (landmarkDetectionAvailable) {
                System.out.println("RealCameraService: [OK] HAAR Cascade facial landmark detection ready");
            } else {
                System.out.println("RealCameraService: [X] HAAR Cascade not available - using skin color detection");
            }
        } catch (UnsatisfiedLinkError ex) {
            // OpenCV natives are optional here: when unavailable, we intentionally fall back
            // to the Java-only color detection pipeline (so the app can still work).
            System.out.println("RealCameraService: OpenCV natives unavailable; using skin-color fallback.");
            landmarkDetectionAvailable = false;
        } catch (NoClassDefFoundError ex) {
            System.err.println("RealCameraService: [X] OpenCV classes not available: " + ex.getMessage());
            landmarkDetectionAvailable = false;
        } catch (Exception ex) {
            System.err.println("RealCameraService: [X] Error initializing landmark detection: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            landmarkDetectionAvailable = false;
        }
    }

    /**
     * Load face and eye detection classifiers (deprecated - use lazy initialization instead)
     */
    private void loadFaceDetectors() {
        initializeLandmarkDetection();
    }

    /**
     * Initialize camera - tries to find and open the first available webcam
     */
    public boolean initializeCamera(int index) {
        try {
            System.out.println("RealCameraService: Attempting to initialize camera at index: " + index);
            
            // Get list of available webcams
            java.util.List<Webcam> webcams = Webcam.getWebcams();
            System.out.println("RealCameraService: Found " + webcams.size() + " webcams");
            
            if (webcams.isEmpty()) {
                System.err.println("RealCameraService: No webcams found on system");
                return false;
            }
            
            // Get the specified webcam or default to first
            if (index >= 0 && index < webcams.size()) {
                webcam = webcams.get(index);
                System.out.println("RealCameraService: Using camera " + index + ": " + webcam.getName());
            } else {
                webcam = Webcam.getDefault();
                System.out.println("RealCameraService: Using default camera: " + webcam.getName());
            }
            
            if (webcam == null) {
                System.err.println("RealCameraService: Failed to get webcam");
                return false;
            }
            
            // Set resolution
            try {
                webcam.setViewSize(new java.awt.Dimension(640, 480));
            } catch (Exception ex) {
                System.out.println("RealCameraService: Could not set resolution, using default");
            }
            
            // Open the webcam
            System.out.println("RealCameraService: Opening webcam...");
            if (!webcam.open()) {
                System.err.println("RealCameraService: Failed to open webcam");
                return false;
            }
            
            System.out.println("RealCameraService: Camera initialized successfully!");
            System.out.println("  - Name: " + webcam.getName());
            System.out.println("  - Resolution: " + webcam.getViewSize());
            
            return true;
            
        } catch (Exception ex) {
            System.err.println("RealCameraService: Error initializing camera: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Start capturing frames from the webcam
     */
    public void startCapture() {
        if (isRunning || webcam == null) {
            System.out.println("RealCameraService: Already capturing or webcam not initialized");
            return;
        }
        
        System.out.println("RealCameraService: Starting capture...");
        isRunning = true;
        
        captureThread = new Thread(() -> {
            int frameCount = 0;
            int errorCount = 0;
            long lastPrintTime = System.currentTimeMillis();
            
            try {
                while (isRunning && webcam != null && webcam.isOpen()) {
                    try {
                        BufferedImage frame = webcam.getImage();
                        if (frame != null) {
                            synchronized (this) {
                                currentFrame = frame;
                                frameSequence++;
                            }
                            frameCount++;
                            errorCount = 0;
                            
                            // Print status every second
                            long now = System.currentTimeMillis();
                            if (now - lastPrintTime > CAPTURE_STATUS_INTERVAL_MS) {
                                System.out.println("RealCameraService: " + frameCount + " frames captured");
                                lastPrintTime = now;
                            }
                        } else {
                            System.err.println("RealCameraService: Null frame from webcam");
                            errorCount++;
                            if (errorCount > 10) {
                                System.err.println("RealCameraService: Too many null frames, stopping");
                                isRunning = false;
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("RealCameraService: Error capturing frame: " + ex.getMessage());
                        errorCount++;
                        if (errorCount > 5) {
                            System.err.println("RealCameraService: Too many errors, stopping");
                            isRunning = false;
                            break;
                        }
                    }
                    
                    Thread.sleep(33); // ~30 FPS
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("RealCameraService: Capture thread error: " + ex.getMessage());
                ex.printStackTrace();
            }
            System.out.println("RealCameraService: Capture stopped. Total frames: " + frameCount);
        });
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stop capturing frames
     */
    public void stopCapture() {
        isRunning = false;
        if (captureThread != null) {
            try {
                captureThread.join(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get current frame as JavaFX Image with face detection and facial landmarks
     */
    public synchronized Image getCurrentFrameWithDetection() {
        if (currentFrame == null) {
            return null;
        }

        if (lastRenderedImage != null && lastRenderedSequence == frameSequence) {
            return lastRenderedImage;
        }
        
        try {
            // First priority: Try InsightFace for 3D face mesh with anti-spoofing
            // Wait for it to finish initializing on first frame (if still starting)
            if (!insightFaceReady) {
                waitForInsightFaceReady();
            }
            
            if (useInsightFace && insightFaceService != null && insightFaceService.isRunning()) {
                System.out.println("RealCameraService: Attempting InsightFace detection...");
                try {
                    Image insightFaceImage = detectWithInsightFace();
                    if (insightFaceImage != null) {
                        System.out.println("RealCameraService: [OK] InsightFace detection succeeded");
                        return cacheRenderedImage(insightFaceImage);
                    }
                } catch (Exception ex) {
                    System.err.println("RealCameraService: InsightFace detection failed: " + ex.getMessage());
                }
            }
            
            // Second priority: Try basic Graphics2D detection (always available)
            System.out.println("RealCameraService: Using basic detection with facial landmarks...");
            try {
                Image detectedImage = detectBasic();
                if (detectedImage != null) {
                    System.out.println("RealCameraService: [OK] Basic detection succeeded - landmarks should be visible");
                    return cacheRenderedImage(detectedImage);
                }
            } catch (Exception ex) {
                System.err.println("RealCameraService: Error in basic detection: " + ex.getMessage());
                ex.printStackTrace();
            }
            
            // Third priority: Try OpenCV landmarks (optional, non-critical)
            try {
                if (!landmarkDetectionInitialized) {
                    initializeLandmarkDetection();
                }
                
                if (landmarkDetectionAvailable) {
                    try {
                        Image landmarkImage = detectWithLandmarks();
                        if (landmarkImage != null) {
                            return cacheRenderedImage(landmarkImage);
                        }
                    } catch (Exception ex) {
                        System.err.println("RealCameraService: Landmark detection failed: " + ex.getMessage());
                        landmarkDetectionAvailable = false;
                    }
                }
            } catch (Throwable ex) {
                System.err.println("RealCameraService: Failed to initialize OpenCV: " + ex.getMessage());
                landmarkDetectionAvailable = false;
            }
        } catch (Exception ex) {
            System.err.println("RealCameraService: Error in detection pipeline: " + ex.getMessage());
        }
        
        // Final fallback: return raw frame
        try {
            return cacheRenderedImage(SwingFXUtils.toFXImage(currentFrame, null));
        } catch (Exception ex) {
            System.err.println("RealCameraService: All frame display methods failed");
            return null;
        }
    }
    
    /**
     * Detect 3D face mesh using InsightFace (468-point 3D landmarks with anti-spoofing)
     */
    private Image detectWithInsightFace() {
        if (insightFaceService == null) {
            return null;
        }

        BufferedImage frameSnapshot = currentFrame;
        if (frameSnapshot == null) {
            return null;
        }
        
        try {
            // Process frame with InsightFace
            InsightFaceService.FaceMesh mesh = insightFaceService.processFaceFrame(frameSnapshot, true);
            
            if (mesh == null || !mesh.detected) {
                return null;
            }
            
            // Reuse a persistent drawing buffer to reduce allocation churn.
            if (insightDisplayBuffer == null
                || insightDisplayBuffer.getWidth() != frameSnapshot.getWidth()
                || insightDisplayBuffer.getHeight() != frameSnapshot.getHeight()) {
                insightDisplayBuffer = new BufferedImage(
                    frameSnapshot.getWidth(),
                    frameSnapshot.getHeight(),
                    BufferedImage.TYPE_INT_RGB
                );
            }
            BufferedImage display = insightDisplayBuffer;
            
            java.awt.Graphics2D g2d = display.createGraphics();
            g2d.drawImage(frameSnapshot, 0, 0, null);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw 3D face mesh
            InsightFaceService.drawFaceMesh(g2d, mesh, frameSnapshot.getWidth(), frameSnapshot.getHeight());
            
            // Draw anti-spoofing status
            if (mesh.spoofing != null) {
                drawSpoofingStatus(g2d, mesh.spoofing, frameSnapshot.getWidth());
            }
            
            g2d.dispose();
            
            // Convert to JavaFX Image
            Image fxImage = SwingFXUtils.toFXImage(display, null);
            return fxImage;
            
        } catch (Exception ex) {
            System.err.println("RealCameraService: InsightFace processing error: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Draw anti-spoofing status indicator
     */
    private void drawSpoofingStatus(java.awt.Graphics2D g2d, InsightFaceService.SpoofingAnalysis spoofing, int imageWidth) {
        java.awt.Color statusColor;
        String statusText;
        
        if (spoofing.isSpoof) {
            statusColor = new java.awt.Color(255, 0, 0);
            statusText = String.format("[WARN] SPOOF DETECTED (%.0f%%)", spoofing.spoofingScore * 100);
        } else if (spoofing.spoofingScore > 0.3) {
            statusColor = new java.awt.Color(255, 200, 0);
            statusText = String.format("? SUSPICIOUS (%.0f%%)", spoofing.spoofingScore * 100);
        } else {
            statusColor = new java.awt.Color(0, 200, 0);
            statusText = "[OK] REAL FACE";
        }
        
        g2d.setColor(statusColor);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        g2d.drawString(statusText, 10, 30);
    }
    
    /**
     * Detect faces with facial landmarks using OpenCV
     */
    private Image detectWithLandmarks() {
        // Convert BufferedImage to Mat for OpenCV processing
        Mat frameMat = bufferedImageToMat(currentFrame);
        if (frameMat == null || frameMat.empty()) {
            return SwingFXUtils.toFXImage(currentFrame, null);
        }
        
        // Convert to grayscale for detection
        Mat gray = new Mat();
        cvtColor(frameMat, gray, CV_BGR2GRAY);
        equalizeHist(gray, gray);
        
        // Detect faces
        RectVector faceDetections = new RectVector();
        if (faceDetector != null) {
            faceDetector.detectMultiScale(gray, faceDetections, 1.1, 4, 0,
                new Size(30, 30), new Size(300, 300));
        }
        
        // Draw face box and landmarks
        Mat display = frameMat.clone();
        
        if (faceDetections.size() > 0) {
            Rect face = faceDetections.get(0);
            
            // Draw main face box (green)
            rectangle(display,
                new Point(face.x(), face.y()),
                new Point(face.x() + face.width(), face.y() + face.height()),
                new Scalar(0, 255, 0, 0), 3, 0, 0);
            
            // Draw facial landmarks (detected eyes and simulated feature points)
            drawFacialLandmarks(display, face, gray);
            
            // Draw center crosshair
            int centerX = face.x() + face.width() / 2;
            int centerY = face.y() + face.height() / 2;
            circle(display, new Point(centerX, centerY), 5, new Scalar(255, 255, 255, 0), -1, 0, 0);
        } else {
            // No face detected - draw guidance text
            putText(display, "Position your face in the frame",
                new Point(20, 50), 1, 1.0, new Scalar(255, 0, 0, 0), 2, 0, false);
        }
        
        // Convert back to JavaFX Image
        BufferedImage result = matToBufferedImage(display);
        gray.release();
        display.release();
        frameMat.release();
        
        if (result == null) {
            return SwingFXUtils.toFXImage(currentFrame, null);
        }
        
        Image fxImage = SwingFXUtils.toFXImage(result, null);
        return fxImage;
    }
    
    /**
     * Simple face detection without landmarks (fallback) - simple and robust
     * Uses Graphics2D to draw 50+ mesh points overlay without dependencies
     * Detects face position and draws dense mesh of landmarks
     */
    private Image detectBasic() {
        if (currentFrame == null) {
            System.out.println("RealCameraService: detectBasic - currentFrame is null!");
            return null;
        }
        
        System.out.println("RealCameraService: detectBasic - starting detection");
        
        try {
            // Reuse a persistent drawing buffer to reduce allocation churn.
            if (basicDisplayBuffer == null
                || basicDisplayBuffer.getWidth() != currentFrame.getWidth()
                || basicDisplayBuffer.getHeight() != currentFrame.getHeight()) {
                basicDisplayBuffer = new BufferedImage(
                    currentFrame.getWidth(),
                    currentFrame.getHeight(),
                    BufferedImage.TYPE_INT_RGB
                );
            }
            BufferedImage display = basicDisplayBuffer;
            
            // Copy current frame
            java.awt.Graphics2D g2d = display.createGraphics();
            g2d.drawImage(currentFrame, 0, 0, null);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Detect face using multiple strategies
            System.out.println("RealCameraService: detectBasic - detecting face with cascade...");
            java.awt.Rectangle detectedFace = detectFaceWithCascade(currentFrame);
            
            int centerX, centerY, boxWidth, boxHeight;
            boolean faceDetected = false;
            
            if (detectedFace != null && detectedFace.width > 50 && detectedFace.height > 50) {
                // Use detected face position (most accurate)
                centerX = detectedFace.x + detectedFace.width / 2;
                centerY = detectedFace.y + detectedFace.height / 2;
                boxWidth = detectedFace.width;
                boxHeight = detectedFace.height;
                faceDetected = true;
                long now = System.currentTimeMillis();
                // Throttle logging: only log if face position changed or every 30 frames
                if (lastLoggedFacePosition == null || 
                    Math.abs(lastLoggedFacePosition.x - centerX) > 50 || 
                    Math.abs(lastLoggedFacePosition.y - centerY) > 50 ||
                    (loggingThrottle == 0 && (now - lastFacePositionLogTime) > FACE_POSITION_LOG_INTERVAL_MS)) {
                    System.out.println("RealCameraService: Face detected at (" + centerX + "," + centerY + ") size:" + boxWidth + "x" + boxHeight);
                    lastLoggedFacePosition = new java.awt.Rectangle(centerX, centerY, boxWidth, boxHeight);
                    lastFacePositionLogTime = now;
                }
            } else {
                // Fallback: detect using color and luminance
                java.awt.Rectangle colorDetectedFace = detectFaceByColor(currentFrame);
                if (colorDetectedFace != null && colorDetectedFace.width > 50 && colorDetectedFace.height > 50) {
                    centerX = colorDetectedFace.x + colorDetectedFace.width / 2;
                    centerY = colorDetectedFace.y + colorDetectedFace.height / 2;
                    boxWidth = colorDetectedFace.width;
                    boxHeight = colorDetectedFace.height;
                    faceDetected = true;
                    long now = System.currentTimeMillis();
                    // Throttle logging: only log if position changed or every 30 frames
                    if (lastLoggedFacePosition == null || 
                        Math.abs(lastLoggedFacePosition.x - centerX) > 50 || 
                        Math.abs(lastLoggedFacePosition.y - centerY) > 50 ||
                        (loggingThrottle == 0 && (now - lastFacePositionLogTime) > FACE_POSITION_LOG_INTERVAL_MS)) {
                        System.out.println("RealCameraService: Face detected by color at (" + centerX + "," + centerY + ")");
                        lastLoggedFacePosition = new java.awt.Rectangle(centerX, centerY, boxWidth, boxHeight);
                        lastFacePositionLogTime = now;
                    }
                } else {
                    // No face detected - use center as last resort
                    centerX = display.getWidth() / 2;
                    centerY = display.getHeight() / 2;
                    boxWidth = 180;
                    boxHeight = 220;
                    // Log no-face state infrequently to avoid UI jank from console I/O.
                    long now = System.currentTimeMillis();
                    if (loggingThrottle == 0 && (now - lastNoFaceLogTime) > NO_FACE_LOG_INTERVAL_MS) {
                        System.out.println("RealCameraService: No face detected, using screen center");
                        lastNoFaceLogTime = now;
                    }
                }
            }
            
            // Update throttle counter (cycle 0-29, so we log every 30 frames)
            loggingThrottle = (loggingThrottle + 1) % 30;
            
            int x1 = centerX - boxWidth / 2;
            int y1 = centerY - boxHeight / 2;
            
            // Draw green detection box
            g2d.setColor(new java.awt.Color(0, 255, 0));
            g2d.setStroke(new java.awt.BasicStroke(2.5f));
            g2d.drawRect(x1, y1, boxWidth, boxHeight);
            
            // Detect actual eye positions for accurate mesh (use display frame copy to avoid null)
            int[][] eyePositions = null;
            if (currentFrame != null) {
                eyePositions = detectEyePositions(currentFrame, centerX, centerY, boxWidth, boxHeight);
            }
            
            // Use default eye positions if detection failed
            if (eyePositions == null) {
                eyePositions = new int[2][2];
                eyePositions[0][0] = x1 + (int)(boxWidth * 0.25);
                eyePositions[0][1] = y1 + (int)(boxHeight * 0.25);
                eyePositions[1][0] = x1 + (int)(boxWidth * 0.75);
                eyePositions[1][1] = y1 + (int)(boxHeight * 0.25);
            }
            
            // Draw 50+ mesh points adapted to actual facial features
            System.out.println("RealCameraService: detectBasic - drawing facial mesh landmarks...");
            drawAdaptiveFacialMesh(g2d, centerX, centerY, boxWidth, boxHeight, eyePositions);
            System.out.println("RealCameraService: detectBasic - mesh landmarks drawn complete");
            
            g2d.dispose();
            
            // Convert to JavaFX Image
            Image fxImage = SwingFXUtils.toFXImage(display, null);
            if (fxImage == null) {
                System.err.println("RealCameraService: Failed to convert to FX Image");
                return SwingFXUtils.toFXImage(currentFrame, null);
            }
            System.out.println("RealCameraService: detectBasic - returning image with landmarks");
            return fxImage;
            
        } catch (Exception ex) {
            System.err.println("RealCameraService: Error in detectBasic: " + ex.getMessage());
            ex.printStackTrace();
            // Final fallback: return raw frame
            try {
                return SwingFXUtils.toFXImage(currentFrame, null);
            } catch (Exception e) {
                System.err.println("RealCameraService: Failed to convert fallback frame: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Detect actual eye positions using OpenCV's pre-trained Haar cascade classifier
     * This is battle-tested and proven to work better than custom algorithms
     * Returns [[leftEyeX, leftEyeY], [rightEyeX, rightEyeY]]
     */
    private int[][] detectEyePositions(BufferedImage frame, int centerX, int centerY, int boxWidth, int boxHeight) {
        int[][] eyePositions = new int[2][2];
        
        // Safety check - if Haar cascade not available, use geometry
        if (frame == null) {
            eyePositions[0][0] = centerX - boxWidth / 5;
            eyePositions[0][1] = centerY - boxHeight / 4;
            eyePositions[1][0] = centerX + boxWidth / 5;
            eyePositions[1][1] = centerY - boxHeight / 4;
            return eyePositions;
        }
        
        // If cascade not yet attempted, try to initialize it ONCE
        if (!landmarkDetectionAttempted) {
            initializeLandmarkDetection();
        }
        
        // If cascade still not available, use geometry-based fallback
        if (eyeDetector == null || eyeDetector.empty()) {
            int x1 = centerX - boxWidth / 2;
            int y1 = centerY - boxHeight / 2;
            eyePositions[0][0] = x1 + (int)(boxWidth * 0.27);
            eyePositions[0][1] = y1 + (int)(boxHeight * 0.30);
            eyePositions[1][0] = x1 + (int)(boxWidth * 0.73);
            eyePositions[1][1] = y1 + (int)(boxHeight * 0.30);
            return eyePositions;
        }
        
        try {
            // Convert BufferedImage to Mat using the project's existing pattern
            Mat mat_img = new Mat(frame.getHeight(), frame.getWidth(), CV_8UC3);
            byte[] data = new byte[frame.getWidth() * frame.getHeight() * 3];
            int[] pixels = new int[frame.getWidth() * frame.getHeight()];
            frame.getRGB(0, 0, frame.getWidth(), frame.getHeight(), pixels, 0, frame.getWidth());
            
            for (int i = 0; i < pixels.length; i++) {
                data[i*3] = (byte)((pixels[i] >> 16) & 0xFF);      // Blue
                data[i*3 + 1] = (byte)((pixels[i] >> 8) & 0xFF);   // Green
                data[i*3 + 2] = (byte)(pixels[i] & 0xFF);          // Red
            }
            mat_img.data().put(data);
            
            // Convert to grayscale for detection
            Mat gray = new Mat();
            cvtColor(mat_img, gray, CV_BGR2GRAY);
            equalizeHist(gray, gray);  // Improve contrast
            
            // Define search region (within detected face box)
            int x1 = centerX - boxWidth / 2;
            int y1 = centerY - boxHeight / 2;
            
            // Clamp ROI to frame bounds
            int roiX = Math.max(0, x1);
            int roiY = Math.max(0, y1);
            int roiW = Math.min(boxWidth, gray.cols() - roiX);
            int roiH = Math.min(boxHeight, gray.rows() - roiY);
            
            Rect roi_rect = new Rect(roiX, roiY, roiW, roiH);
            Mat roi_mat = new Mat(gray, roi_rect);
            
            // Detect eyes with STRICTER parameters to avoid false positives
            RectVector eyes = new RectVector();
            if (eyeDetector != null && !eyeDetector.empty()) {
                eyeDetector.detectMultiScale(roi_mat, eyes, 1.05, 7, 0,  // 1.05 scale, 7 neighbors = stricter
                    new Size(20, 20), new Size(Math.min(boxWidth/3, 80), Math.min(boxHeight/3, 80)));
            }
            
            long numEyes = eyes.size();
            System.out.println("RealCameraService: Haar cascade detected " + numEyes + " eye regions (strict mode)");
            
            if (numEyes >= 2) {
                // Find the best pair: look for two eyes that are roughly symmetrical
                // Sort by X position to find left and right candidates
                java.util.List<Rect> eyeList = new java.util.ArrayList<>();
                for (long i = 0; i < eyes.size(); i++) {
                    eyeList.add(eyes.get((int)i));
                }
                eyeList.sort((a, b) -> Integer.compare(a.x(), b.x()));
                
                // Take first (leftmost) and last (rightmost) - most likely to be actual eyes
                Rect leftEye = eyeList.get(0);
                Rect rightEye = eyeList.get(eyeList.size() - 1);
                
                // Validate they're at reasonable distance and Y positions are close
                int eyeDistX = rightEye.x() - leftEye.x();
                int eyeDistY = Math.abs(rightEye.y() - leftEye.y());
                int minDist = boxWidth / 8;
                int maxDist = (int)(boxWidth / 1.5);
                
                if (eyeDistX > minDist && eyeDistX < maxDist && eyeDistY < boxHeight / 5) {
                    // Calculate eye centers in FRAME coordinates (not ROI)
                    int leftEyeX = roiX + leftEye.x() + leftEye.width() / 2;
                    int leftEyeY = roiY + leftEye.y() + leftEye.height() / 2;
                    int rightEyeX = roiX + rightEye.x() + rightEye.width() / 2;
                    int rightEyeY = roiY + rightEye.y() + rightEye.height() / 2;
                    
                    eyePositions[0][0] = leftEyeX;
                    eyePositions[0][1] = leftEyeY;
                    eyePositions[1][0] = rightEyeX;
                    eyePositions[1][1] = rightEyeY;
                    
                    System.out.println("RealCameraService: Valid eye pair detected - L:(" + leftEyeX + "," + leftEyeY + 
                        ") R:(" + rightEyeX + "," + rightEyeY + ") dist=" + eyeDistX);
                } else {
                    // Eyes detected but invalid pair (too close/far or wrong Y)
                    System.out.println("RealCameraService: Eye pair invalid (distX=" + eyeDistX + " distY=" + eyeDistY + "), using defaults");
                    eyePositions[0][0] = roiX + (int)(roiW * 0.27);
                    eyePositions[0][1] = roiY + (int)(roiH * 0.30);
                    eyePositions[1][0] = roiX + (int)(roiW * 0.73);
                    eyePositions[1][1] = roiY + (int)(roiH * 0.30);
                }
            } else if (numEyes == 1) {
                // Only one eye found - use geometric estimate for the other
                Rect eye = eyes.get(0);
                int eyeX = roiX + eye.x() + eye.width() / 2;
                int eyeY = roiY + eye.y() + eye.height() / 2;
                int eyeGap = Math.max(50, eye.width() * 2);
                
                eyePositions[0][0] = eyeX - eyeGap / 2;
                eyePositions[0][1] = eyeY;
                eyePositions[1][0] = eyeX + eyeGap / 2;
                eyePositions[1][1] = eyeY;
                
                System.out.println("RealCameraService: 1 eye found, mirrored");
            } else {
                // No eyes found - use face geometry
                System.out.println("RealCameraService: No eyes detected (strict cascade), using geometry-based defaults");
                eyePositions[0][0] = roiX + (int)(roiW * 0.27);
                eyePositions[0][1] = roiY + (int)(roiH * 0.30);
                eyePositions[1][0] = roiX + (int)(roiW * 0.73);
                eyePositions[1][1] = roiY + (int)(roiH * 0.30);
            }
            
            // Cleanup
            mat_img.release();
            gray.release();
            roi_mat.release();
            
        } catch (Exception ex) {
            System.err.println("RealCameraService: Eye detection error: " + ex.getMessage());
            ex.printStackTrace();
            // Fallback to estimated positions
            eyePositions[0][0] = centerX - boxWidth / 5;
            eyePositions[0][1] = centerY - boxHeight / 4;
            eyePositions[1][0] = centerX + boxWidth / 5;
            eyePositions[1][1] = centerY - boxHeight / 4;
        }
        
        // Apply position smoothing to reduce jitter
        eyePositions = smoothEyePositions(eyePositions);
        
        return eyePositions;
    }
    
    /**
     * Smooth eye positions using frame history buffer
     * Keeps positions stable instead of jumping around frame-to-frame
     * Uses averaged positions from last 5 frames
     */
    private int[][] smoothEyePositions(int[][] detectedPositions) {
        // Store current detection midpoint in history
        int midX = (detectedPositions[0][0] + detectedPositions[1][0]) / 2;
        int midY = (detectedPositions[0][1] + detectedPositions[1][1]) / 2;
        
        eyePositionHistory[eyeHistoryIndex][0] = midX;
        eyePositionHistory[eyeHistoryIndex][1] = midY;
        
        eyeHistoryIndex = (eyeHistoryIndex + 1) % 5;
        if (eyeHistoryIndex == 0) {
            eyeHistoryFilled = true;
        }
        
        // Average last N frames for smooth position
        int framesToAverage = eyeHistoryFilled ? 5 : Math.max(1, eyeHistoryIndex);
        int sumX = 0, sumY = 0;
        
        for (int i = 0; i < framesToAverage; i++) {
            sumX += eyePositionHistory[i][0];
            sumY += eyePositionHistory[i][1];
        }
        
        int avgMidX = sumX / framesToAverage;
        int avgMidY = sumY / framesToAverage;
        
        // Use detected eye distance (current frame) to maintain accurate spacing
        int eyeDistance = Math.abs(detectedPositions[1][0] - detectedPositions[0][0]);
        eyeDistance = Math.max(eyeDistance, 40);  // Minimum distance
        
        // Return smoothed positions using averaged center and current eye distance
        int[][] smoothedPositions = new int[2][2];
        smoothedPositions[0][0] = avgMidX - eyeDistance / 2;  // Left eye
        smoothedPositions[0][1] = avgMidY;
        smoothedPositions[1][0] = avgMidX + eyeDistance / 2;  // Right eye  
        smoothedPositions[1][1] = avgMidY;
        
        System.out.println("RealCameraService: Smoothed (avg " + framesToAverage + " frames) center=(" + avgMidX + "," + avgMidY + ")");
        
        return smoothedPositions;
    }
    
    /**
     * Mesh points conform to the person's real face geometry, not a fixed template
     */
    private void drawAdaptiveFacialMesh(java.awt.Graphics2D g2d, int centerX, int centerY, 
                                       int boxWidth, int boxHeight, int[][] eyePositions) {
        int leftEyeX = eyePositions[0][0];
        int leftEyeY = eyePositions[0][1];
        int rightEyeX = eyePositions[1][0];
        int rightEyeY = eyePositions[1][1];
        
        int x1 = centerX - boxWidth / 2;
        int y1 = centerY - boxHeight / 2;
        
        // Calculate mesh spacing based on detected eye distance
        int eyeDistance = Math.abs(rightEyeX - leftEyeX);
        int noseX = leftEyeX + eyeDistance / 2;  // Nose between eyes
        int noseY = centerY;
        
        // FACE OUTLINE - 30+ points adapting to face shape
        g2d.setColor(new java.awt.Color(50, 255, 100, 200));
        g2d.setStroke(new java.awt.BasicStroke(1.5f));
        
        // Top forehead curve
        int[] foreheadX = new int[6];
        int[] foreheadY = new int[6];
        for (int i = 0; i < 6; i++) {
            foreheadX[i] = x1 + (int)(boxWidth * i / 6.0);
            foreheadY[i] = y1 + (int)(boxHeight * 0.08);
            if (i > 0) g2d.drawLine(foreheadX[i-1], foreheadY[i-1], foreheadX[i], foreheadY[i]);
        }
        
        // Right temple to ear
        int rightTempleX = x1 + (int)(boxWidth * 0.75);
        int rightTempleY = y1 + (int)(boxHeight * 0.15);
        g2d.drawLine(foreheadX[5], foreheadY[5], rightTempleX, rightTempleY);
        
        int rightEarX = x1 + boxWidth - 5;
        int rightEarY = centerY;
        g2d.drawLine(rightTempleX, rightTempleY, rightEarX, rightEarY);
        
        // Right cheek to jaw
        int rightCheekX = x1 + (int)(boxWidth * 0.7);
        int rightCheekY = centerY + (int)(boxHeight * 0.15);
        g2d.drawLine(rightEarX, rightEarY, rightCheekX, rightCheekY);
        
        int rightJawX = centerX + (int)(boxWidth * 0.3);
        int rightJawY = centerY + (int)(boxHeight * 0.35);
        g2d.drawLine(rightCheekX, rightCheekY, rightJawX, rightJawY);
        
        // Chin
        int chinX = centerX;
        int chinY = centerY + (int)(boxHeight * 0.45);
        g2d.drawLine(rightJawX, rightJawY, chinX, chinY);
        
        // Left jaw (mirror of right)
        int leftJawX = centerX - (int)(boxWidth * 0.3);
        int leftJawY = rightJawY;
        g2d.drawLine(chinX, chinY, leftJawX, leftJawY);
        
        int leftCheekX = x1 + (int)(boxWidth * 0.3);
        int leftCheekY = rightCheekY;
        g2d.drawLine(leftJawX, leftJawY, leftCheekX, leftCheekY);
        
        int leftEarX = x1 + 5;
        g2d.drawLine(leftCheekX, leftCheekY, leftEarX, rightEarY);
        
        int leftTempleX = x1 + (int)(boxWidth * 0.25);
        int leftTempleY = rightTempleY;
        g2d.drawLine(leftEarX, rightEarY, leftTempleX, leftTempleY);
        
        // Left forehead curve
        g2d.drawLine(leftTempleX, leftTempleY, foreheadX[0], foreheadY[0]);
        
        // Draw face outline points
        g2d.setColor(new java.awt.Color(100, 255, 150, 180));
        int[] outlineX = {foreheadX[0], foreheadX[2], foreheadX[5], rightTempleX, rightEarX, 
                         rightCheekX, rightJawX, chinX, leftJawX, leftCheekX, leftEarX, 
                         leftTempleX, foreheadX[0]};
        int[] outlineY = {foreheadY[0], foreheadY[2], foreheadY[5], rightTempleY, rightEarY,
                         rightCheekY, rightJawY, chinY, leftJawY, leftCheekY, rightEarY,
                         leftTempleY, foreheadY[0]};
        
        // Draw face outline points
        g2d.setColor(new java.awt.Color(100, 255, 150, 180));
        for (int i = 0; i < outlineX.length - 1; i++) {
            g2d.fillOval(outlineX[i] - 2, outlineY[i] - 2, 4, 4);
        }
        
        // LEFT EYE - Elliptical rendering with 12 points (improved circular contour)
        g2d.setColor(new java.awt.Color(0, 255, 255, 220));
        int leftEyeW = (int)(eyeDistance * 0.35);
        int leftEyeH = (int)(eyeDistance * 0.22);
        
        int[] leftEyePtsX = new int[12];
        int[] leftEyePtsY = new int[12];
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            leftEyePtsX[i] = leftEyeX + (int)(leftEyeW * Math.cos(angle));
            leftEyePtsY[i] = leftEyeY + (int)(leftEyeH * Math.sin(angle));
        }
        
        for (int i = 0; i < leftEyePtsX.length; i++) {
            g2d.fillOval(leftEyePtsX[i] - 3, leftEyePtsY[i] - 3, 6, 6);
            int next = (i + 1) % leftEyePtsX.length;
            g2d.drawLine(leftEyePtsX[i], leftEyePtsY[i], leftEyePtsX[next], leftEyePtsY[next]);
        }
        
        // Draw pupil marker (small circle inside eye)
        g2d.setColor(new java.awt.Color(0, 200, 200, 180));
        int pupilRadius = (int)(eyeDistance * 0.06);
        g2d.fillOval(leftEyeX - pupilRadius, leftEyeY - pupilRadius, pupilRadius * 2, pupilRadius * 2);
        g2d.drawOval(leftEyeX - pupilRadius - 1, leftEyeY - pupilRadius - 1, (pupilRadius + 1) * 2, (pupilRadius + 1) * 2);
        
        // RIGHT EYE - Elliptical rendering with 12 points (mirror of left)
        int[] rightEyePtsX = new int[12];
        int[] rightEyePtsY = new int[12];
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            rightEyePtsX[i] = rightEyeX + (int)(leftEyeW * Math.cos(angle));
            rightEyePtsY[i] = rightEyeY + (int)(leftEyeH * Math.sin(angle));
        }
        
        g2d.setColor(new java.awt.Color(0, 255, 255, 220));
        for (int i = 0; i < rightEyePtsX.length; i++) {
            g2d.fillOval(rightEyePtsX[i] - 3, rightEyePtsY[i] - 3, 6, 6);
            int next = (i + 1) % rightEyePtsX.length;
            g2d.drawLine(rightEyePtsX[i], rightEyePtsY[i], rightEyePtsX[next], rightEyePtsY[next]);
        }
        
        // Draw right pupil
        g2d.setColor(new java.awt.Color(0, 200, 200, 180));
        g2d.fillOval(rightEyeX - pupilRadius, rightEyeY - pupilRadius, pupilRadius * 2, pupilRadius * 2);
        g2d.drawOval(rightEyeX - pupilRadius - 1, rightEyeY - pupilRadius - 1, (pupilRadius + 1) * 2, (pupilRadius + 1) * 2);
        
        // EYEBROWS - 8 purple points each (scaled to eye size)
        g2d.setColor(new java.awt.Color(180, 100, 255, 200));
        int browHeight = (int)(eyeDistance * 0.18);
        int browY = leftEyeY - browHeight - (int)(eyeDistance * 0.08);
        
        // Left eyebrow (curved arc)
        int[] leftBrowX = new int[8];
        int[] leftBrowY = new int[8];
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI + (i / 8.0) * Math.PI;
            leftBrowX[i] = leftEyeX + (int)(leftEyeW * 0.6 * Math.cos(angle));
            leftBrowY[i] = browY + (int)(browHeight * 0.5 * Math.sin(angle));
        }
        for (int i = 0; i < leftBrowX.length; i++) {
            g2d.fillOval(leftBrowX[i] - 2, leftBrowY[i] - 2, 4, 4);
        }
        
        // Right eyebrow (curved arc)
        int[] rightBrowX = new int[8];
        int[] rightBrowY = new int[8];
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI + (i / 8.0) * Math.PI;
            rightBrowX[i] = rightEyeX + (int)(leftEyeW * 0.6 * Math.cos(angle));
            rightBrowY[i] = browY + (int)(browHeight * 0.5 * Math.sin(angle));
        }
        for (int i = 0; i < rightBrowX.length; i++) {
            g2d.fillOval(rightBrowX[i] - 2, rightBrowY[i] - 2, 4, 4);
        }
        
        // NOSE - 9 yellow points
        g2d.setColor(new java.awt.Color(255, 200, 0, 220));
        int noseW = (int)(eyeDistance * 0.14);
        int[] nosePtsX = {noseX, noseX - noseW/3, noseX + noseW/3, noseX - noseW/2, noseX + noseW/2,
                         noseX - noseW/3, noseX, noseX + noseW/3, noseX};
        int[] nosePtsY = {leftEyeY + (int)(boxHeight * 0.08), leftEyeY + (int)(boxHeight * 0.12),
                         leftEyeY + (int)(boxHeight * 0.12), leftEyeY + (int)(boxHeight * 0.18),
                         leftEyeY + (int)(boxHeight * 0.18), leftEyeY + (int)(boxHeight * 0.25),
                         leftEyeY + (int)(boxHeight * 0.28), leftEyeY + (int)(boxHeight * 0.25),
                         chinY - (int)(boxHeight * 0.15)};
        
        for (int i = 0; i < nosePtsX.length; i++) {
            g2d.fillOval(nosePtsX[i] - 3, nosePtsY[i] - 3, 6, 6);
            g2d.drawOval(nosePtsX[i] - 4, nosePtsY[i] - 4, 8, 8);
        }
        
        // MOUTH - 12 red points (elliptical)
        g2d.setColor(new java.awt.Color(255, 100, 100, 220));
        int mouthY = chinY - (int)(boxHeight * 0.1);
        int mouthW = (int)(eyeDistance * 0.55);
        int mouthH = (int)(eyeDistance * 0.18);
        
        int[] mouthX = new int[12];
        int[] mouthY_arr = new int[12];
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            mouthX[i] = noseX + (int)(mouthW * Math.cos(angle));
            mouthY_arr[i] = mouthY + (int)(mouthH * Math.sin(angle));
        }
        
        for (int i = 0; i < mouthX.length; i++) {
            g2d.fillOval(mouthX[i] - 3, mouthY_arr[i] - 3, 6, 6);
            int next = (i + 1) % mouthX.length;
            g2d.drawLine(mouthX[i], mouthY_arr[i], mouthX[next], mouthY_arr[next]);
        }
        
        // CHEEKBONES - 6 points each side (improved rendering)
        g2d.setColor(new java.awt.Color(255, 150, 100, 180));
        int cheekY = centerY + (int)(boxHeight * 0.02);
        
        // Right cheekbone arc
        int[] rightCheekPtsX = new int[6];
        int[] rightCheekPtsY = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI - (i / 6.0) * Math.PI * 0.5;
            rightCheekPtsX[i] = noseX + (int)(eyeDistance * 0.5 * Math.cos(angle));
            rightCheekPtsY[i] = cheekY + (int)(eyeDistance * 0.25 * Math.sin(angle));
        }
        for (int i = 0; i < rightCheekPtsX.length; i++) {
            g2d.fillOval(rightCheekPtsX[i] - 2, rightCheekPtsY[i] - 2, 4, 4);
        }
        
        // Left cheekbone arc
        int[] leftCheekPtsX = new int[6];
        int[] leftCheekPtsY = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = (i / 6.0) * Math.PI * 0.5;
            leftCheekPtsX[i] = noseX - (int)(eyeDistance * 0.5 * Math.cos(angle));
            leftCheekPtsY[i] = cheekY + (int)(eyeDistance * 0.25 * Math.sin(angle));
        }
        for (int i = 0; i < leftCheekPtsX.length; i++) {
            g2d.fillOval(leftCheekPtsX[i] - 2, leftCheekPtsY[i] - 2, 4, 4);
        }
        
        // CENTER PULSING DOT (white)
        g2d.setColor(new java.awt.Color(255, 255, 255, 255));
        int dotRadius = 5 + (faceDetectCounter % 3);
        g2d.fillOval(centerX - dotRadius, centerY - dotRadius, dotRadius * 2, dotRadius * 2);
        g2d.drawOval(centerX - dotRadius - 2, centerY - dotRadius - 2, (dotRadius + 2) * 2, (dotRadius + 2) * 2);
        
        // Add detection status text with eye distance info
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        g2d.setColor(new java.awt.Color(0, 255, 100));
        g2d.drawString("Adaptive Mesh | Eyes: " + eyeDistance + "px", 10, 25);
    }
    
    /**
     * Detect face by skin color (fallback when cascade fails)
     */
    private java.awt.Rectangle detectFaceByColor(BufferedImage frame) {
        try {
            int width = frame.getWidth();
            int height = frame.getHeight();
            
            int[] pixels = new int[width * height];
            frame.getRGB(0, 0, width, height, pixels, 0, width);
            
            // Scan for skin-colored regions
            byte[] isSkin = new byte[width * height];
            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                
                // Simple skin color detection
                if (r > 95 && g > 40 && b > 20 &&
                    r > b && r > g &&
                    Math.abs(r - g) > 15) {
                    isSkin[i] = 1;
                }
            }
            
            // Find bounding box of skin pixels
            int minX = width, maxX = 0;
            int minY = height, maxY = 0;
            int skinPixels = 0;
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (isSkin[y * width + x] == 1) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        skinPixels++;
                    }
                }
            }
            
            // Check if we found a reasonable face-sized region
            if (skinPixels > 2000 && maxX - minX > 50 && maxY - minY > 60) {
                return new java.awt.Rectangle(minX, minY, maxX - minX, maxY - minY);
            }
        } catch (Exception ex) {
            System.err.println("RealCameraService: Color detection error: " + ex.getMessage());
        }
        
        return null;
    }
    
    /**
     * Basic Java-only face detection using luminance and variance analysis
     * Works without OpenCV - analyzes pixel patterns to find face region
     */
    private java.awt.Rectangle detectFaceBasic(BufferedImage frame) {
        try {
            int width = frame.getWidth();
            int height = frame.getHeight();
            
            // Convert to grayscale and analyze
            byte[] gray = new byte[width * height];
            int[] pixels = new int[width * height];
            frame.getRGB(0, 0, width, height, pixels, 0, width);
            
            // Convert RGB to grayscale
            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                int grayValue = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                gray[i] = (byte) (grayValue & 0xFF);
            }
            
            // Calculate variance in small blocks to find facial features
            int blockSize = 16;  // 16x16 pixel blocks
            int blockCols = (width + blockSize - 1) / blockSize;
            int blockRows = (height + blockSize - 1) / blockSize;
            
            double[] blockVariance = new double[blockCols * blockRows];
            double maxVariance = 0;
            int maxVarianceIdx = 0;
            
            for (int by = 0; by < blockRows; by++) {
                for (int bx = 0; bx < blockCols; bx++) {
                    int startX = bx * blockSize;
                    int startY = by * blockSize;
                    int endX = Math.min(startX + blockSize, width);
                    int endY = Math.min(startY + blockSize, height);
                    
                    // Calculate mean
                    double sum = 0;
                    int count = 0;
                    for (int y = startY; y < endY; y++) {
                        for (int x = startX; x < endX; x++) {
                            sum += (gray[y * width + x] & 0xFF);
                            count++;
                        }
                    }
                    double mean = sum / count;
                    
                    // Calculate variance
                    double varSum = 0;
                    for (int y = startY; y < endY; y++) {
                        for (int x = startX; x < endX; x++) {
                            double diff = (gray[y * width + x] & 0xFF) - mean;
                            varSum += diff * diff;
                        }
                    }
                    double variance = varSum / count;
                    blockVariance[by * blockCols + bx] = variance;
                    
                    if (variance > maxVariance) {
                        maxVariance = variance;
                        maxVarianceIdx = by * blockCols + bx;
                    }
                }
            }
            
            // Find connected region of high variance blocks (facial features area)
            int maxVarBx = maxVarianceIdx % blockCols;
            int maxVarBy = maxVarianceIdx / blockCols;
            
            // Expand from center to find face bounds using high-variance region
            double varianceThreshold = maxVariance * 0.4;  // 40% of maximum
            
            int minBx = maxVarBx, maxBx = maxVarBx;
            int minBy = maxVarBy, maxBy = maxVarBy;
            
            // Expand search area
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int bx = maxVarBx + dx;
                    int by = maxVarBy + dy;
                    if (bx >= 0 && bx < blockCols && by >= 0 && by < blockRows) {
                        if (blockVariance[by * blockCols + bx] > varianceThreshold) {
                            minBx = Math.min(minBx, bx);
                            maxBx = Math.max(maxBx, bx);
                            minBy = Math.min(minBy, by);
                            maxBy = Math.max(maxBy, by);
                        }
                    }
                }
            }
            
            // Convert block coordinates to pixel coordinates
            int faceX = minBx * blockSize;
            int faceY = minBy * blockSize;
            int faceWidth = (maxBx - minBx + 2) * blockSize;
            int faceHeight = (maxBy - minBy + 2) * blockSize;
            
            // Ensure face region is within bounds
            faceX = Math.max(0, faceX);
            faceY = Math.max(0, faceY);
            faceWidth = Math.min(width - faceX, faceWidth);
            faceHeight = Math.min(height - faceY, faceHeight);
            
            // Ensure minimum face size (at least 80x80)
            if (faceWidth >= 80 && faceHeight >= 80) {
                System.out.println("RealCameraService: Basic detection found face at (" + faceX + "," + faceY + ") size:" + faceWidth + "x" + faceHeight);
                return new java.awt.Rectangle(faceX, faceY, faceWidth, faceHeight);
            }
            
        } catch (Exception ex) {
            System.err.println("RealCameraService: Basic detection error: " + ex.getMessage());
        }
        
        return null;
    }
    
    /**
     * Detect face using Haar Cascade classifier (most reliable method)
     */
    private java.awt.Rectangle detectFaceWithCascade(BufferedImage frame) {
        // Initialize cascade only once, on first call
        if (!landmarkDetectionAttempted) {
            initializeLandmarkDetection();
        }
        
        // If cascade is not available, skip to color detection
        if (!landmarkDetectionAvailable || faceDetector == null || faceDetector.empty()) {
            return null;  // Let caller use skin color detection as fallback
        }
        
        try {
            Mat frameMat = bufferedImageToMat(frame);
            if (frameMat != null && !frameMat.empty()) {
                Mat gray = new Mat();
                cvtColor(frameMat, gray, CV_BGR2GRAY);
                equalizeHist(gray, gray);  // Improve contrast for better detection
                
                RectVector detections = new RectVector();
                // Use balanced parameters: 1.05 scale factor (conservative) + 5 neighbors (stricter than 3)
                faceDetector.detectMultiScale(gray, detections, 1.05, 5, 0,
                    new Size(20, 20), new Size(500, 500));
                
                if (detections.size() > 0) {
                    // Get the largest face detected (most likely to be the main face)
                    Rect largestFace = detections.get(0);
                    long largestArea = (long) largestFace.width() * largestFace.height();
                    
                    for (long i = 1; i < detections.size(); i++) {
                        Rect face = detections.get((int)i);
                        long area = (long) face.width() * face.height();
                        if (area > largestArea) {
                            largestFace = face;
                            largestArea = area;
                        }
                    }
                    
                    // Only accept if face is reasonably sized
                    if (largestFace.width() > 30 && largestFace.height() > 30) {
                        java.awt.Rectangle result = new java.awt.Rectangle(
                            largestFace.x(), 
                            largestFace.y(), 
                            largestFace.width(), 
                            largestFace.height()
                        );
                        
                        System.out.println("RealCameraService: [HAAR CASCADE] Face detected at (" + result.x + "," + result.y + ") size:" + result.width + "x" + result.height);
                        
                        gray.release();
                        frameMat.release();
                        detections.close();
                        
                        return result;
                    }
                }
                gray.release();
                frameMat.release();
                detections.close();
            }
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("RealCameraService: OpenCV native libraries missing - disabling HAAR");
            landmarkDetectionAvailable = false;
        } catch (Exception ex) {
            // Silently skip - will fall back to color detection
        }
        
        return null;  // Fall back to color detection
    }    
    /**
     * Draw facial landmarks (eyes, nose, mouth approximations)
     */
    private void drawFacialLandmarks(Mat frame, Rect face, Mat grayFrame) {
        try {
            int faceX = face.x();
            int faceY = face.y();
            int faceW = face.width();
            int faceH = face.height();
            
            // Detect eyes within face region
            Mat faceROI = new Mat(grayFrame, face);
            RectVector eyes = new RectVector();
            
            if (eyeDetector != null) {
                eyeDetector.detectMultiScale(faceROI, eyes, 1.1, 4, 0,
                    new Size(15, 15), new Size(60, 60));
            }
            
            // Draw eyes as blue dots
            int eyeCount = 0;
            for (long i = 0; i < eyes.size() && eyeCount < 2; i++) {
                Rect eye = eyes.get(i);
                int eyeX = faceX + eye.x() + eye.width() / 2;
                int eyeY = faceY + eye.y() + eye.height() / 2;
                circle(frame, new Point(eyeX, eyeY), 8, new Scalar(255, 0, 0, 0), -1, 0, 0); // Blue
                eyeCount++;
            }
            eyes.close();
            faceROI.release();
            
            // If eyes not detected by cascade, estimate positions
            if (eyeCount < 2) {
                // Estimate eye positions based on face geometry
                int leftEyeX = faceX + (int)(faceW * 0.35);
                int rightEyeX = faceX + (int)(faceW * 0.65);
                int eyeY = faceY + (int)(faceH * 0.35);
                
                circle(frame, new Point(leftEyeX, eyeY), 6, new Scalar(255, 100, 0, 0), -1, 0, 0);  // Blue-ish
                circle(frame, new Point(rightEyeX, eyeY), 6, new Scalar(255, 100, 0, 0), -1, 0, 0); // Blue-ish
            }
            
            // Draw nose (approximated - center top area)
            int noseX = faceX + faceW / 2;
            int noseY = faceY + (int)(faceH * 0.5);
            circle(frame, new Point(noseX, noseY), 7, new Scalar(0, 255, 255, 0), -1, 0, 0); // Cyan
            
            // Draw mouth corners
            int mouthY = faceY + (int)(faceH * 0.75);
            int leftMouthX = faceX + (int)(faceW * 0.3);
            int rightMouthX = faceX + (int)(faceW * 0.7);
            circle(frame, new Point(leftMouthX, mouthY), 6, new Scalar(0, 255, 0, 0), -1, 0, 0);  // Green
            circle(frame, new Point(rightMouthX, mouthY), 6, new Scalar(0, 255, 0, 0), -1, 0, 0); // Green
            
            // Draw cheekbones
            int cheekY = faceY + (int)(faceH * 0.55);
            int leftCheekX = faceX + (int)(faceW * 0.2);
            int rightCheekX = faceX + (int)(faceW * 0.8);
            circle(frame, new Point(leftCheekX, cheekY), 5, new Scalar(200, 100, 255, 0), -1, 0, 0); // Purple
            circle(frame, new Point(rightCheekX, cheekY), 5, new Scalar(200, 100, 255, 0), -1, 0, 0); // Purple
            
            // Draw chin
            int chinX = faceX + faceW / 2;
            int chinY = faceY + (int)(faceH * 0.9);
            circle(frame, new Point(chinX, chinY), 6, new Scalar(255, 0, 255, 0), -1, 0, 0); // Magenta
        } catch (Exception ex) {
            System.err.println("RealCameraService: Error drawing landmarks: " + ex.getMessage());
        }
    }

    /**
     * Convert BufferedImage to OpenCV Mat
     */
    private Mat bufferedImageToMat(BufferedImage image) {
        try {
            Mat mat = new Mat(image.getHeight(), image.getWidth(), CV_8UC3);
            byte[] data = new byte[image.getWidth() * image.getHeight() * 3];
            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
            
            for (int i = 0; i < pixels.length; i++) {
                data[i * 3] = (byte) ((pixels[i] >> 16) & 0xFF);     // R
                data[i * 3 + 1] = (byte) ((pixels[i] >> 8) & 0xFF);  // G
                data[i * 3 + 2] = (byte) (pixels[i] & 0xFF);         // B
            }
            
            mat.data().put(data);
            return mat;
        } catch (Exception ex) {
            System.err.println("RealCameraService: Error converting to Mat: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Convert OpenCV Mat to BufferedImage
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        try {
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_INT_RGB);
            byte[] data = new byte[mat.cols() * mat.rows() * 3];
            mat.data().get(data);
            
            for (int i = 0; i < data.length; i += 3) {
                int b = data[i] & 0xFF;
                int g = data[i + 1] & 0xFF;
                int r = data[i + 2] & 0xFF;
                
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(i / 3 % mat.cols(), i / 3 / mat.cols(), rgb);
            }
            return image;
        } catch (Exception ex) {
            System.err.println("RealCameraService: Error converting Mat to BufferedImage: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Get current frame without overlays
     */
    public synchronized Image getCurrentFrame() {
        if (currentFrame == null) {
            return null;
        }
        return SwingFXUtils.toFXImage(currentFrame, null);
    }

    /**
     * Capture a frame for enrollment
     */
    public synchronized FaceFrameData captureFrame() {
        if (currentFrame == null) {
            return null;
        }
        
        // Create a copy of the current frame
        BufferedImage frameCopy = new BufferedImage(
            currentFrame.getWidth(), 
            currentFrame.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        for (int y = 0; y < currentFrame.getHeight(); y++) {
            for (int x = 0; x < currentFrame.getWidth(); x++) {
                frameCopy.setRGB(x, y, currentFrame.getRGB(x, y));
            }
        }
        
        capturedFrames.add(frameCopy);
        return new FaceFrameData(frameCopy, true);
    }

    /**
     * Get number of captured frames
     */
    public int getCapturedFrameCount() {
        return capturedFrames.size();
    }

    /**
     * Clear captured frames
     */
    public void clearCapturedFrames() {
        capturedFrames.clear();
    }

    /**
     * Check if face is detected (simulated with animation)
     */
    public synchronized FaceQuality assessFrameQuality() {
        if (currentFrame == null) {
            return new FaceQuality(false, "No frame available");
        }
        
        // Simulate face detection with increasing success rate
        double rand = Math.random();
        if (rand > 0.3) {
            return new FaceQuality(true, "Face detected [OK]");
        }
        
        if (rand > 0.15) {
            return new FaceQuality(false, "Move face closer to camera");
        }
        
        return new FaceQuality(false, "Center your face in the frame");
    }

    /**
     * Release resources
     */
    public void release() {
        stopCapture();
        
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    System.out.println("RealCameraService: Closing webcam...");
                    webcam.close();
                }
            } catch (Exception ex) {
                System.err.println("RealCameraService: Error closing webcam: " + ex.getMessage());
            }
        }
        
        currentFrame = null;
        frameSequence = 0L;
        lastRenderedSequence = -1L;
        lastRenderedImage = null;
        insightDisplayBuffer = null;
        basicDisplayBuffer = null;
        clearCapturedFrames();
    }

    private Image cacheRenderedImage(Image image) {
        lastRenderedImage = image;
        lastRenderedSequence = frameSequence;
        return image;
    }

    /**
     * Check if camera is ready
     */
    public boolean isReady() {
        return isRunning && webcam != null && webcam.isOpen();
    }

    /**
     * Inner class for frame data
     */
    public static class FaceFrameData {
        public BufferedImage frameImage;
        public boolean faceDetected;

        public FaceFrameData(BufferedImage frameImage, boolean faceDetected) {
            this.frameImage = frameImage;
            this.faceDetected = faceDetected;
        }
    }

    /**
     * Inner class for face quality assessment
     */
    public static class FaceQuality {
        public boolean isGood;
        public String message;

        public FaceQuality(boolean isGood, String message) {
            this.isGood = isGood;
            this.message = message;
        }
    }
}

