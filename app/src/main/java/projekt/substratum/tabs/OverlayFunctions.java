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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.omnirom.substratum.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import projekt.substratum.OmniActivity;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.CacheCreator;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.FileUtils;

import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;

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
            fragment.final_runner = new ArrayList<>();
            fragment.late_install = new ArrayList<>();

            if (!fragment.enable_mode && !fragment.disable_mode) {
                int notification_priority = Notification.PRIORITY_MAX;

                PendingIntent resultPendingIntent = PendingIntent.getActivity(
                        context, 0, new Intent(), 0);

                // This is the time when the notification should be shown on the user's screen
                fragment.mNotifyManager =
                        (NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                fragment.mBuilder = new NotificationCompat.Builder(context);
                fragment.mBuilder.setContentTitle(
                        context.getString(R.string.notification_initial_title))
                        .setProgress(100, 0, true)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setContentIntent(resultPendingIntent)
                        .setOngoing(true);
                fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());

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
            }
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
            if (!fragment.enable_mode && !fragment.disable_mode) {
                // Initialize Substratum cache with theme only if permitted
                if (References.isCachingEnabled(context) && !fragment.has_initialized_cache) {
                    Log.d(Overlays.TAG,
                            "Decompiling and initializing work area with the " +
                                    "selected theme's assets...");
                    fragment.sb = new SubstratumBuilder();

                    File versioning = new File(context.getCacheDir().getAbsoluteFile() +
                            References.SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid +
                            "/substratum.xml");
                    if (versioning.exists()) {
                        fragment.has_initialized_cache = true;
                    } else {
                        new CacheCreator().initializeCache(context, fragment.theme_pid,
                                fragment.cipher);
                        fragment.has_initialized_cache = true;
                    }
                } else {
                    try {
                        Resources themeResources = context.getPackageManager()
                                .getResourcesForApplication(fragment.theme_pid);
                        fragment.themeAssetManager = themeResources.getAssets();
                    } catch (PackageManager.NameNotFoundException e) {
                        // Suppress exception
                    }
                    Log.d(Overlays.TAG, "Work area is ready to be compiled!");
                }
                if (sUrl[0].length() != 0) {
                    return sUrl[0];
                } else {
                    return null;
                }
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

            if (!fragment.enable_mode && !fragment.disable_mode) {
                // Change title in preparation for loop to change subtext
                if (fragment.checkActiveNotifications()) {
                    fragment.mBuilder
                            .setContentTitle(context.getString(R.string.notification_processing_n))
                            .setProgress(100, 0, false);
                    fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());
                }
                fragment.mProgressDialog.setMessage(context.getResources().getString(
                        R.string.sb_phase_2_loader));
            }
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
            fragment.final_command = new ArrayList<>();

            // Check if not compile_enable_mode
            if (!fragment.compile_enable_mode) {
                fragment.final_command.addAll(fragment.final_runner);
            } else {
                // It's compile and enable mode, we have to first sort out all the "pm install"'s
                // from the final_commands
                fragment.final_command.addAll(fragment.final_runner);
            }

            if (!fragment.enable_mode && !fragment.disable_mode) {
                new Phase4_finishUpdateFunction(fragment, context, refActivity.get()).execute();
                if (fragment.has_failed) {
                    fragment.failedFunction(context);
                } else {
                    // Restart SystemUI if an enabled SystemUI overlay is updated
                    if (checkOMS(context)) {
                        for (int i = 0; i < fragment.checkedOverlays.size(); i++) {
                            String targetOverlay = fragment.checkedOverlays.get(i).getPackageName();
                            if (targetOverlay.equals("com.android.systemui")) {
                                String packageName =
                                        fragment.checkedOverlays.get(i).getFullOverlayParameters();
                                if (ThemeManager.isOverlayEnabled(context, packageName)) {
                                    ThemeManager.restartSystemUI(context);
                                    break;
                                }
                            }
                        }
                    }
                }
                try {
                    context.unregisterReceiver(fragment.finishReceiver);
                } catch (IllegalArgumentException e) {
                    // Suppress warning
                }
            } else if (fragment.enable_mode) {
                new Phase4_finishEnableFunction(fragment, context, refActivity.get()).execute();
            } else if (fragment.disable_mode) {
                new Phase4_finishDisableFunction(fragment, context, refActivity.get()).execute();
            }
            if (!References.checkOMS(context) &&
                    fragment.final_runner.size() == fragment.fail_count) {
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(context);
                alertDialogBuilder
                        .setTitle(context.getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(context.getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok,
                                (dialog, id12) -> ElevatedCommands.reboot());
                alertDialogBuilder
                        .setNegativeButton(R.string.remove_dialog_later,
                                (dialog, id1) -> {
                                    dialog.dismiss();
                                });
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            fragment.is_active = false;
            fragment.mAdapter.notifyDataSetChanged();
            if (fragment.toggle_all.isChecked()) fragment.toggle_all.setChecked(false);
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

            if (fragment.mixAndMatchMode && !References.checkOMS(context)) {
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = References.PIXEL_NEXUS_DIR;
                } else {
                    current_directory = References.LEGACY_NEXUS_DIR;
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    FileOperations.mountRW();
                    FileOperations.delete(context, current_directory);
                }
            }

            // Enable listener
            if (References.checkThemeInterfacer(context) &&
                    !References.isBinderInterfacer(context)) {
                if (fragment.finishReceiver == null) {
                    fragment.finishReceiver = new Overlays.FinishReceiver(fragment);
                }
                IntentFilter filter = new IntentFilter(References.STATUS_CHANGED);
                context.registerReceiver(fragment.finishReceiver, filter);
            }

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

                if (!fragment.enable_mode && !fragment.disable_mode) {
                    publishProgress((int) fragment.current_amount);
                    if (fragment.compile_enable_mode) {
                        if (fragment.final_runner == null) {
                            fragment.final_runner = new ArrayList<>();
                        }
                        String package_name = overlayItem.getFullOverlayParameters();
                        if (References.isPackageInstalled(context, package_name) ||
                                fragment.compile_enable_mode) {
                            fragment.final_runner.add(package_name);
                        }
                    }
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

                        // Initialize working notification
                        if (fragment.checkActiveNotifications()) {
                            fragment.mBuilder.setProgress(100, (int) (((double) (i + 1) /
                                    fragment.checkedOverlays.size()) * 100), false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                fragment.mBuilder.setContentText("\"" + packageTitle + "\"");
                            } else {
                                fragment.mBuilder.setContentText(
                                        context.getString(R.string.notification_processing) +
                                                "\"" + packageTitle + "\"");
                            }
                            fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());
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
                        if (References.isCachingEnabled(context)) {
                            File srcDir = new File(workingDirectory +
                                    ((sUrl[0].length() != 0) ? "/type3_" + sUrl[0] : "/res"));
                            File destDir = new File(workingDirectory + "/workdir");
                            if (destDir.exists()) {
                                FileOperations.delete(context, destDir.getAbsolutePath());
                            }
                            FileUtils.copyDirectory(srcDir, destDir);
                        } else {
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
                        }

                        if (overlayItem.is_variant_chosen ||
                                sUrl[0].length() != 0) {
                            // Type 1a
                            if (overlayItem.is_variant_chosen1) {
                                fragment.type1a = overlayItem.getSelectedVariantName();
                                if (References.isCachingEnabled(context)) {
                                    String sourceLocation = workingDirectory + "/type1a_" +
                                            overlayItem.getSelectedVariantName()
                                            + ".xml";

                                    String targetLocation = workingDirectory +
                                            "/workdir/values/type1a.xml";

                                    Log.d(Overlays.TAG,
                                            "You have selected variant file \"" +
                                                    overlayItem.getSelectedVariantName() + "\"");
                                    Log.d(Overlays.TAG, "Moving variant file to: " +
                                            targetLocation);
                                    FileOperations.copy(
                                            context,
                                            sourceLocation,
                                            targetLocation);
                                } else {
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
                            }

                            // Type 1b
                            if (overlayItem.is_variant_chosen2) {
                                fragment.type1b = overlayItem.getSelectedVariantName2();
                                if (References.isCachingEnabled(context)) {
                                    String sourceLocation2 = workingDirectory + "/type1b_" +
                                            overlayItem
                                                    .getSelectedVariantName2() + ".xml";

                                    String targetLocation2 = workingDirectory +
                                            "/workdir/values/type1b.xml";

                                    Log.d(Overlays.TAG, "You have selected variant file \"" +
                                            overlayItem
                                                    .getSelectedVariantName2() + "\"");
                                    Log.d(Overlays.TAG, "Moving variant file to: " +
                                            targetLocation2);
                                    FileOperations.copy(context, sourceLocation2,
                                            targetLocation2);
                                } else {
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
                            }
                            // Type 1c
                            if (overlayItem.is_variant_chosen3) {
                                fragment.type1c = overlayItem.getSelectedVariantName3();
                                if (References.isCachingEnabled(context)) {
                                    String sourceLocation3 = workingDirectory + "/type1c_" +
                                            overlayItem
                                                    .getSelectedVariantName3() + ".xml";

                                    String targetLocation3 = workingDirectory +
                                            "/workdir/values/type1c.xml";

                                    Log.d(Overlays.TAG, "You have selected variant file \"" +
                                            overlayItem
                                                    .getSelectedVariantName3() + "\"");
                                    Log.d(Overlays.TAG, "Moving variant file to: " +
                                            targetLocation3);

                                    FileOperations.copy(
                                            context,
                                            sourceLocation3,
                                            targetLocation3);
                                } else {
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
                                } else if (References.checkThemeInterfacer(context) &&
                                        !References.isBinderInterfacer(context)) {
                                    // Thread wait
                                    fragment.isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(Overlays.THREAD_WAIT_DURATION);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (fragment.isWaiting);
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
                                } else if (References.checkThemeInterfacer(context) &&
                                        !References.isBinderInterfacer(context)) {
                                    // Thread wait
                                    fragment.isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(Overlays.THREAD_WAIT_DURATION);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (fragment.isWaiting);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(Overlays.TAG, "Main function has unexpectedly stopped!");
                    }
                } else {
                    if (fragment.final_runner == null)
                        fragment.final_runner = new ArrayList<>();
                    if (fragment.enable_mode || fragment.compile_enable_mode ||
                            fragment.disable_mode) {
                        String package_name =
                                overlayItem.getFullOverlayParameters();
                        if (References.isPackageInstalled(context, package_name)) {
                            fragment.final_runner.add(package_name);
                        }
                    }
                }
            }
            return null;
        }
    }

    static class Phase4_finishEnableFunction extends AsyncTask<Void, Void, Void> {
        private WeakReference<Overlays> ref;
        private WeakReference<Context> refContext;
        private WeakReference<Activity> refActivity;

        Phase4_finishEnableFunction(Overlays overlays, Context context, Activity activity) {
            ref = new WeakReference<>(overlays);
            refActivity = new WeakReference<Activity>(activity);
            refContext = new WeakReference<Context>(context);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();

            if (fragment.toggle_all.isChecked()) fragment.toggle_all.setChecked(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }

            if (fragment.final_runner.size() > 0) {
                fragment.enable_mode = false;

                if (fragment.mixAndMatchMode) {
                    // Buffer the disableBeforeEnabling String
                    ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                    for (int i = 0; i < fragment.all_installed_overlays.size(); i++) {
                        if (!References.grabOverlayParent(context,
                                fragment.all_installed_overlays.get(i))
                                .equals(fragment.theme_pid)) {
                            disableBeforeEnabling.add(fragment.all_installed_overlays.get(i));
                        }
                    }
                    ThemeManager.disableOverlay(context, disableBeforeEnabling);
                    ThemeManager.enableOverlay(context, fragment.final_command);
                } else {
                    ThemeManager.enableOverlay(context, fragment.final_command);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            if (fragment.final_runner.size() > 0) {
                if (fragment.needsRecreate(context)) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            fragment.overlaysLists = ((OverlaysAdapter) fragment.mAdapter)
                                    .getOverlayList();
                            for (int i = 0; i < fragment.overlaysLists.size(); i++) {
                                OverlaysItem currentOverlay = fragment.overlaysLists.get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(
                                        fragment.updateEnabledOverlays());
                                fragment.mAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, References.REFRESH_WINDOW_DELAY);
                }
            } else {
                fragment.compile_enable_mode = false;
                fragment.enable_mode = false;
                Toast.makeText(context,
                        R.string.toast_disabled3,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    static class Phase4_finishDisableFunction extends AsyncTask<Void, Void, Void> {
        private WeakReference<Overlays> ref;
        private WeakReference<Context> refContext;
        private WeakReference<Activity> refActivity;

        Phase4_finishDisableFunction(Overlays overlays, Context context, Activity activity) {
            ref = new WeakReference<>(overlays);
            refActivity = new WeakReference<Activity>(activity);
            refContext = new WeakReference<Context>(context);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            Activity activity = fragment.getActivity();
            if (activity == null) {
                activity = refActivity.get();
            }

            if (fragment.final_runner.size() > 0) {
                if (fragment.toggle_all.isChecked())
                    activity.runOnUiThread(() -> fragment.toggle_all.setChecked(false));
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            if (fragment.final_runner.size() > 0) {
                fragment.disable_mode = false;
                ThemeManager.disableOverlay(context, fragment.final_command);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            Activity activity = fragment.getActivity();
            if (activity == null) {
                activity = refActivity.get();
            }
            final Activity finalActivity = activity;
            if (fragment.final_runner.size() > 0) {
                if (fragment.needsRecreate(context)) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            fragment.overlaysLists = ((OverlaysAdapter) fragment.mAdapter)
                                    .getOverlayList();
                            for (int i = 0; i < fragment.overlaysLists.size(); i++) {
                                OverlaysItem currentOverlay = fragment.overlaysLists.get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(
                                        fragment.updateEnabledOverlays());
                                finalActivity.runOnUiThread(() ->
                                        fragment.mAdapter.notifyDataSetChanged());
                            }
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, References.REFRESH_WINDOW_DELAY);
                }
            } else {
                fragment.disable_mode = false;
                Toast.makeText(context,
                        R.string.toast_disabled4,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    static class Phase4_finishUpdateFunction extends AsyncTask<Void, Void, Void> {
        private WeakReference<Overlays> ref;
        private WeakReference<Context> refContext;
        private WeakReference<Activity> refActivity;

        Phase4_finishUpdateFunction(Overlays overlays, Context context, Activity activity) {
            ref = new WeakReference<>(overlays);
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

            fragment.mProgressDialog.dismiss();

            // Add dummy intent to be able to close the notification on click
            Intent notificationIntent = new Intent(context, OmniActivity.class);
            notificationIntent.putExtra("theme_name", fragment.theme_name);
            notificationIntent.putExtra("theme_pid", fragment.theme_pid);
            notificationIntent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent =
                    PendingIntent.getActivity(context, 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            if (!fragment.has_failed) {
                // Closing off the persistent notification
                if (fragment.checkActiveNotifications()) {
                    fragment.mNotifyManager.cancel(fragment.id);
                    fragment.mBuilder = new NotificationCompat.Builder(context);
                    fragment.mBuilder.setAutoCancel(true);
                    fragment.mBuilder.setProgress(0, 0, false);
                    fragment.mBuilder.setOngoing(false);
                    fragment.mBuilder.setContentIntent(intent);
                    fragment.mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                    fragment.mBuilder.setContentTitle(
                            context.getString(R.string.notification_done_title));
                    fragment.mBuilder.setContentText(
                            context.getString(R.string.notification_no_errors_found));
                    if (fragment.prefs.getBoolean("vibrate_on_compiled", false)) {
                        fragment.mBuilder.setVibrate(new long[]{100, 200, 100, 500});
                    }
                    fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());
                }

                if (fragment.missingType3) {
                    Toast.makeText(context,
                            R.string.toast_compiled_missing,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context,
                            R.string.toast_compiled_updated,
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Overlays fragment = ref.get();
            Activity activity = fragment.getActivity();
            if (activity == null) {
                activity = refActivity.get();
            }
            final Activity finalActivity = activity;
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            if (!fragment.has_failed || fragment.final_runner.size() > fragment.fail_count) {
                StringBuilder final_commands = new StringBuilder();
                if (fragment.compile_enable_mode && fragment.mixAndMatchMode) {
                    // Buffer the disableBeforeEnabling String
                    ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                    for (String p : fragment.all_installed_overlays) {
                        if (!fragment.theme_pid.equals(References.grabOverlayParent(context, p))) {
                            disableBeforeEnabling.add(p);
                        } else {
                            for (OverlaysItem oi : fragment.checkedOverlays) {
                                String targetOverlay = oi.getPackageName();
                                if (targetOverlay.equals(
                                        References.grabOverlayTarget(context, p))) {
                                    disableBeforeEnabling.add(p);
                                }
                            }
                        }
                    }
                    if (checkThemeInterfacer(context)) {
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                    } else {
                        final_commands = new StringBuilder(ThemeManager.disableOverlay);
                        for (int i = 0; i < disableBeforeEnabling.size(); i++) {
                            final_commands.append(" ").append(disableBeforeEnabling.get(i))
                                    .append(" ");
                        }
                        Log.d(TAG, final_commands.toString());
                    }
                }

                if (fragment.compile_enable_mode) {
                    ThemeManager.enableOverlay(context, fragment.final_command);
                }

                if (fragment.final_runner.size() == 0) {
                    if (fragment.base_spinner.getSelectedItemPosition() == 0) {
                        finalActivity.runOnUiThread(() -> fragment.mAdapter.notifyDataSetChanged());
                    } else {
                        finalActivity.runOnUiThread(() -> fragment.mAdapter.notifyDataSetChanged());
                    }
                } else {
                    if (fragment.toggle_all.isChecked())
                        finalActivity.runOnUiThread(() -> fragment.toggle_all.setChecked(false));
                    finalActivity.runOnUiThread(() -> fragment.mAdapter.notifyDataSetChanged());
                }

                if (fragment.needsRecreate(context)) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            fragment.overlaysLists =
                                    ((OverlaysAdapter) fragment.mAdapter).getOverlayList();
                            for (int i = 0; i < fragment.overlaysLists.size(); i++) {
                                OverlaysItem currentOverlay = fragment.overlaysLists.get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(
                                        fragment.updateEnabledOverlays());
                                finalActivity.runOnUiThread(() ->
                                        fragment.mAdapter.notifyDataSetChanged());
                            }
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, REFRESH_WINDOW_DELAY);
                }

                if (!fragment.late_install.isEmpty()) {
                    // Install remaining overlays
                    ThemeManager.installOverlay(context, fragment.late_install);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

        }
    }
}
