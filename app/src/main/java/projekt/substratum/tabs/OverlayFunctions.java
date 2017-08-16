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

package projekt.substratum.tabs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.view.WindowManager;

import org.omnirom.substratum.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;

// TODO: neko bro convert to Service pl0x
class OverlayFunctions {

    static final String TAG = Overlays.TAG;

    static class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;
        private WeakReference<Context> refContext;
        private WeakReference<Activity> refActivity;

        Phase2_InitializeCache(Overlays fragment, Context context, Activity activity) {
            ref = new WeakReference<>(fragment);
            refActivity = new WeakReference<Activity>(activity);
            refContext = new WeakReference<Context>(context);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            fragment.late_install = new ArrayList<>();

            fragment.mProgressDialog = null;
            fragment.mProgressDialog = new ProgressDialog(context);
            fragment.mProgressDialog.setIndeterminate(true);
            fragment.mProgressDialog.setCancelable(false);
            fragment.mProgressDialog.show();
            if (fragment.mProgressDialog.getWindow() != null) {
                fragment.mProgressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            fragment.mProgressDialog.setMessage(context.getResources().getString(
                    R.string.sb_phase_1_loader));
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Overlays fragment = ref.get();
            Context context = refContext.get();
            Activity activity = refActivity.get();
            fragment.phase3_mainFunction = new Phase3_mainFunction(fragment, context, activity);
            if (result != null) {
                fragment.phase3_mainFunction.execute(result);
            } else {
                fragment.phase3_mainFunction.execute("");
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            try {
                Resources themeResources = context.getPackageManager()
                        .getResourcesForApplication(fragment.theme_pid);
                fragment.themeAssetManager = themeResources.getAssets();
            } catch (PackageManager.NameNotFoundException e) {
                // Suppress exception
            }
            Log.d(Overlays.TAG, "Work area is ready to be compiled!");

            if (sUrl[0].length() != 0) {
                return sUrl[0];
            }
            return null;
        }
    }

    static class Phase3_mainFunction extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;
        private WeakReference<Context> refContext;
        private WeakReference<Activity> refActivity;

        Phase3_mainFunction(Overlays fragment, Context context, Activity activity) {
            ref = new WeakReference<>(fragment);
            refActivity = new WeakReference<Activity>(activity);
            refContext = new WeakReference<Context>(context);
        }

        @Override
        protected void onPreExecute() {
            Log.d(Overlays.TAG,
                    "Substratum is proceeding with your actions and is now actively running...");
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }

            fragment.missingType3 = false;
            fragment.has_failed = false;
            fragment.fail_count = 0;
            fragment.error_logs = new StringBuilder();

            fragment.mProgressDialog.setMessage(context.getResources().getString(
                    R.string.sb_phase_2_loader));
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Overlays fragment = ref.get();
            fragment.mProgressDialog.setMessage(fragment.current_dialog_overlay);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(String result) {
            // TODO: onPostExecute runs on UI thread, so move the hard job to doInBackground
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            if (fragment.has_failed) {
                fragment.failedFunction(context);
            }

            fragment.is_active = false;
            fragment.mAdapter.notifyDataSetChanged();
            fragment.mProgressDialog.dismiss();
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            String parsedVariant = sUrl[0].replaceAll("\\s+", "");
            String unparsedVariant = sUrl[0];
            fragment.failed_packages = new StringBuilder();

            fragment.total_amount = fragment.checkedOverlays.size();
            for (int i = 0; i < fragment.checkedOverlays.size(); i++) {
                fragment.type1a = "";
                fragment.type1b = "";
                fragment.type1c = "";
                fragment.type2 = "";
                fragment.type3 = "";

                OverlaysItem overlayItem = fragment.checkedOverlays.get(i);
                fragment.current_amount = i + 1;
                String theme_name_parsed =
                        fragment.theme_name.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                String current_overlay = overlayItem.getPackageName();
                fragment.current_dialog_overlay =
                        "'" + References.grabPackageName(context, current_overlay) + "'";

                fragment.setType1Value(current_overlay, "type1a_value", overlayItem.getSelectedVariantName());
                fragment.setType1Value(current_overlay, "type1b_value", overlayItem.getSelectedVariantName2());
                fragment.setType1Value(current_overlay, "type1c_value", overlayItem.getSelectedVariantName3());
                fragment.setType2Value(current_overlay, "type2_value", overlayItem.getSelectedVariantName4());

                publishProgress((int) fragment.current_amount);

                try {
                    String packageTitle = "";
                    if (References.allowedSystemUIOverlay(current_overlay)) {
                        switch (current_overlay) {
                            case "com.android.systemui.headers":
                                packageTitle = context.getString(R.string.systemui_headers);
                                break;
                            case "com.android.systemui.navbars":
                                packageTitle = context.getString(R.string.systemui_navigation);
                                break;
                            case "com.android.systemui.statusbars":
                                packageTitle = context.getString(R.string.systemui_statusbar);
                                break;
                            case "com.android.systemui.tiles":
                                packageTitle = context.getString(R.string.systemui_qs_tiles);
                                break;
                        }
                    } else if (References.allowedSettingsOverlay(current_overlay)) {
                        switch (current_overlay) {
                            case "com.android.settings.icons":
                                packageTitle = context.getString(R.string.settings_icons);
                                break;
                        }
                    } else {
                        ApplicationInfo applicationInfo =
                                context.getPackageManager()
                                        .getApplicationInfo(current_overlay, 0);
                        packageTitle = context.getPackageManager()
                                .getApplicationLabel(applicationInfo).toString();
                    }
                    
                    String workingDirectory = context.getCacheDir().getAbsolutePath() +
                            References.SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid +
                            "/assets/overlays/" + current_overlay;

                    if (!References.checkOMS(context)) {
                        File check_legacy = new File(context.getCacheDir()
                                .getAbsolutePath() + References.SUBSTRATUM_BUILDER_CACHE +
                                fragment.theme_pid + "/assets/overlays_legacy/" +
                                current_overlay);
                        if (check_legacy.exists()) {
                            workingDirectory = check_legacy.getAbsolutePath();
                        }
                    }
                    String suffix = ((sUrl[0].length() != 0) ?
                            "/type3_" + parsedVariant : "/res");
                    String unparsedSuffix =
                            ((sUrl[0].length() != 0) ? "/type3_" + unparsedVariant : "/res");
                    fragment.type3 = parsedVariant;

                    workingDirectory = context.getCacheDir().getAbsolutePath() +
                            References.SUBSTRATUM_BUILDER_CACHE.substring(0,
                                    References.SUBSTRATUM_BUILDER_CACHE.length() - 1);
                    File created = new File(workingDirectory);
                    if (created.exists()) {
                        FileOperations.delete(context, created.getAbsolutePath());
                        FileOperations.createNewFolder(context, created
                                .getAbsolutePath());
                    } else {
                        FileOperations.createNewFolder(context, created
                                .getAbsolutePath());
                    }
                    String listDir = Overlays.overlaysDir + "/" + current_overlay +
                            unparsedSuffix;

                    FileOperations.copyFileOrDir(
                            fragment.themeAssetManager,
                            listDir,
                            workingDirectory + suffix,
                            listDir,
                            fragment.cipher
                    );

                    if (overlayItem.is_variant_chosen ||
                            sUrl[0].length() != 0) {
                        // Type 1a
                        if (overlayItem.is_variant_chosen1) {
                            fragment.type1a = overlayItem.getSelectedVariantName();
                            Log.d(Overlays.TAG, "You have selected variant file \"" +
                                    overlayItem.getSelectedVariantName()
                                    + "\"");
                            Log.d(Overlays.TAG, "Moving variant file to: " +
                                    workingDirectory + suffix + "/values/type1a.xml");

                            String to_copy =
                                    Overlays.overlaysDir + "/" + current_overlay +
                                            "/type1a_" +
                                            overlayItem.getSelectedVariantName() +
                                            (fragment.encrypted ? ".xml.enc" : ".xml");

                            FileOperations.copyFileOrDir(
                                    fragment.themeAssetManager,
                                    to_copy,
                                    workingDirectory + suffix + (
                                            fragment.encrypted ?
                                                    "/values/type1a.xml.enc" :
                                                    "/values/type1a.xml"),
                                    to_copy,
                                    fragment.cipher);
                        }

                        // Type 1b
                        if (overlayItem.is_variant_chosen2) {
                            fragment.type1b = overlayItem.getSelectedVariantName2();
                            Log.d(Overlays.TAG, "You have selected variant file \"" +
                                    overlayItem
                                            .getSelectedVariantName2() + "\"");
                            Log.d(Overlays.TAG, "Moving variant file to: " +
                                    workingDirectory + suffix + "/values/type1b.xml");

                            String to_copy =
                                    Overlays.overlaysDir + "/" + current_overlay +
                                            "/type1b_" +
                                            overlayItem
                                                    .getSelectedVariantName2() +
                                            (fragment.encrypted ? ".xml.enc" : ".xml");

                            FileOperations.copyFileOrDir(
                                    fragment.themeAssetManager,
                                    to_copy,
                                    workingDirectory + suffix + (
                                            fragment.encrypted ?
                                                    "/values/type1b.xml.enc" :
                                                    "/values/type1b.xml"),
                                    to_copy,
                                    fragment.cipher);
                        }
                        // Type 1c
                        if (overlayItem.is_variant_chosen3) {
                            fragment.type1c = overlayItem.getSelectedVariantName3();
                            Log.d(Overlays.TAG, "You have selected variant file \"" +
                                    overlayItem
                                            .getSelectedVariantName3() + "\"");
                            Log.d(Overlays.TAG, "Moving variant file to: " +
                                    workingDirectory + suffix + "/values/type1c.xml");

                            String to_copy =
                                    Overlays.overlaysDir + "/" + current_overlay +
                                            "/type1c_" +
                                            overlayItem
                                                    .getSelectedVariantName3() +
                                            (fragment.encrypted ? ".xml.enc" : ".xml");

                            FileOperations.copyFileOrDir(
                                    fragment.themeAssetManager,
                                    to_copy,
                                    workingDirectory + suffix + (
                                            fragment.encrypted ?
                                                    "/values/type1c.xml.enc" :
                                                    "/values/type1c.xml"),
                                    to_copy,
                                    fragment.cipher);
                        }

                        String packageName =
                                (overlayItem.is_variant_chosen1 ?
                                        overlayItem
                                                .getSelectedVariantName() : "") +
                                        (overlayItem.is_variant_chosen2 ?
                                                overlayItem
                                                        .getSelectedVariantName2() : "") +
                                        (overlayItem.is_variant_chosen3 ?
                                                overlayItem
                                                        .getSelectedVariantName3() : "").
                                                replaceAll("\\s+", "").replaceAll
                                                ("[^a-zA-Z0-9]+", "");

                        if (overlayItem.is_variant_chosen4) {
                            packageName = (packageName + overlayItem
                                    .getSelectedVariantName4()).replaceAll("\\s+", "")
                                    .replaceAll("[^a-zA-Z0-9]+", "");
                            fragment.type2 = overlayItem.getSelectedVariantName4();
                            String type2folder = "/type2_" + fragment.type2;
                            String to_copy = Overlays.overlaysDir + "/" + current_overlay +
                                    type2folder;
                            FileOperations.copyFileOrDir(
                                    fragment.themeAssetManager,
                                    to_copy,
                                    workingDirectory + type2folder,
                                    to_copy,
                                    fragment.cipher);
                            Log.d(Overlays.TAG, "Currently processing package" +
                                    " \"" + overlayItem
                                    .getFullOverlayParameters() + "\"...");

                            if (sUrl[0].length() != 0) {
                                fragment.sb = new SubstratumBuilder();
                                fragment.sb.beginAction(
                                        context,
                                        fragment.theme_pid,
                                        current_overlay,
                                        fragment.theme_name,
                                        packageName,
                                        overlayItem
                                                .getSelectedVariantName4(),
                                        sUrl[0],
                                        fragment.versionName,
                                        References.checkOMS(context),
                                        fragment.theme_pid,
                                        suffix,
                                        fragment.type1a,
                                        fragment.type1b,
                                        fragment.type1c,
                                        fragment.type2,
                                        fragment.type3,
                                        null);
                                fragment.logTypes();
                            } else {
                                fragment.sb = new SubstratumBuilder();
                                fragment.sb.beginAction(
                                        context,
                                        fragment.theme_pid,
                                        current_overlay,
                                        fragment.theme_name,
                                        packageName,
                                        overlayItem
                                                .getSelectedVariantName4(),
                                        null,
                                        fragment.versionName,
                                        References.checkOMS(context),
                                        fragment.theme_pid,
                                        suffix,
                                        fragment.type1a,
                                        fragment.type1b,
                                        fragment.type1c,
                                        fragment.type2,
                                        fragment.type3,
                                        null);
                                fragment.logTypes();
                            }
                        } else {
                            Log.d(Overlays.TAG, "Currently processing package" +
                                    " \"" + overlayItem
                                    .getFullOverlayParameters() + "\"...");

                            if (sUrl[0].length() != 0) {
                                fragment.sb = new SubstratumBuilder();
                                fragment.sb.beginAction(
                                        context,
                                        fragment.theme_pid,
                                        current_overlay,
                                        fragment.theme_name,
                                        packageName,
                                        null,
                                        sUrl[0],
                                        fragment.versionName,
                                        References.checkOMS(context),
                                        fragment.theme_pid,
                                        suffix,
                                        fragment.type1a,
                                        fragment.type1b,
                                        fragment.type1c,
                                        fragment.type2,
                                        fragment.type3,
                                        null);
                                fragment.logTypes();
                            } else {
                                fragment.sb = new SubstratumBuilder();
                                fragment.sb.beginAction(
                                        context,
                                        fragment.theme_pid,
                                        current_overlay,
                                        fragment.theme_name,
                                        packageName,
                                        null,
                                        null,
                                        fragment.versionName,
                                        References.checkOMS(context),
                                        fragment.theme_pid,
                                        suffix,
                                        fragment.type1a,
                                        fragment.type1b,
                                        fragment.type1c,
                                        fragment.type2,
                                        fragment.type3,
                                        null);
                                fragment.logTypes();
                            }
                        }
                        if (fragment.sb.has_errored_out) {
                            if (!fragment.sb.getErrorLogs().contains("type3") ||
                                    !fragment.sb.getErrorLogs().contains("does not exist")) {
                                fragment.fail_count += 1;
                                if (fragment.error_logs.length() == 0) {
                                    fragment.error_logs.append(fragment.sb.getErrorLogs());
                                } else {
                                    fragment.error_logs.append("\n")
                                            .append(fragment.sb.getErrorLogs());
                                }
                                fragment.failed_packages.append(current_overlay);
                                fragment.failed_packages.append(" (");
                                fragment.failed_packages.append(
                                        References.grabAppVersion(context, current_overlay));
                                fragment.failed_packages.append(")\n");
                                fragment.has_failed = true;
                            } else {
                                fragment.missingType3 = true;
                            }
                        } else {
                            if (fragment.sb.special_snowflake ||
                                    fragment.sb.no_install.length() > 0) {
                                fragment.late_install.add(fragment.sb.no_install);
                            }
                        }
                    } else {
                        Log.d(Overlays.TAG, "Currently processing package" +
                                " \"" + current_overlay + "." + theme_name_parsed + "\"...");
                        fragment.sb = new SubstratumBuilder();
                        fragment.sb.beginAction(
                                context,
                                fragment.theme_pid,
                                current_overlay,
                                fragment.theme_name,
                                null,
                                null,
                                null,
                                fragment.versionName,
                                References.checkOMS(context),
                                fragment.theme_pid,
                                suffix,
                                fragment.type1a,
                                fragment.type1b,
                                fragment.type1c,
                                fragment.type2,
                                fragment.type3,
                                null);
                        fragment.logTypes();

                        if (fragment.sb.has_errored_out) {
                            fragment.fail_count += 1;
                            if (fragment.error_logs.length() == 0) {
                                fragment.error_logs.append(fragment.sb.getErrorLogs());
                            } else {
                                fragment.error_logs.append("\n")
                                        .append(fragment.sb.getErrorLogs());
                            }
                            fragment.failed_packages.append(current_overlay);
                            fragment.failed_packages.append(" (");
                            fragment.failed_packages.append(
                                    References.grabAppVersion(context, current_overlay));
                            fragment.failed_packages.append(")\n");
                            fragment.has_failed = true;
                        } else {
                            if (fragment.sb.special_snowflake ||
                                    fragment.sb.no_install.length() > 0) {
                                fragment.late_install.add(fragment.sb.no_install);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(Overlays.TAG, "Main function has unexpectedly stopped!");
                }
            }
            if (!fragment.late_install.isEmpty()) {
                // Install remaining overlays
                ThemeManager.installOverlay(context, fragment.late_install);
            }

            return null;
        }
    }
}
