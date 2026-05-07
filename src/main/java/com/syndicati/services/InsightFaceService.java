package com.syndicati.services;

import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * InsightFace Service Wrapper
 * Communicates with Python InsightFace service over stdio
 * Provides face detection, landmarks, and embeddings
 */
public class InsightFaceService {
    
    private static final String TAG = "InsightFaceService";
    private static final boolean DEBUG = true;
    
    private Process pythonProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private Gson gson;
    private ExecutorService threadPool;
    private boolean isInitialized = false;
    private boolean suppressLogging = false;  // Suppress repeated errors after first failure
    
    public static class FaceMesh {
        public boolean detected;
        public int numLandmarks;
        public double confidence;
        public List<Landmark3D> landmarks;
        public SpoofingAnalysis spoofing;
        
        public static class Landmark3D {
            public double x;  // 0-1 (normalized to image width)
            public double y;  // 0-1 (normalized to image height)
            public double z;  // depth in normalized space
            public double visibility;
            
            public int getPixelX(int imageWidth) {
                return (int) (x * imageWidth);
            }
            
            public int getPixelY(int imageHeight) {
                return (int) (y * imageHeight);
            }
        }
    }
    
    public static class SpoofingAnalysis {
        public boolean isSpoof;
        public double spoofingScore;  // 0.0 (real) to 1.0 (spoof)
        public List<String> indicators;
        public double depthVariance;
        public double blurVariance;
        public double eyeVisibility;
        public double textureUniformity;
    }
    
