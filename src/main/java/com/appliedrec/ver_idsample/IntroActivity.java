package com.appliedrec.ver_idsample;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDSessionIntent;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDRegistrationSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.PageViewActivity;
import com.appliedrec.ver_id.ui.VerIDActivity;

public class IntroActivity extends PageViewActivity implements LoaderManager.LoaderCallbacks {

    private static final int REQUEST_CODE_REGISTER = 0;
    public static final String EXTRA_SHOW_REGISTRATION = "showRegistration";
    private static final int QR_CODE_SCAN_REQUEST_CODE = 1;
    private static final int LOADER_ID_IMPORT_REGISTRATION = 0;
    private static final int REQUEST_CODE_IMPORT = 2;
    boolean showRegistration = true;
    private AlertDialog tempDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showRegistration = getIntent().getBooleanExtra(EXTRA_SHOW_REGISTRATION, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.intro, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_import).setVisible(showRegistration && ((SampleApplication)getApplication()).getRegistrationDownload() != null);
        int title;
        if (getViewPager().getCurrentItem() < getPageCount() - 1) {
            title = R.string.next;
        } else if (showRegistration) {
            title = R.string.register;
        } else {
            title = R.string.done;
        }
        menu.findItem(R.id.action_next).setTitle(title);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_next) {
            if (getViewPager().getCurrentItem() < getPageCount() - 1) {
                getViewPager().setCurrentItem(getViewPager().getCurrentItem() + 1, true);
            } else if (showRegistration) {
                register();
            } else {
                finish();
            }
            return true;
        }
        if (item.getItemId() == R.id.action_import) {
            // If you want to be able to import face registrations from other devices create an activity
            // that scans a QR code and returns a URL string in its intent's Intent.EXTRA_TEXT extra.
            Intent intent = new Intent("com.appliedrec.ACTION_SCAN_QR_CODE");
            startActivityForResult(intent, QR_CODE_SCAN_REQUEST_CODE);
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_REGISTER && resultCode == RESULT_OK && data != null) {
            VerIDSessionResult result = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            if (result.getIdentifiedUsers().length > 0) {
                VerIDUser user = result.getIdentifiedUsers()[0];
                Intent intent = new Intent(this, RegisteredUserActivity.class);
                intent.putExtra(RegisteredUserActivity.EXTRA_USER, user);
                startActivity(intent);
                finish();
            }
        } else if (requestCode == QR_CODE_SCAN_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.hasExtra(Intent.EXTRA_TEXT)) {
            getSupportLoaderManager().restartLoader(LOADER_ID_IMPORT_REGISTRATION, data.getExtras(), this).forceLoad();
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            try {
                VerIDUser[] users = VerID.shared.getRegisteredVerIDUsers();
                if (users.length > 0) {
                    Intent intent = new Intent(this, RegisteredUserActivity.class);
                    intent.putExtra(RegisteredUserActivity.EXTRA_USER, users[0]);
                    startActivity(intent);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void register() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        VerID.LivenessDetection livenessDetection = VerID.LivenessDetection.NONE;
        if (preferences.getBoolean(getString(R.string.pref_key_enable_liveness_detection), true)) {
            livenessDetection = VerID.LivenessDetection.REGULAR;
        }
        VerIDRegistrationSessionSettings settings = new VerIDRegistrationSessionSettings(VerIDUser.DEFAULT_USER_ID, livenessDetection);
        settings.showResult = true;
        VerIDSessionIntent intent = new VerIDSessionIntent(this, settings);
        startActivityForResult(intent, REQUEST_CODE_REGISTER);
    }

    @Override
    protected int getPageCount() {
        return 3;
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        invalidateOptionsMenu();
    }

    @Override
    protected View createViewForPage(ViewGroup container, int page) {
        return IntroFragment.createView(getLayoutInflater(), container, page);
    }

    public static class IntroFragment extends Fragment {

        static int[] imageResourceIds = new int[]{
                R.mipmap.guide_head_straight,
                R.mipmap.multiple_heads,
                R.mipmap.authentication
        };
        static int[] titleResourceIds = new int[]{
                R.string.verid_person_sdk,
                R.string.one_registration,
                R.string.two_authentication
        };
        static int[] textResourceIds = new int[]{
                R.string.verid_person_sdk_text,
                R.string.one_registration_text,
                R.string.two_authentication_text
        };

        static View createView(LayoutInflater inflater, ViewGroup container, int index) {
            View view = inflater.inflate(R.layout.intro_page_fragment, container, false);
            ((ImageView)view.findViewById(R.id.imageView)).setImageResource(imageResourceIds[index]);
            ((TextView)view.findViewById(R.id.title)).setText(titleResourceIds[index]);
            ((TextView)view.findViewById(R.id.text)).setText(textResourceIds[index]);
            return view;
        }

        public static IntroFragment newInstance(int index) {
            Bundle args = new Bundle();
            args.putInt("index", index);
            IntroFragment fragment = new IntroFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            int index = args.getInt("index", 0);
            return IntroFragment.createView(inflater, container, index);
        }
    }

    //region Registration import

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID_IMPORT_REGISTRATION && args != null && args.containsKey(Intent.EXTRA_TEXT)) {
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
        if (loader.getId() == LOADER_ID_IMPORT_REGISTRATION) {
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
                        Intent intent = new Intent(IntroActivity.this, RegistrationImportActivity.class);
                        intent.putExtras(extras);
                        startActivityForResult(intent, REQUEST_CODE_IMPORT);
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

    private void showImportError() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.failed_to_import_registration)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }
    //endregion
}
