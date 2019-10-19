package com.appliedrec.ver_idsample;

import android.graphics.Bitmap;

import com.appliedrec.ver_id.model.FaceTemplate;

public class RegistrationData {

    private FaceTemplate[] faceTemplates;
    private Bitmap profilePicture;

    public FaceTemplate[] getFaceTemplates() {
        return faceTemplates;
    }

    public Bitmap getProfilePicture() {
        return profilePicture;
    }

    public void setFaceTemplates(FaceTemplate[] faceTemplates) {
        this.faceTemplates = faceTemplates;
    }

    public void setProfilePicture(Bitmap profilePicture) {
        this.profilePicture = profilePicture;
    }
}