    /**
     * Initialize the InsightFace Python service
     * @return true if initialization successful
     */
    public synchronized boolean initialize() {
        if (isInitialized) return true;
        
        try {
            // Find Python executable
            String pythonCmd = findPythonExecutable();
            if (pythonCmd == null) {
                log("ERROR: Python not found in system PATH");
                log("  Install Python 3.8+ and add to PATH, or install from python.org");
                log("  Then install: pip install opencv-python numpy");
                return false;
            }
            
            log("Found Python: " + pythonCmd);
            
            // Check if required packages are installed
            if (!checkPythonPackages(pythonCmd)) {
                log("ERROR: Required Python packages not installed");
                log("  Run: pip install opencv-python numpy");
                return false;
            }
            
            String scriptPath = new File("face_detect_service.py").getAbsolutePath();
            if (!new File(scriptPath).exists()) {
                log("ERROR: face_detect_service.py not found at: " + scriptPath);
                return false;
            }
            
            log("Starting lightweight face detection service from: " + scriptPath);
            
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptPath);
            pb.directory(new File("."));  // Run from project root
            pb.redirectErrorStream(false);  // Keep stdout and stderr separate
            
            pythonProcess = pb.start();
            processInput = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream(), "UTF-8"));
            processOutput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), "UTF-8"));
            
            // Start thread to capture stderr for debugging
            captureProcessOutput();
            
            threadPool = Executors.newFixedThreadPool(2);
            gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            
            // Give service a moment to initialize before pinging
            Thread.sleep(200);
            
            // Test connection with ping - service starts quickly since it's lightweight
            // Keep trying for up to 30 seconds with more detailed logging
            boolean connected = false;
            for (int i = 0; i < 60; i++) {  // 60 attempts = 30 seconds (500ms each)
                try {
                    if (ping()) {
                        log("[OK] Face detection service initialized successfully (attempt " + (i+1) + ")");
                        isInitialized = true;
                        connected = true;
                        return true;
                    }
                } catch (Exception e) {
                    // Continue retrying
                }
                
                if (i % 10 == 0 && i > 0) {
                    log("Waiting for service... (attempt " + (i+1) + "/60)");
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log("ERROR: Failed to connect to face detection service (timeout after 30 seconds)");
            log("  Python service may have crashed - check console for errors");
            log("  Try running: pip install opencv-python numpy");
            suppressLogging = true;  // Suppress future errors
            cleanup();
            
            return false;
            
        } catch (Exception e) {
            log("ERROR initializing InsightFace service: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Capture Python process stdout/stderr for debugging
     */
    private void captureProcessOutput() {
        // Capture stderr for errors and diagnostics
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pythonProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Log all stderr output from Python
                    System.out.println("[InsightFaceService-Python] " + line);
                }
            } catch (Exception e) {
                // Ignore
            }
        });
        stderrThread.setDaemon(true);
        stderrThread.setName("InsightFace-Stderr");
        stderrThread.start();
    }
    
    /**
     * Check if Python has required packages
     */
    private boolean checkPythonPackages(String pythonCmd) {
        try {
            // Check only essential packages - the new service doesn't need insightface
            String[] packages = {"cv2", "numpy"};
            String[] importCmds = {
                "import cv2",
                "import numpy"
            };
            
            for (int i = 0; i < packages.length; i++) {
                ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-c", importCmds[i]);
                pb.redirectErrorStream(true);
                Process checkProcess = pb.start();
                
                int exitCode = checkProcess.waitFor();
                if (exitCode != 0) {
                    log("  Missing or broken package: " + packages[i]);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log("  Could not verify Python packages: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Process a camera frame and detect faces
     * @param frame BufferedImage from webcam
     * @param checkSpoofing whether to analyze for spoofing
     * @return Face data with landmarks, or null if detection failed
     */
    public FaceMesh processFaceFrame(BufferedImage frame, boolean checkSpoofing) {
        if (frame == null) {
            if (!suppressLogging) {
                log("ERROR: Frame is null");
            }
            return null;
        }
        
        if (!isInitialized) {
            // Silently fail if not initialized (avoid spam after startup failure)
            return null;
        }
        
        try {
            // Encode frame to base64 JPEG
            String base64Frame = bufferedImageToBase64(frame);
            
            // Create JSON-RPC 2.0 request
            JsonObject request = new JsonObject();
            request.addProperty("jsonrpc", "2.0");
            request.addProperty("method", "detect");
            
            JsonObject params = new JsonObject();
            params.addProperty("frame", base64Frame);
            request.add("params", params);
            request.addProperty("id", 1);
            
            // Send to Python service
            synchronized (processInput) {
                processInput.write(gson.toJson(request));
                processInput.newLine();
                processInput.flush();
            }
            
            // Read response
            String responseLine = null;
            synchronized (processOutput) {
                responseLine = processOutput.readLine();
            }
            
            if (responseLine == null) {
                log("ERROR: No response from InsightFace service");
                return null;
            }
            
            // Parse JSON-RPC 2.0 response
            JsonObject response = JsonParser.parseString(responseLine).getAsJsonObject();
            
            // Check for error
            if (response.has("error")) {
                log("ERROR: " + response.get("error").toString());
                return null;
            }
            
            if (!response.has("result")) {
                log("ERROR: Invalid response format");
                return null;
            }
            
            // Deserialize to FaceMesh object
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(FaceMesh.class, new FaceMeshDeserializer())
                .create();
            
            FaceMesh mesh = gson.fromJson(response.get("result"), FaceMesh.class);
            
            if (DEBUG && mesh.detected) {
                log("Face detected: " + mesh.numLandmarks + " landmarks, confidence: " 
                    + String.format("%.2f", mesh.confidence));
                if (mesh.spoofing != null) {
                    log("Spoofing score: " + String.format("%.2f", mesh.spoofing.spoofingScore) 
                        + " (is_spoof: " + mesh.spoofing.isSpoof + ")");
                }
            }
            
            return mesh;
            
        } catch (Exception e) {
            log("ERROR processing frame: " + e.getMessage());
            if (DEBUG) e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Draw 3D face mesh visualization on graphics context
     */
    public static void drawFaceMesh(java.awt.Graphics2D g2d, FaceMesh mesh, int imageWidth, int imageHeight) {
        if (mesh == null || !mesh.detected || mesh.landmarks == null) {
            return;
        }
        
        // Draw mesh connections (face contour, eyes, nose, mouth, etc.)
        drawMeshConnections(g2d, mesh.landmarks, imageWidth, imageHeight);
        
        // Draw landmark points
        drawLandmarkPoints(g2d, mesh.landmarks, imageWidth, imageHeight);
        
        // Draw spoofing indicator if available
        if (mesh.spoofing != null) {
            drawSpoofingIndicator(g2d, mesh.spoofing, imageWidth, imageHeight);
        }
    }
    
    private static void drawMeshConnections(java.awt.Graphics2D g2d, List<FaceMesh.Landmark3D> landmarks,
                                            int imageWidth, int imageHeight) {
        // Key face connections (simplified mesh)
        int[][] connections = {
            // Face contour (upper)
            {10, 338}, {338, 297}, {297, 332}, {332, 284}, {284, 251},
            // Face contour (lower)
            {251, 389}, {389, 356}, {356, 454}, {454, 323}, {323, 361},
            // Lips
            {78, 191}, {191, 80}, {80, 81}, {81, 82}, {82, 13},
            // Left eye
            {263, 249}, {249, 390}, {390, 373}, {373, 374}, {374, 380}, {380, 381}, {381, 382},
            // Right eye
            {33, 7}, {7, 163}, {163, 144}, {144, 145}, {145, 153}, {153, 154}, {154, 155},
            // Nose
            {9, 4}, {4, 6}, {6, 5}
        };
        
        g2d.setColor(new java.awt.Color(0, 255, 0, 100));
        g2d.setStroke(new java.awt.BasicStroke(1.5f));
        
        for (int[] connection : connections) {
            if (connection[0] < landmarks.size() && connection[1] < landmarks.size()) {
                FaceMesh.Landmark3D lm1 = landmarks.get(connection[0]);
                FaceMesh.Landmark3D lm2 = landmarks.get(connection[1]);
                
                // Filter based on visibility
                if (lm1.visibility > 0.3 && lm2.visibility > 0.3) {
                    g2d.drawLine(
                        (int) (lm1.x * imageWidth),
                        (int) (lm1.y * imageHeight),
                        (int) (lm2.x * imageWidth),
                        (int) (lm2.y * imageHeight)
                    );
                }
            }
        }
    }
    
    private static void drawLandmarkPoints(java.awt.Graphics2D g2d, List<FaceMesh.Landmark3D> landmarks,
                                          int imageWidth, int imageHeight) {
        // Draw all 468 landmarks as small circles
        g2d.setColor(new java.awt.Color(255, 100, 100, 200));
        
        int pointSize = 2;
        for (int i = 0; i < Math.min(landmarks.size(), 468); i++) {
            FaceMesh.Landmark3D lm = landmarks.get(i);
            
            if (lm.visibility > 0.3) {
                int x = (int) (lm.x * imageWidth);
                int y = (int) (lm.y * imageHeight);
                g2d.fillOval(x - pointSize, y - pointSize, pointSize * 2, pointSize * 2);
            }
        }
        
        // Highlight key facial features
        drawKeyFeatures(g2d, landmarks, imageWidth, imageHeight);
    }
    
    private static void drawKeyFeatures(java.awt.Graphics2D g2d, List<FaceMesh.Landmark3D> landmarks,
                                       int imageWidth, int imageHeight) {
        // Eyes centers
        int leftEyeId = 33;
        int rightEyeId = 133;
        int noseId = 9;
        
        g2d.setColor(new java.awt.Color(0, 255, 255, 255));
        int sizeKey = 4;
        
        if (leftEyeId < landmarks.size()) {
            FaceMesh.Landmark3D eye = landmarks.get(leftEyeId);
            int x = (int) (eye.x * imageWidth);
            int y = (int) (eye.y * imageHeight);
            g2d.fillOval(x - sizeKey, y - sizeKey, sizeKey * 2, sizeKey * 2);
            g2d.drawOval(x - sizeKey - 2, y - sizeKey - 2, (sizeKey + 2) * 2, (sizeKey + 2) * 2);
        }
        
        if (rightEyeId < landmarks.size()) {
            FaceMesh.Landmark3D eye = landmarks.get(rightEyeId);
            int x = (int) (eye.x * imageWidth);
            int y = (int) (eye.y * imageHeight);
            g2d.fillOval(x - sizeKey, y - sizeKey, sizeKey * 2, sizeKey * 2);
            g2d.drawOval(x - sizeKey - 2, y - sizeKey - 2, (sizeKey + 2) * 2, (sizeKey + 2) * 2);
        }
        
        if (noseId < landmarks.size()) {
            FaceMesh.Landmark3D nose = landmarks.get(noseId);
            int x = (int) (nose.x * imageWidth);
            int y = (int) (nose.y * imageHeight);
            g2d.setColor(new java.awt.Color(255, 200, 0, 255));
            g2d.fillOval(x - sizeKey, y - sizeKey, sizeKey * 2, sizeKey * 2);
        }
    }
    
    private static void drawSpoofingIndicator(java.awt.Graphics2D g2d, SpoofingAnalysis spoofing,
                                             int imageWidth, int imageHeight) {
        // Color code: green = real, yellow = suspicious, red = likely spoof
        java.awt.Color indicatorColor;
        String text;
        
        if (spoofing.isSpoof) {
            indicatorColor = new java.awt.Color(255, 0, 0, 200);
            text = String.format("[WARN] SPOOF DETECTED (%.0f%%)", spoofing.spoofingScore * 100);
        } else if (spoofing.spoofingScore > 0.3) {
            indicatorColor = new java.awt.Color(255, 255, 0, 200);
            text = String.format("? SUSPICIOUS (%.0f%%)", spoofing.spoofingScore * 100);
        } else {
            indicatorColor = new java.awt.Color(0, 255, 0, 200);
            text = "[OK] REAL FACE";
        }
        
        g2d.setColor(indicatorColor);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        g2d.drawString(text, 10, 25);
    }
    
    private boolean ping() {
        try {
            JsonObject pingRequest = new JsonObject();
            pingRequest.addProperty("jsonrpc", "2.0");
            pingRequest.addProperty("method", "ping");
            pingRequest.addProperty("id", 1);
            
            String requestStr = gson.toJson(pingRequest);
            
            synchronized (processInput) {
                processInput.write(requestStr);
                processInput.newLine();
                processInput.flush();
            }
            
            String response = null;
            // Use a timeout for reading response - wait up to 2 seconds
            long startTime = System.currentTimeMillis();
            while (response == null && System.currentTimeMillis() - startTime < 2000) {
                synchronized (processOutput) {
                    if (processOutput.ready()) {
                        response = processOutput.readLine();
                        if (response != null && response.contains("pong")) {
                            return true;
                        }
                    }
                }
                Thread.sleep(50);  // Check every 50ms
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String findPythonExecutable() {
        String[] candidates = {"python3", "python", "python.exe", "python3.exe"};
        String pathEnv = System.getenv("PATH");
        
        // First, try PATH environment variable
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            for (String candidate : candidates) {
                for (String path : paths) {
                    File executable = new File(path, candidate);
                    if (executable.exists() && executable.isFile()) {
                        log("  Found " + candidate + " in PATH: " + executable.getAbsolutePath());
                        return executable.getAbsolutePath();
                    }
                }
            }
        }
        
        // On Windows, also check common installation locations
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            String[] winPaths = {
                "C:\\Python314\\python.exe",  // Python 3.14
                "C:\\Python313\\python.exe",  // Python 3.13
                "C:\\Python312\\python.exe",  // Python 3.12
                "C:\\Python311\\python.exe",  // Python 3.11
                "C:\\Python310\\python.exe",  // Python 3.10
                "C:\\Python39\\python.exe",   // Python 3.9
                "C:\\Program Files\\Python314\\python.exe",
                "C:\\Program Files\\Python313\\python.exe",
                "C:\\Program Files\\Python312\\python.exe",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python314\\python.exe",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python313\\python.exe"
            };
            
            for (String pythonPath : winPaths) {
                if (pythonPath != null) {
                    File executable = new File(pythonPath);
                    if (executable.exists()) {
                        log("  Found Python at: " + pythonPath);
                        return pythonPath;
                    }
                }
            }
        }
        
        return null;
    }
    
    private String bufferedImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    public synchronized void shutdown() {
        if (pythonProcess != null) {
            try {
                processInput.close();
                processOutput.close();
                pythonProcess.destroy();
                if (threadPool != null) {
                    threadPool.shutdown();
                    threadPool.awaitTermination(5, TimeUnit.SECONDS);
                }
                log("Face detection service shut down successfully");
            } catch (Exception e) {
                // Suppress shutdown errors if we already failed initialization
                if (!suppressLogging) {
                    log("ERROR shutting down: " + e.getMessage());
                }
            }
        }
        isInitialized = false;
    }
    
    private void cleanup() {
        shutdown();
    }
    
    public boolean isRunning() {
        return isInitialized && pythonProcess != null && pythonProcess.isAlive();
    }
    
    private static void log(String message) {
        System.out.println("[" + TAG + "] " + message);
    }
    
    // Custom deserializer for FaceMesh
    private static class FaceMeshDeserializer implements com.google.gson.JsonDeserializer<FaceMesh> {
        @Override
        public FaceMesh deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                   com.google.gson.JsonDeserializationContext context)
                throws com.google.gson.JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            FaceMesh mesh = new FaceMesh();
            
            mesh.detected = obj.get("detected").getAsBoolean();
            mesh.confidence = obj.get("confidence").getAsDouble();
            
            if (mesh.detected) {
                mesh.numLandmarks = obj.get("num_landmarks") != null ? 
                    obj.get("num_landmarks").getAsInt() : 0;
                
                mesh.landmarks = new ArrayList<>();
                JsonArray landmarksArray = obj.getAsJsonArray("landmarks");
                for (JsonElement element : landmarksArray) {
                    JsonObject lmObj = element.getAsJsonObject();
                    FaceMesh.Landmark3D landmark = new FaceMesh.Landmark3D();
                    landmark.x = lmObj.get("x").getAsDouble();
                    landmark.y = lmObj.get("y").getAsDouble();
                    landmark.z = lmObj.get("z").getAsDouble();
                    landmark.visibility = lmObj.get("visibility").getAsDouble();
                    mesh.landmarks.add(landmark);
                }
                
                if (obj.has("spoofing") && !obj.get("spoofing").isJsonNull()) {
                    JsonObject spoofObj = obj.getAsJsonObject("spoofing");
                    mesh.spoofing = new SpoofingAnalysis();
                    mesh.spoofing.isSpoof = spoofObj.get("is_spoof").getAsBoolean();
                    mesh.spoofing.spoofingScore = spoofObj.get("spoofing_score").getAsDouble();
                    mesh.spoofing.depthVariance = spoofObj.get("depth_variance").getAsDouble();
                    mesh.spoofing.blurVariance = spoofObj.get("blur_variance").getAsDouble();
                    mesh.spoofing.eyeVisibility = spoofObj.get("eye_visibility").getAsDouble();
                    mesh.spoofing.textureUniformity = spoofObj.get("texture_uniformity").getAsDouble();
                    
                    mesh.spoofing.indicators = new ArrayList<>();
                    JsonArray indicatorsArray = spoofObj.getAsJsonArray("indicators");
                    for (JsonElement elem : indicatorsArray) {
                        mesh.spoofing.indicators.add(elem.getAsString());
                    }
                }
            } else {
                mesh.landmarks = new ArrayList<>();
            }
            
            return mesh;
        }
    }
}

