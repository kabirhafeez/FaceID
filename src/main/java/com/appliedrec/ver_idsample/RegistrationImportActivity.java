package com.appliedrec.ver_idsample;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.appliedrec.ver_id.UserManager;
import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.model.FaceTemplate;
import com.appliedrec.ver_id.model.VerIDUser;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Activity that displays downloaded profile picture and registers downloaded face templates
 */
public class RegistrationImportActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "com.appliedrec.EXTRA_IMAGE_URI";
    public static final String EXTRA_FACE_TEMPLATES_PATH = "com.appliedred.EXTRA_FACE_TEMPLATES_PATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_import);
        final String faceTemplatesPath;
        if (getIntent() != null) {
            Uri imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
            faceTemplatesPath = getIntent().getStringExtra(EXTRA_FACE_TEMPLATES_PATH);
            if (imageUri != null) {
                Bitmap image = BitmapFactory.decodeFile(imageUri.getPath());
                if (image != null) {
                    ((ImageView)findViewById(R.id.imageView)).setImageBitmap(image);
                }
            }
        } else {
            faceTemplatesPath = null;
        }
        final CheckBox checkBox = findViewById(R.id.checkBox);
        try {
            if (!VerID.shared.isUserRegistered(VerIDUser.DEFAULT_USER_ID)) {
                checkBox.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean overwrite = checkBox.getVisibility() == View.VISIBLE && checkBox.isChecked();
                importRegistration(faceTemplatesPath, overwrite);
            }
        });
    }

    /// Begins the import of the downloaded templates
    private void importRegistration(final String faceTemplatesPath, final boolean overwrite) {
        if (overwrite) {
            UserManager userManager = new UserManager();
            // If overwriting the user delete the existing registration first
            userManager.removeUser(VerIDUser.DEFAULT_USER_ID, new UserManager.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    amendRegistration(faceTemplatesPath);
                }

                @Override
                public void onFailure(Exception exception) {
                    RegistrationImportActivity.this.onFailure();
                }
            });
        } else {
            amendRegistration(faceTemplatesPath);
        }
    }

    /// Amends registration with the supplied face templates
    private void amendRegistration(final String faceTemplatesPath) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Gson gson = new Gson();
                    File faceTemplatesFile = new File(faceTemplatesPath);
                    InputStream inputStream = new FileInputStream(faceTemplatesFile);
                    JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
                    ArrayList<FaceTemplate> templates = new ArrayList<>();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        FaceTemplate template = gson.fromJson(reader, FaceTemplate.class);
                        templates.add(template);
                    }
                    reader.endArray();
                    reader.close();
                    FaceTemplate[] faceTemplates = new FaceTemplate[templates.size()];
                    templates.toArray(faceTemplates);
                    UserManager userManager = new UserManager();
                    userManager.assignFaceTemplatesToUser(faceTemplates, VerIDUser.DEFAULT_USER_ID);
                    faceTemplatesFile.delete();
                    Uri imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
                    if (imageUri != null) {
                        VerID.shared.saveImageUriAsProfilePictureForUser(imageUri, VerIDUser.DEFAULT_USER_ID);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onFailure();
                        }
                    });
                }
            }
        });
    }

    private void onFailure() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.failed_to_import_registration)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void finish() {
        super.finish();
        Uri imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (imageUri != null) {
            // Delete the downloaded profile picture
            new File(imageUri.getPath()).delete();
        }
    }
}
