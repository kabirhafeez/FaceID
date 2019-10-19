package com.appliedrec.ver_idsample;

import android.graphics.Bitmap;

public interface IQRCodeGenerator {

    Bitmap generateQRCode(String payload) throws Exception;
}
