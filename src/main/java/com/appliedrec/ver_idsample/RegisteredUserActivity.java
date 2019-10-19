package com.appliedrec.ver_idsample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.appliedrec.ver_id.UserManager;
import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDSessionIntent;
import com.appliedrec.ver_id.model.FaceTemplate;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDAuthenticationSessionSettings;
import com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.VerIDActivity;

import java.net.URL;

public class RegisteredUserActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks {

    public static final String EXTRA_USER = "com.appliedrec.verid.user";
    private static final int AUTHENTICATION_REQUEST_CODE = 0;
    private static final int REGISTRATION_REQUEST_CODE = 1;
    private static final int QR_CODE_SCAN_REQUEST_CODE = 2;
    private static final int LOADER_ID_REGISTRATION_IMPORT = 0;

    VerIDUser user;

    private AlertDialog tempDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_user);
        if (getIntent() != null) {
            user = getIntent().getParcelableExtra(EXTRA_USER);
            if (user != null) {
                loadProfilePicture();
            }
        }
        findViewById(R.id.removeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterUser();
            }
        });
        findViewById(R.id.authenticate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticate();
            }
        });
        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerMoreFaces();
            }
        });
        // Show the registration import button if the app handles registration downloads
        findViewById(R.id.import_registration).setVisibility(((SampleApplication)getApplication()).getRegistrationDownload() != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.import_registration).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importRegistration();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // To inspect the result of the session:
        if (resultCode == RESULT_OK && data != null && (requestCode == REGISTRATION_REQUEST_CODE || requestCode == AUTHENTICATION_REQUEST_CODE)) {
            VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            // See documentation at
            // https://appliedrecognition.github.io/Ver-ID-Android-Sample/com.appliedrec.ver_id.session.VerIDSessionResult.html
        } else if (resultCode == RESULT_OK && data != null && requestCode == QR_CODE_SCAN_REQUEST_CODE && data.hasExtra(Intent.EXTRA_TEXT)) {
            getSupportLoaderManager().restartLoader(LOADER_ID_REGISTRATION_IMPORT, data.getExtras(), this).forceLoad();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.registered_user, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SampleApplication app = (SampleApplication)getApplication();
        menu.findItem(R.id.action_export_registration).setVisible(app.getRegistrationUpload() != null && app.getQRCodeGenerator() != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                showIntro();
                return true;
            case R.id.action_settings:
                showSettings();
                return true;
            case R.id.action_export_registration:
                exportRegistration();
                return true;
        }
        return false;
    }

    private void loadProfilePicture() {
        final ImageView profileImageView = findViewById(R.id.profileImage);
        profileImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    profileImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    profileImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                final int width = profileImageView.getWidth();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap colourBitmap = VerID.shared.getUserProfilePicture(user.getUserId());
                        if (colourBitmap != null) {
                            byte[] grayscale = VerID.shared.getPlatformUtils().bitmapToGrayscale(colourBitmap, ExifInterface.ORIENTATION_NORMAL);
                            Bitmap grayscaleBitmap;
                            if (grayscale != null) {
                                grayscaleBitmap = VerID.shared.getPlatformUtils().grayscaleToBitmap(grayscale, colourBitmap.getWidth(), colourBitmap.getHeight());
                            } else {
                                grayscaleBitmap = colourBitmap;
                            }
                            if (grayscaleBitmap != null) {
                                int size = Math.min(grayscaleBitmap.getWidth(), grayscaleBitmap.getHeight());
                                int x = (int) ((double) grayscaleBitmap.getWidth() / 2.0 - (double) size / 2.0);
                                int y = (int) ((double) grayscaleBitmap.getHeight() / 2.0 - (double) size / 2.0);
                                grayscaleBitmap = Bitmap.createBitmap(grayscaleBitmap, x, y, size, size);
                                grayscaleBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, width, width, true);
                                final RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), grayscaleBitmap);
                                roundedBitmapDrawable.setCornerRadius((float) width / 2f);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        profileImageView.setImageDrawable(roundedBitmapDrawable);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    private void showIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        intent.putExtra(IntroActivity.EXTRA_SHOW_REGISTRATION, false);
        startActivity(intent);
    }

    private void showSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    //region Authentication

    private void authenticate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // Note that strict liveness detection requires the user to also be registered with the STRICT level of liveness detection. This negatively affects the user experience.
        VerIDAuthenticationSessionSettings settings = new VerIDAuthenticationSessionSettings(user.getUserId(), livenessDetection);
        // This setting dictates how many poses the user will be required to move her/his head to to ensure liveness
        // The higher the count the more confident we can be of a live face at the expense of usability
        // Note that 1 is added to the setting to include the initial mandatory straight pose
        settings.numberOfResultsToCollect = Integer.parseInt(preferences.getString(getString(R.string.pref_key_required_pose_count), "1")) + 1;
        if (settings.numberOfResultsToCollect == 1) {
            // Turn off liveness detection if only one pose is requested
            settings.setLivenessDetection(VerID.LivenessDetection.NONE);
        }
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        try {
            if (VerID.shared.canUserAuthenticateWithSettings(user.getUserId(), settings)) {
                Intent intent = new VerIDSessionIntent(this, settings);
                startActivityForResult(intent, AUTHENTICATION_REQUEST_CODE);
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.unable_to_authenticate_with_settings)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                registerMoreFaces(VerID.LivenessDetection.STRICT);
                            }
                        })
                        .create()
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //endregion

    //region Registration

    private void registerMoreFaces() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        // If your application requires an extra level of confidence on liveness detection set the livenessDetection parameter to VerID.LivenessDetection.STRICT.
        // This negatively affects the user experience at registration.
        registerMoreFaces(livenessDetection);
    }

    private void registerMoreFaces(VerID.LivenessDetection livenessDetection) {
        VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(VerIDUser.DEFAULT_USER_ID, livenessDetection);
        // Setting showResult to false will prevent the activity from displaying a result at the end of the session
        settings.showResult = true;
        settings.appendIfUserExists = true;
        Intent intent = new VerIDSessionIntent(this, settings);
        startActivityForResult(intent, REGISTRATION_REQUEST_CODE);
    }

    private void unregisterUser() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_unregister)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.unregister, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            VerID.shared.deregisterUser(user.getUserId());
                            Intent intent = new Intent(RegisteredUserActivity.this, IntroActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .create()
                .show();
    }
    //endregion

    //region Registration import and export

    private void importRegistration() {
        // If you want to be able to import face registrations from other devices create an activity
        // that scans a QR code and returns a URL string in its intent's Intent.EXTRA_TEXT extra.
        Intent intent = new Intent("com.appliedrec.ACTION_SCAN_QR_CODE");
        startActivityForResult(intent, QR_CODE_SCAN_REQUEST_CODE);
    }

    private void exportRegistration() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.share_registration)
                .setMessage(R.string.app_will_generate_code)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.generate_code, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        uploadRegistration();
                    }
                })
                .create()
                .show();
    }

    private void uploadRegistration() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.exporting_registration)
                .setView(new ProgressBar(this))
                .create();
        alertDialog.show();
        UserManager userManager = new UserManager();
        userManager.getFaceTemplatesForUser(VerIDUser.DEFAULT_USER_ID, new UserManager.Callback<FaceTemplate[]>() {
            @Override
            public void onSuccess(final FaceTemplate[] result) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bitmap profilePicture = VerID.shared.getUserProfilePicture(VerIDUser.DEFAULT_USER_ID);
                            RegistrationData registrationData = new RegistrationData();
                            registrationData.setProfilePicture(profilePicture);
                            registrationData.setFaceTemplates(result);
                            SampleApplication app = (SampleApplication) getApplication();
                            URL exportURL = app.getRegistrationUpload().uploadRegistration(registrationData);
                            final Bitmap bitmap = app.getQRCodeGenerator().generateQRCode(exportURL.toString());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog.dismiss();
                                    ImageView imageView = new ImageView(RegisteredUserActivity.this);
                                    imageView.setImageBitmap(bitmap);
                                    new AlertDialog.Builder(RegisteredUserActivity.this)
                                            .setView(imageView)
                                            .setTitle(R.string.scan_to_import)
                                            .setPositiveButton(R.string.done, null)
                                            .create()
                                            .show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog.dismiss();
                                    showExportError();
                                }
                            });
                        }
                    }
                });

            }

            @Override
            public void onFailure(Exception exception) {
                alertDialog.hide();
                showExportError();
            }
        });
    }

    private void showExportError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.failed_to_export_registration)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private void showImportError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.failed_to_import_registration)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID_REGISTRATION_IMPORT) {
            tempDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.downloading)
                    .setView(new ProgressBar(this))
                    .create();
            tempDialog.show();
            String url = args.getString(Intent.EXTRA_TEXT);
            return new RegistrationImportLoader(this, url, ((SampleApplication)getApplication()).getRegistrationDownload());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        if (loader.getId() == LOADER_ID_REGISTRATION_IMPORT) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tempDialog != null) {
                        tempDialog.dismiss();
                        tempDialog = null;
                    }
                }
            });
            if (data != null && data instanceof Bundle) {
                final Bundle extras = (Bundle) data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(RegisteredUserActivity.this, RegistrationImportActivity.class);
                        intent.putExtras(extras);
                        startActivity(intent);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showImportError();
                    }
                });
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
    //endregion
}
