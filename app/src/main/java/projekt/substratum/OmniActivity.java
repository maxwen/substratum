/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.omnirom.substratum.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.Overlays;
import projekt.substratum.util.injectors.AOPTCheck;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static android.util.Base64.DEFAULT;
import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.metadataOverlayParent;

public class OmniActivity extends SubstratumActivity {

    private SharedPreferences prefs;
    private ProgressDialog mProgressDialog;
    private LocalBroadcastManager localBroadcastManager;
    private boolean mStoragePerms;
    private FloatingActionButton floatingActionButton;
    private String theme_pid;
    private String theme_name;
    private byte[] encryption_key;
    private byte[] iv_encrypt_key;

    private class FabDialog extends Dialog implements View.OnClickListener {
        private TextView compile_selected;
        private TextView disable_selected;
        private TextView enable_selected;
        boolean enablePossible;
        boolean disablePossible;
        boolean compilePossible;

        protected FabDialog(@NonNull Context context, boolean compilePossible, boolean enablePossible, boolean disablePossible) {
            super(context);
            this.enablePossible = enablePossible;
            this.disablePossible = disablePossible;
            this.compilePossible = compilePossible;
        }

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.sheet_dialog);

            compile_selected = (TextView) findViewById(R.id.compile_selected);
            if (!compilePossible) {
                compile_selected.setEnabled(false);
            } else {
                compile_selected.setOnClickListener(this);
            }
            disable_selected = (TextView) findViewById(R.id.disable_selected);
            if (!disablePossible) {
                disable_selected.setEnabled(false);
            } else {
                disable_selected.setOnClickListener(this);
            }
            enable_selected = (TextView) findViewById(R.id.enable_selected);
            if (!enablePossible) {
                enable_selected.setEnabled(false);
            } else {
                enable_selected.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent("Overlays.START_JOB");
            if (view == compile_selected) {
                dismiss();
                intent.putExtra("command", "CompileEnable");
                localBroadcastManager.sendBroadcast(intent);
            }
            if (view == disable_selected) {
                dismiss();
                intent.putExtra("command", "Disable");
                localBroadcastManager.sendBroadcast(intent);
            }
            if (view == enable_selected) {
                dismiss();
                intent.putExtra("command", "Enable");
                localBroadcastManager.sendBroadcast(intent);
            }
        }
    }

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private static void setOverflowButtonColor(final Activity activity, final Boolean dark_mode) {
        @SuppressLint("PrivateResource") final String overflowDescription =
                activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            final ArrayList<View> outViews = new ArrayList<>();
            decorView.findViewsWithText(
                    outViews,
                    overflowDescription,
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
            if (outViews.isEmpty()) {
                return;
            }
            AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
            overflow.setImageResource(dark_mode ? R.drawable.information_activity_overflow_dark :
                    R.drawable.information_activity_overflow_light);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");
        encryption_key = currentIntent.getByteArrayExtra("encryption_key");
        iv_encrypt_key = currentIntent.getByteArrayExtra("iv_encrypt_key");

        if (theme_pid == null) {
            theme_name = prefs.getString("theme_name", null);
            theme_pid = prefs.getString("theme_pid", null);
            String s = prefs.getString("encryption_key", null);
            if (s != null) {
                encryption_key = Base64.decode(s, DEFAULT);
            }
            s = prefs.getString("iv_encrypt_key", null);
            if (s != null) {
                iv_encrypt_key = Base64.decode(s, DEFAULT);
            }
        } else {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("theme_name");
            editor.remove("theme_pid");
            editor.remove("encryption_key");
            editor.remove("iv_encrypt_key");
            editor.commit();
        }
        if (theme_pid == null) {
            finish();
        }

        setContentView(R.layout.information_activity);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(theme_name);
            toolbar.setTitleTextColor(getColor(R.color.information_activity_light_icon_mode));
            toolbar.setPopupTheme(R.style.ActionBarPopupTheme);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setElevation(8.0f);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> onBackPressed());

        floatingActionButton = (FloatingActionButton) findViewById(R.id.apply_fab);

        Drawable upArrow = getResources().getDrawable(R.drawable.information_activity_back_light);
        if (upArrow != null)
            upArrow.setColorFilter(getResources().getColor(R.color.information_activity_light_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        setOverflowButtonColor(this, false);

        floatingActionButton.setOnClickListener(v -> {
            try {
                Overlays fragment = (Overlays) getSupportFragmentManager().findFragmentById(R.id.overlays);
                if (fragment != null) {
                    boolean disablePossible = fragment.getDisableOverlays().size() != 0;
                    boolean enablePossible = fragment.canEnableOverlays();
                    boolean compilePossible = fragment.canCompileOverlays();
                    final FabDialog sheetDialog = new FabDialog(this, compilePossible, enablePossible, disablePossible);
                    sheetDialog.show();
                }
            } catch (NullPointerException npe) {
                // Suppress warning
            }
        });

        requestStoragePermissions();
        new AOPTCheck().injectAOPT(this, false);

        Overlays fragment = (Overlays) getSupportFragmentManager().findFragmentById(R.id.overlays);
        if (fragment != null && !fragment.init(theme_name, theme_pid, encryption_key, iv_encrypt_key)) {
            finish();
        }

        checkIfOverlaysAreFromOmni();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);

        menu.findItem(R.id.disable).setVisible(false);
        menu.findItem(R.id.enable).setVisible(false);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isOMS = References.checkOMS(getApplicationContext());
        if (isOMS) {
            //menu.findItem(R.id.disable).setVisible(mStoragePerms);
            //menu.findItem(R.id.enable).setVisible(mStoragePerms);
            menu.findItem(R.id.clean).setVisible(mStoragePerms);
        }
        boolean enabled = prefs.getBoolean("enable_restart_systemui", true);
        menu.findItem(R.id.auto_restart_systemui).setChecked(enabled);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.clean:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(OmniActivity.this);
                builder1.setTitle(theme_name);
                builder1.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder1.setMessage(R.string.clean_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id18) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all installed overlays
                            List<String> stateAll = ThemeManager.listAllOverlays(
                                    getApplicationContext());

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(theme_pid)) {
                                            // we want ourself at the end to see nice progress
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            // Begin uninstalling overlays for this package
                            new cleanTheme().execute(all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id19) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder1.create();
                builder1.show();
                return true;
            case R.id.disable:
                AlertDialog.Builder builder3 = new AlertDialog.Builder(OmniActivity.this);
                builder3.setTitle(theme_name);
                builder3.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder3.setMessage(R.string.disable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id16) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all enabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    getApplicationContext(), STATE_APPROVED_ENABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            // Begin disabling overlays
                            new disableInstalledTheme().execute(all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id15) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder3.create();
                builder3.show();
                return true;
            case R.id.enable:
                AlertDialog.Builder builder4 = new AlertDialog.Builder(OmniActivity.this);
                builder4.setTitle(theme_name);
                builder4.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder4.setMessage(R.string.enable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id14) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all disabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    getApplicationContext(), STATE_APPROVED_DISABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            // Begin enabling overlays
                            new enableInstalledTheme().execute(all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id13) ->
                                dialog
                                        .cancel());
                // Create the AlertDialog object and return it
                builder4.create();
                builder4.show();
                return true;
            case R.id.restart_systemui:
                ThemeManager.restartSystemUI(getApplicationContext());
                return true;
            case R.id.about_substratum:
                startActivity(new Intent(this, TeamActivity.class));
                return true;
            case R.id.auto_restart_systemui:
                boolean enabled = prefs.getBoolean("enable_restart_systemui", true);
                prefs.edit().putBoolean("enable_restart_systemui", !enabled).commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            String workingDirectory =
                    getApplicationContext().getCacheDir().getAbsolutePath();
            File deleted = new File(workingDirectory);
            FileOperations.delete(getApplicationContext(), deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared Substratum cache!");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // save state
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("theme_name", theme_name);
        editor.putString("theme_pid", theme_pid);
        if (encryption_key != null) {
            editor.putString("encryption_key", Base64.encodeToString(encryption_key, DEFAULT));
        }
        if (iv_encrypt_key != null) {
            editor.putString("iv_encrypt_key", Base64.encodeToString(iv_encrypt_key, DEFAULT));
        }
        editor.commit();
    }

    private class enableInstalledTheme extends AsyncTask<ArrayList<String>, Void, Void> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(OmniActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.overlay_enable_progress));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                mProgressDialog.cancel();
            } catch (Exception e) {
            }
        }

        @Override
        protected Void doInBackground(ArrayList<String>... overlays) {
            ThemeManager.enableOverlay(getApplicationContext(), overlays[0]);
            return null;
        }
    }

    private class disableInstalledTheme extends AsyncTask<ArrayList<String>, Void, Void> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(OmniActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.overlay_disable_progress));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                mProgressDialog.cancel();
            } catch (Exception e) {
            }
        }

        @Override
        protected Void doInBackground(ArrayList<String>... overlays) {
            ThemeManager.disableOverlay(getApplicationContext(), overlays[0]);
            return null;
        }
    }

    private class cleanTheme extends AsyncTask<ArrayList<String>, Void, Void> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(OmniActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.overlay_clean_progress));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                mProgressDialog.cancel();
            } catch (Exception e) {
            }
        }

        @Override
        protected Void doInBackground(ArrayList<String>... overlays) {
            ThemeManager.uninstallOverlay(
                    getApplicationContext(),
                    overlays[0]);
            return null;
        }
    }

    private boolean checkPermissionGrantResults(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (checkPermissionGrantResults(grantResults)) {
                    mStoragePerms = true;
                    floatingActionButton.show();
                }
            }
        }
    }

    private void requestStoragePermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            mStoragePerms = false;
            floatingActionButton.hide();
            return;
        }
        mStoragePerms = true;
        floatingActionButton.show();
    }

    private boolean checkIfOverlaysAreFromOmni() {
        List<String> overlays = ThemeManager.listAllOverlays(getApplicationContext());
        for (int i = 0; i < overlays.size(); i++) {
            int current_version = References.grabOverlayOmniVersion(
                    getApplicationContext(),
                    overlays.get(i));
            if (current_version == 0) {
                Log.d("OverlayOmniCheck",
                        "Overlay is not built with OmniSubs " + overlays.get(i));
                return true;
            } else {
                Log.d("OverlayOmniCheck",
                        "Overlay is built with OmniSubs " + overlays.get(i));
            }
        }
        return false;
    }
}
