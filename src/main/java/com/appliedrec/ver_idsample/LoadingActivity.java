package com.appliedrec.ver_idsample;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.loaders.VerIDLoaderResponse;
import com.appliedrec.ver_id.loaders.VerIDUserPictureUriLoader;
import com.appliedrec.ver_id.loaders.VerIDUsersLoader;
import com.appliedrec.ver_id.model.VerIDUser;

public class LoadingActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<VerIDLoaderResponse> {

    private static final int USER_LOADER_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportLoaderManager().initLoader(USER_LOADER_ID, null, this).forceLoad();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getSupportLoaderManager().destroyLoader(USER_LOADER_ID);
    }

    private void showIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void showRegisteredUser(VerIDUser user) {
        Intent intent = new Intent(this, RegisteredUserActivity.class);
        intent.putExtra(RegisteredUserActivity.EXTRA_USER, user);
        startActivity(intent);
        finish();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == USER_LOADER_ID) {
            // Loads registered users
            return new VerIDUsersLoader(this);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<VerIDLoaderResponse> loader, VerIDLoaderResponse data) {
        if (data != null && data.getException() != null && ((data.getException() instanceof IllegalStateException) || (data.getException().getCause() != null && data.getException().getCause() instanceof IllegalStateException))) {
            // Unable to load Ver-ID
            throw new RuntimeException(getString(R.string.verid_failed_to_load), ((VerIDLoaderResponse)data).getException());
        }
        if (loader.getId() == USER_LOADER_ID) {
            if (data.getException() != null) {
                throw new RuntimeException(getString(R.string.failed_to_load_users), data.getException());
            }
            if (data.getResult() == null) {
                throw new RuntimeException(getString(R.string.unexpected_error));
            }
            // Update preferences
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                    .putString(getResources().getString(R.string.pref_key_security_level), Integer.toString(VerID.shared.getSecurityLevel().ordinal()))
                    .apply();
            try {
                VerIDUser[] users = (VerIDUser[]) data.getResult();
                if (users != null && users.length > 0) {
                    showRegisteredUser(users[0]);
                } else {
                    showIntro();
                }
            } catch (ClassCastException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }
}
