package com.syndicati.controllers.biometric;

import com.syndicati.services.biometric.RealCameraService;

/**
 * Camera controller to keep camera service lifecycle out of view classes.
 */
public class CameraController {

    public RealCameraService getOrCreate(RealCameraService current) {
        if (current != null) {
            return current;
        }
        return new RealCameraService();
    }

    public boolean initializeDefaultCamera(RealCameraService service) {
        if (service == null) {
            return false;
        }
        return service.initializeCamera(0);
    }
}