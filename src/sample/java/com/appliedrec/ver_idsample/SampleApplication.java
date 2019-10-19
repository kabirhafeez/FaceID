package com.appliedrec.ver_idsample;

import android.app.Application;

public class SampleApplication extends Application {

    /**
     * Return a class that implements IRegistrationUpload if you want to enable registration uploads
     */
    public IRegistrationUpload getRegistrationUpload() {
        return null;
    }

    /**
     * Return a class that implements IRegistrationDownload if you want to enable registration downloads
     */
    public IRegistrationDownload getRegistrationDownload() {
        return null;
    }

    /**
     * Return a class that implements IQRCodeGenerator if you want to enable generating QR codes for registration sharing
     */
    public IQRCodeGenerator getQRCodeGenerator() {
        return null;
    }
}
