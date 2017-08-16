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
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.omnirom.substratum.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.Substratum;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantAdapter;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.MapUtils;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.metadataEmail;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;

public class Overlays extends Fragment {

    public static final String overlaysDir = "overlays";
    public static final String TAG = SUBSTRATUM_BUILDER;
    public static final int THREAD_WAIT_DURATION = 500;
    public ProgressDialog mProgressDialog;
    public SubstratumBuilder sb;
    public List<OverlaysItem> overlaysLists, checkedOverlays;
    public RecyclerView.Adapter mAdapter;
    public String theme_name;
    public String theme_pid;
    public String versionName;
    public boolean has_failed = false;
    public int fail_count;
    public StringBuilder failed_packages;
    public int id = References.notification_id;
    public ArrayList<OverlaysItem> values2;
    public RecyclerView mRecyclerView;
    public Spinner base_spinner;
    public SharedPreferences prefs;
    public ArrayList<String> late_install;
    public boolean compile_enable_mode;
    public ArrayList<String> all_installed_overlays;
    public Switch toggle_all;
    public SwipeRefreshLayout swipeRefreshLayout;
    public Boolean is_active = false;
    public StringBuilder error_logs;
    public ProgressBar materialProgressBar;
    public double current_amount = 0;
    public double total_amount = 0;
    public String current_dialog_overlay;
    public FinishReceiver finishReceiver;
    public boolean isWaiting;
    public AssetManager themeAssetManager;
    public Boolean missingType3 = false;
    public JobReceiver jobReceiver;
    public LocalBroadcastManager localBroadcastManager, localBroadcastManager2;
    public String type1a = "";
    public String type1b = "";
    public String type1c = "";
    public String type2 = "";
    public String type3 = "";
    public OverlayFunctions.Phase3_mainFunction phase3_mainFunction;
    public Boolean encrypted = false;
    public Cipher cipher = null;
    public ActivityManager am;
    public boolean decryptedAssetsExceptionReached;
    private Context context;
    private Activity activity;

    protected void logTypes() {
        if (ENABLE_PACKAGE_LOGGING) {
            Log.d("Theme Type1a Resource", type1a);
            Log.d("Theme Type1b Resource", type1b);
            Log.d("Theme Type1c Resource", type1c);
            Log.d("Theme Type2  Resource", type2);
            Log.d("Theme Type3  Resource", type3);
        }
    }

    public void startCompileEnableMode() {
        if (!is_active) {
            is_active = true;
            compile_enable_mode = true;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    checkedOverlays.add(currentOverlay);
                }
            }
            if (!checkedOverlays.isEmpty()) {
                OverlayFunctions.Phase2_InitializeCache phase2 = new OverlayFunctions
                        .Phase2_InitializeCache(this, context, activity);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "startCompileEnableMode: Killing package : " + overlay.getPackageName());
                    am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                is_active = false;
                Toast.makeText(getContext(),
                        R.string.toast_disabled5,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public void startDisableNew() {
        if (References.checkOMS(getContext())) {
            ArrayList<String> disableOverlays = getDisableOverlays();
            if (disableOverlays.size() != 0) {
                // Begin disabling overlays
                EnableDisableTheme task = new EnableDisableTheme(this, context, activity);
                task.execute(new Pair(disableOverlays, new ArrayList<String>()));
            }
        }
    }

    private String getCurrentEnabledAndroidOverlay() {
        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
        List<String> stateEnabled = ThemeManager.listOverlays(getContext(), STATE_APPROVED_ENABLED);
        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            if (currentOverlay.isAndroidPackage()) {
                for (int j = 0; j < stateEnabled.size(); j++) {
                    String current = stateEnabled.get(j);
                    if (current.startsWith(currentOverlay.getPackageName() + "." + currentOverlay.getThemeName())) {
                        return current;
                    }
                }
            }
        }
        return null;
    }

    private String getCurrentEnabledOverlay(OverlaysItem overlay) {
        List<String> stateEnabled = ThemeManager.listOverlays(getContext(), STATE_APPROVED_ENABLED);
        for (int j = 0; j < stateEnabled.size(); j++) {
            String current = stateEnabled.get(j);
            if (current.startsWith(overlay.getPackageName() + "." + overlay.getThemeName())) {
                return current;
            }
        }
        return null;
    }

    public void startEnableNew() {
        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
        ArrayList<String> enableOverlays = new ArrayList<>();
        ArrayList<String> disableOverlays = new ArrayList<>();

        String currentAndroidOverlay = getCurrentEnabledAndroidOverlay();
        List<String> stateEnabled = ThemeManager.listOverlays(getContext(), STATE_APPROVED_ENABLED);

        if (References.checkOMS(getContext())) {
            boolean cantEnableAndroid = false;
            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                String currentFullName = currentOverlay.getFullOverlayParameters();

                if (currentOverlay.isAndroidPackage()) {
                    if (!currentOverlay.isPackageInstalled(currentFullName)) {
                        // we must never enable anything if we dont have a valid android package
                        cantEnableAndroid = true;
                        break;
                    }
                    if (currentAndroidOverlay != null && !currentAndroidOverlay.equals(currentOverlay.getFullOverlayParameters())) {
                        // add current on to disable if we have a new one to enable
                        disableOverlays.add(currentAndroidOverlay);
                    }
                } else {
                    if (!stateEnabled.contains(currentOverlay.getFullOverlayParameters())) {
                        // add a current enabled one to disable
                        String currentEnabledOverlay = getCurrentEnabledOverlay(currentOverlay);
                        if (currentEnabledOverlay != null) {
                            disableOverlays.add(currentEnabledOverlay);
                        }
                    }
                }
                // this assumes that any other variant is no longer enabled at this stage
                if (currentOverlay.isSelected() && currentOverlay.isPackageInstalled(currentFullName) && !currentOverlay.isOverlayEnabled()) {
                    enableOverlays.add(currentFullName);

                    setType1Value(currentOverlay.getPackageName(), "type1a_value", currentOverlay.getSelectedVariantName());
                    setType1Value(currentOverlay.getPackageName(), "type1b_value", currentOverlay.getSelectedVariantName2());
                    setType1Value(currentOverlay.getPackageName(), "type1c_value", currentOverlay.getSelectedVariantName3());
                    setType2Value(currentOverlay.getPackageName(), "type2_value", currentOverlay.getSelectedVariantName4());
                }
            }

            if (!cantEnableAndroid && enableOverlays.size() != 0) {
                EnableDisableTheme task = new EnableDisableTheme(this, context, activity);
                task.execute(new Pair(disableOverlays, enableOverlays));
            }
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_overlays, container, false);
    }

    public boolean init(String themeName, String themePid, byte[] encryption_key, byte[] iv_encrypt_key) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        am = (ActivityManager) getContext().getSystemService(Activity.ACTIVITY_SERVICE);
        context = getContext();
        activity = getActivity();

        theme_name = themeName;
        theme_pid = themePid;
        String encrypt_check =
                References.getOverlayMetadata(getContext(), theme_pid, metadataEncryption);

        if (encrypt_check != null && encrypt_check.equals(metadataEncryptionValue)) {
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(
                        Cipher.DECRYPT_MODE,
                        new SecretKeySpec(encryption_key, "AES"),
                        new IvParameterSpec(iv_encrypt_key)
                );
                Log.d(TAG, "Loading substratum theme in encrypted assets mode.");
                encrypted = true;
            } catch (Exception e) {
                Log.d(TAG,
                        "Loading substratum theme in decrypted assets mode due to an exception.");
                decryptedAssetsExceptionReached = true;
            }
        } else {
            Log.d(TAG, "Loading substratum theme in decrypted assets mode.");
        }

        if (decryptedAssetsExceptionReached) {
            return false;
        }

        materialProgressBar = (ProgressBar) getView().findViewById(R.id.progress_bar_loader);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) getView().findViewById(R.id.overlayRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<OverlaysItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        TextView toggle_all_overlays_text = (TextView) getView().findViewById(R.id.toggle_all_overlays_text);
        toggle_all_overlays_text.setVisibility(View.VISIBLE);

        File work_area = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                EXTERNAL_STORAGE_CACHE);
        if (!work_area.exists() && work_area.mkdir()) {
            Log.d(TAG, "Updating the internal storage with proper file directories...");
        }

        // Adjust the behaviour of the mix and match toggle in the sheet
        toggle_all = (Switch) getView().findViewById(R.id.toggle_all_overlays);
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    setOverlaySelected(isChecked);
                });

        // Allow the user to swipe down to refresh the overlay list
        swipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                currentOverlay.setSelected(toggle_all.isChecked());
                currentOverlay.updateEnabledOverlays(updateEnabledOverlays());
                mAdapter.notifyDataSetChanged();
            }
            swipeRefreshLayout.setRefreshing(false);
        });
        swipeRefreshLayout.setVisibility(View.GONE);

        /*
          PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options
         */
        base_spinner = (Spinner) getView().findViewById(R.id.type3_spinner);
        Overlays overlays = this;
        base_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
                setType3Value(pos);
                if (pos == 0) {
                    toggle_all.setChecked(false);
                    new LoadOverlays(overlays).execute("");
                } else {
                    toggle_all.setChecked(false);
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new LoadOverlays(overlays).execute(commands);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        base_spinner.setEnabled(false);
        ArrayList<VariantItem> type3 = new ArrayList<>();

        try {
            Resources themeResources = getContext().getPackageManager()
                    .getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();

            ArrayList<String> stringArray = new ArrayList<>();

            File f = new File(getContext().getCacheDir().getAbsoluteFile() +
                    SUBSTRATUM_BUILDER_CACHE +
                    theme_pid + "/assets/overlays/android/");
            if (!References.checkOMS(getContext())) {
                File check_file = new File(getContext().getCacheDir().getAbsoluteFile() +
                        SUBSTRATUM_BUILDER_CACHE + theme_pid + "/assets/overlays_legacy/android/");
                if (check_file.exists() && check_file.isDirectory()) {
                    f = new File(check_file.getAbsolutePath());
                }
            }

            String[] listArray = themeAssetManager.list("overlays/android");
            Collections.addAll(stringArray, listArray);

            if (stringArray.contains("type3") || stringArray.contains("type3.enc")) {
                InputStream inputStream;
                if (encrypted) {
                    inputStream = FileOperations.getInputStream(
                            themeAssetManager,
                            "overlays/android/type3.enc",
                            cipher);
                } else {
                    inputStream = themeAssetManager.open("overlays/android/type3");
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream))) {
                    String formatter = reader.readLine();
                    type3.add(new VariantItem(formatter, null));
                } catch (IOException e) {
                    Log.e(TAG, "There was an error parsing asset file!");
                    type3.add(new VariantItem(getString(R.string
                            .overlays_variant_default_3), null));
                }
                inputStream.close();
            } else {
                type3.add(new VariantItem(getString(R.string.overlays_variant_default_3), null));
            }

            if (stringArray.size() > 1) {
                for (int i = 0; i < stringArray.size(); i++) {
                    String current = stringArray.get(i);
                    if (!current.equals("res") &&
                            !current.contains(".") &&
                            current.length() >= 6 &&
                            current.substring(0, 6).equals("type3_")) {
                        type3.add(new VariantItem(current.substring(6), null));
                    }
                }
                VariantAdapter adapter1 = new VariantAdapter(getActivity(), type3, R.layout.preview_spinner_dark);
                if (type3.size() > 1) {
                    toggle_all_overlays_text.setVisibility(View.GONE);
                    base_spinner.setVisibility(View.VISIBLE);
                    base_spinner.setAdapter(adapter1);
                } else {
                    toggle_all_overlays_text.setVisibility(View.VISIBLE);
                    base_spinner.setVisibility(View.INVISIBLE);
                    new LoadOverlays(this).execute("");
                }
            } else {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
                new LoadOverlays(this).execute("");
            }
        } catch (Exception e) {
            if (base_spinner.getVisibility() == View.VISIBLE) {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
            }
            if (materialProgressBar != null) {
                materialProgressBar.setVisibility(View.GONE);
            }
            e.printStackTrace();
            Log.e(TAG, "Could not parse list of base options for this theme!");
        }

        int prefType3Pos = prefs.getInt("type3_value", 0);
        base_spinner.setSelection(prefType3Pos);

        // Enable job listener
        jobReceiver = new JobReceiver();
        IntentFilter intentFilter = new IntentFilter("Overlays.START_JOB");
        localBroadcastManager2 = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager2.registerReceiver(jobReceiver, intentFilter);
        return true;
    }

    protected List<String> updateEnabledOverlays() {
        List<String> state5 = ThemeManager.listOverlays(getContext(), STATE_APPROVED_ENABLED);
        ArrayList<String> all = new ArrayList<>(state5);

        all_installed_overlays = new ArrayList<>();

        // ValidatorFilter out icon pack overlays from all overlays
        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).endsWith(".icon")) {
                all_installed_overlays.add(all.get(i));
            }
        }
        return new ArrayList<>(all_installed_overlays);
    }

    protected void failedFunction(Context context) {
        // Add dummy intent to be able to close the notification on click
        Intent notificationIntent = new Intent(context, this.getClass());
        notificationIntent.putExtra("theme_name", theme_name);
        notificationIntent.putExtra("theme_pid", theme_pid);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        Toast.makeText(
                context,
                context.getString(R.string.toast_compiled_updated_with_errors),
                Toast.LENGTH_LONG).show();

        try {
            invokeLogCharDialog(context);
        } catch (Exception e) {
        }
    }

    public void invokeLogCharDialog(Context context) {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.logcat_dialog);
        dialog.setTitle(R.string.logcat_dialog_title);
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT);

        TextView text = (TextView) dialog.findViewById(R.id.textField);
        text.setText(error_logs);
        ImageButton confirm = (ImageButton) dialog.findViewById(R.id.confirm);
        confirm.setOnClickListener(view -> dialog.dismiss());

        ImageButton copy_clipboard = (ImageButton) dialog.findViewById(R.id.copy_clipboard);
        copy_clipboard.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("substratum_log", error_logs);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        R.string.logcat_dialog_copy_success,
                        Toast.LENGTH_LONG).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            getContext().unregisterReceiver(finishReceiver);
            localBroadcastManager2.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    protected boolean needsRecreate(Context context) {
        for (OverlaysItem oi : checkedOverlays) {
            String packageName = oi.getPackageName();
            if (packageName.equals("android") || packageName.equals("org.omnirom.substratum")) {
                if (ThemeManager.isOverlayEnabled(context, oi.getFullOverlayParameters())) {
                    return false;
                } else if (compile_enable_mode) {
                    return false;
                }
            }
        }
        return References.checkOMS(getContext()) && !has_failed;
    }

    public VariantItem setTypeOneSpinners(Object typeArrayRaw,
                                          String package_identifier,
                                          String type) {
        InputStream inputStream = null;
        try {
            // Bypasses the caching mode, since if it's encrypted, caching mode is useless anyways
            if (encrypted) {
                inputStream = FileOperations.getInputStream(
                        themeAssetManager,
                        overlaysDir + "/" + package_identifier + "/type1" + type + ".enc",
                        cipher);
            } else {
                inputStream = themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + "/type1" + type);
            }
        } catch (IOException ioe) {
            // Suppress warning
        }

        // Parse current default types on type3 base resource folders
        String parsedVariant = "";
        try {
            if (base_spinner.getSelectedItemPosition() != 0) {
                parsedVariant = base_spinner.getSelectedItem().toString().replaceAll("\\s+", "");
            }
        } catch (NullPointerException npe) {
            // Suppress warning
        }
        String suffix = ((parsedVariant.length() != 0) ? "/type3_" + parsedVariant : "/res");

        // Type1 Spinner Text Adjustments
        assert inputStream != null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            // This adjusts it so that we have the spinner text set
            String formatter = reader.readLine();
            // This is the default type1 xml hex, if present
            String hex = null;

            if (encrypted) {
                try (InputStream name = FileOperations.getInputStream(
                        themeAssetManager,
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml.enc",
                        cipher)) {
                    hex = References.getOverlayResource(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try (InputStream name = themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml")) {
                    hex = References.getOverlayResource(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new VariantItem(formatter, hex);
        } catch (Exception e) {
            // When erroring out, put the default spinner text
            Log.d(TAG, "Falling back to default base variant text...");
            String hex = null;
            if (encrypted) {
                try (InputStream input = FileOperations.getInputStream(
                        themeAssetManager,
                        overlaysDir + "/" + package_identifier +
                                suffix + "/values/type1" + type + ".xml.enc",
                        cipher)) {
                    hex = References.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            } else {
                try (InputStream input = themeAssetManager.open(overlaysDir +
                        "/" + package_identifier + suffix + "/values/type1" + type + ".xml")) {
                    hex = References.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            }
            switch (type) {
                case "a":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1a), hex);
                case "b":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1b), hex);
                case "c":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1c), hex);
                default:
                    return null;
            }
        }
    }

    public VariantItem setTypeTwoSpinners(InputStreamReader inputStreamReader) {
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return new VariantItem(reader.readLine(), null);
        } catch (Exception e) {
            Log.d(TAG, "Falling back to default base variant text...");
            return new VariantItem(getString(R.string.overlays_variant_default_2), null);
        }
    }

    public VariantItem setTypeOneHexAndSpinner(String current, String package_identifier) {
        if (encrypted) {
            try (InputStream inputStream = FileOperations.getInputStream(themeAssetManager,
                    "overlays/" + package_identifier + "/" + current, cipher)) {
                String hex = References.getOverlayResource(inputStream);

                return new VariantItem(
                        current.substring(7, current.length() - 8), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (InputStream inputStream = themeAssetManager.open(
                    "overlays/" + package_identifier + "/" + current)) {
                String hex = References.getOverlayResource(inputStream);

                return new VariantItem(
                        current.substring(7, current.length() - 4), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void refreshList() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        if (overlaysLists != null) {
            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    currentOverlay.setSelected(false);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private static class LoadOverlays extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;

        LoadOverlays(Overlays fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            if (fragment.materialProgressBar != null) {
                fragment.materialProgressBar.setVisibility(View.VISIBLE);
            }
            fragment.mRecyclerView.setVisibility(View.INVISIBLE);
            fragment.swipeRefreshLayout.setVisibility(View.GONE);
            fragment.toggle_all.setEnabled(false);
            fragment.base_spinner.setEnabled(false);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            if (fragment.materialProgressBar != null) {
                fragment.materialProgressBar.setVisibility(View.GONE);
            }
            fragment.toggle_all.setEnabled(true);
            fragment.base_spinner.setEnabled(true);
            fragment.mAdapter = new OverlaysAdapter(fragment.values2);
            fragment.mRecyclerView.setAdapter(fragment.mAdapter);
            fragment.mAdapter.notifyDataSetChanged();
            fragment.mRecyclerView.setVisibility(View.VISIBLE);
            fragment.swipeRefreshLayout.setVisibility(View.VISIBLE);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            Context context = fragment.getActivity();
            // Refresh asset manager
            try {
                try {
                    Resources themeResources = context.getPackageManager()
                            .getResourcesForApplication(fragment.theme_pid);
                    fragment.themeAssetManager = themeResources.getAssets();
                } catch (PackageManager.NameNotFoundException e) {
                    // Suppress exception
                }

                // Grab the current theme_pid's versionName so that we can version our overlays
                fragment.versionName = References.grabAppVersion(context, fragment.theme_pid);
                List<String> state5overlays = fragment.updateEnabledOverlays();
                String parse1_themeName = fragment.theme_name.replaceAll("\\s+", "");
                String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

                ArrayList<String> values = new ArrayList<>();
                fragment.values2 = new ArrayList<>();

                // Buffer the initial values list so that we get the list of packages
                // inside this theme

                ArrayList<String> overlaysFolder = new ArrayList<>();
                try {
                    String[] overlayList = fragment.themeAssetManager.list(Overlays
                            .overlaysDir);
                    Collections.addAll(overlaysFolder, overlayList);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                values.addAll(overlaysFolder.stream().filter(package_name -> (References
                        .isPackageInstalled(context, package_name) ||
                        References.allowedSystemUIOverlay(package_name) ||
                        References.allowedSettingsOverlay(package_name)))
                        .collect(Collectors.toList()));

                // Create the map for {package name: package identifier}
                HashMap<String, String> unsortedMap = new HashMap<>();

                // Then let's convert all the package names to their app names
                for (int i = 0; i < values.size(); i++) {
                    try {
                        if (References.allowedSystemUIOverlay(values.get(i))) {
                            String package_name = "";
                            switch (values.get(i)) {
                                case "com.android.systemui.headers":
                                    package_name = context.getString(R.string.systemui_headers);
                                    break;
                                case "com.android.systemui.navbars":
                                    package_name = context.getString(R.string.systemui_navigation);
                                    break;
                                case "com.android.systemui.statusbars":
                                    package_name = context.getString(R.string.systemui_statusbar);
                                    break;
                                case "com.android.systemui.tiles":
                                    package_name = context.getString(R.string.systemui_qs_tiles);
                                    break;
                            }
                            unsortedMap.put(values.get(i), package_name);
                        } else if (References.allowedSettingsOverlay(values.get(i))) {
                            String package_name = "";
                            switch (values.get(i)) {
                                case "com.android.settings.icons":
                                    package_name = context.getString(R.string.settings_icons);
                                    break;
                            }
                            unsortedMap.put(values.get(i), package_name);
                        } else if (References.allowedAppOverlay(values.get(i))) {
                            ApplicationInfo applicationInfo = context.getPackageManager()
                                    .getApplicationInfo
                                            (values.get(i), 0);
                            String packageTitle = context.getPackageManager()
                                    .getApplicationLabel
                                            (applicationInfo).toString();
                            unsortedMap.put(values.get(i), packageTitle);
                        }
                    } catch (Exception e) {
                        // Exception
                    }
                }

                // Sort the values list
                List<Pair<String, String>> sortedMap = MapUtils.sortMapByValues(unsortedMap);

                // Now let's add the new information so that the adapter can recognize custom method
                // calls
                for (Pair<String, String> entry : sortedMap) {
                    String package_name = entry.second;
                    String package_identifier = entry.first;

                    try {
                        ArrayList<VariantItem> type1a = new ArrayList<>();
                        ArrayList<VariantItem> type1b = new ArrayList<>();
                        ArrayList<VariantItem> type1c = new ArrayList<>();
                        ArrayList<VariantItem> type2 = new ArrayList<>();
                        ArrayList<String> typeArray = new ArrayList<>();

                        Object typeArrayRaw;
                        // Begin the no caching algorithm
                        typeArrayRaw = fragment.themeAssetManager.list(
                                Overlays.overlaysDir + "/" + package_identifier);

                        // Sort the typeArray so that the types are asciibetical
                        Collections.addAll(typeArray, (String[]) typeArrayRaw);
                        Collections.sort(typeArray);

                        if (!References.checkOMS(context)) {
                            File check_file = new File(
                                    context.getCacheDir().getAbsoluteFile() +
                                            References.SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid
                                            + "/assets/overlays_legacy/" + package_identifier +
                                            "/");
                            if (check_file.exists() && check_file.isDirectory()) {
                                typeArrayRaw = new File(check_file.getAbsolutePath());
                            }
                        }

                        // Sort the typeArray so that the types are asciibetical
                        Collections.sort(typeArray);

                        // Let's start adding the type xmls to be parsed into the spinners
                        if (typeArray.contains("type1a") || typeArray.contains("type1a.enc")) {
                            type1a.add(fragment.setTypeOneSpinners(
                                    typeArrayRaw, package_identifier, "a"));
                        }

                        if (typeArray.contains("type1b") || typeArray.contains("type1b.enc")) {
                            type1b.add(fragment.setTypeOneSpinners(
                                    typeArrayRaw, package_identifier, "b"));
                        }

                        if (typeArray.contains("type1c") || typeArray.contains("type1c.enc")) {
                            type1c.add(fragment.setTypeOneSpinners(
                                    typeArrayRaw, package_identifier, "c"));
                        }

                        boolean type2checker = false;
                        for (int i = 0; i < typeArray.size(); i++) {
                            if (typeArray.get(i).startsWith("type2_")) {
                                type2checker = true;
                                break;
                            }
                        }
                        if (typeArray.contains("type2") ||
                                typeArray.contains("type2.enc") ||
                                type2checker) {
                            InputStreamReader inputStreamReader = null;
                            try {
                                inputStreamReader =
                                        new InputStreamReader(
                                                FileOperations.getInputStream(
                                                        fragment.themeAssetManager,
                                                        Overlays.overlaysDir + "/" +
                                                                package_identifier +
                                                                (fragment.encrypted ?
                                                                        "/type2.enc" :
                                                                        "/type2"),
                                                        (fragment.encrypted ?
                                                                fragment.cipher :
                                                                null)));
                            } catch (Exception e) {
                                // Suppress warning
                            }
                            type2.add(fragment.setTypeTwoSpinners(inputStreamReader));
                        }
                        if (typeArray.size() > 1) {
                            for (int i = 0; i < typeArray.size(); i++) {
                                String current = typeArray.get(i);
                                if (!current.equals("res")) {
                                    if (current.contains(".xml")) {
                                        switch (current.substring(0, 7)) {
                                            case "type1a_":
                                                type1a.add(
                                                        fragment.setTypeOneHexAndSpinner(
                                                                current, package_identifier));
                                                break;
                                            case "type1b_":
                                                type1b.add(
                                                        fragment.setTypeOneHexAndSpinner(
                                                                current, package_identifier));
                                                break;
                                            case "type1c_":
                                                type1c.add(
                                                        fragment.setTypeOneHexAndSpinner(
                                                                current, package_identifier));
                                                break;
                                        }
                                    } else if (!current.contains(".") && current.length() > 5 &&
                                            current.substring(0, 6).equals("type2_")) {
                                        type2.add(new VariantItem(current.substring(6), null));
                                    }
                                }
                            }

                            VariantAdapter adapter1 = new VariantAdapter(context, type1a, R.layout.preview_spinner);
                            VariantAdapter adapter2 = new VariantAdapter(context, type1b, R.layout.preview_spinner);
                            VariantAdapter adapter3 = new VariantAdapter(context, type1c, R.layout.preview_spinner);
                            VariantAdapter adapter4 = new VariantAdapter(context, type2, R.layout.preview_spinner);

                            boolean adapterOneChecker = type1a.size() == 0;
                            boolean adapterTwoChecker = type1b.size() == 0;
                            boolean adapterThreeChecker = type1c.size() == 0;
                            boolean adapterFourChecker = type2.size() == 0;

                            OverlaysItem overlaysItem =
                                    new OverlaysItem(
                                            parse2_themeName,
                                            package_name,
                                            package_identifier,
                                            false,
                                            (adapterOneChecker ? null : adapter1),
                                            (adapterTwoChecker ? null : adapter2),
                                            (adapterThreeChecker ? null : adapter3),
                                            (adapterFourChecker ? null : adapter4),
                                            context,
                                            fragment.versionName,
                                            sUrl[0],
                                            state5overlays,
                                            References.checkOMS(context));
                            fragment.values2.add(overlaysItem);
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Substratum.getInstance().getApplicationContext());
                            if (type1a.size() != 0) {
                                String prefType1aValue = prefs.getString("type1a_value" + ":" + package_identifier, null);
                                if (!TextUtils.isEmpty(prefType1aValue)) {
                                    int prefPosition = 0;
                                    for (VariantItem i : type1a) {
                                        if (prefType1aValue.equals(i.getVariantName())) {
                                            overlaysItem.setSelectedVariant(prefPosition);
                                            break;
                                        }
                                        prefPosition++;
                                    }
                                }
                            }
                            if (type1b.size() != 0) {
                                String prefType1bValue = prefs.getString("type1b_value" + ":" + package_identifier, null);
                                if (!TextUtils.isEmpty(prefType1bValue)) {
                                    int prefPosition = 0;
                                    for (VariantItem i : type1b) {
                                        if (prefType1bValue.equals(i.getVariantName())) {
                                            overlaysItem.setSelectedVariant2(prefPosition);
                                            break;
                                        }
                                        prefPosition++;
                                    }
                                }
                            }
                            if (type1c.size() != 0) {
                                String prefType1cValue = prefs.getString("type1c_value" + ":" + package_identifier, null);
                                if (!TextUtils.isEmpty(prefType1cValue)) {
                                    int prefPosition = 0;
                                    for (VariantItem i : type1c) {
                                        if (prefType1cValue.equals(i.getVariantName())) {
                                            overlaysItem.setSelectedVariant3(prefPosition);
                                            break;
                                        }
                                        prefPosition++;
                                    }
                                }
                            }
                            if (type2.size() != 0) {
                                String prefType2Value = prefs.getString("type2_value" + ":" + package_identifier, null);
                                if (!TextUtils.isEmpty(prefType2Value)) {
                                    int prefPosition = 0;
                                    for (VariantItem i : type2) {
                                        if (prefType2Value.equals(i.getVariantName())) {
                                            overlaysItem.setSelectedVariant4(prefPosition);
                                            break;
                                        }
                                        prefPosition++;
                                    }
                                }
                            }
                        } else {
                            // At this point, there is no spinner adapter, so it should be null
                            OverlaysItem overlaysItem =
                                    new OverlaysItem(
                                            parse2_themeName,
                                            package_name,
                                            package_identifier,
                                            false,
                                            null,
                                            null,
                                            null,
                                            null,
                                            context,
                                            fragment.versionName,
                                            sUrl[0],
                                            state5overlays,
                                            References.checkOMS(context));
                            fragment.values2.add(overlaysItem);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                // Consume window disconnection
            }
            return null;
        }
    }

    protected static class FinishReceiver extends BroadcastReceiver {
        private WeakReference<Overlays> ref;

        FinishReceiver(Overlays fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String PRIMARY_COMMAND_KEY = "primary_command_key";
            String COMMAND_VALUE_JOB_COMPLETE = "job_complete";
            String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);

            if (command.equals(COMMAND_VALUE_JOB_COMPLETE)) {
                Log.d(TAG,
                        "Substratum is now refreshing its resources after " +
                                "successful job completion!");
                ref.get().isWaiting = false;
            }
        }
    }

    protected class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded()) return;
            String command = intent.getStringExtra("command");

            boolean runCommand = true;
            boolean restartUIEnabled = prefs.getBoolean("enable_restart_systemui", true);
            if (restartUIEnabled) {
                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                ArrayList<String> checkedOverlays = new ArrayList<>();

                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = overlaysLists.get(i);
                    if (currentOverlay.isSelected()) {
                        checkedOverlays.add(currentOverlay.getPackageName());
                    }
                }
                boolean wouldRestartSystemUI = ThemeManager.willRestartUI(getContext(), checkedOverlays);
                if (wouldRestartSystemUI) {
                    if (!prefs.getBoolean("warning_auto_restart_systemui", false)) {
                        runCommand = false;
                        prefs.edit().putBoolean("warning_auto_restart_systemui", true).commit();
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(R.string.restart_systemui_warning_title);
                        builder.setMessage(R.string.restart_auto_systemui_warning)
                                .setPositiveButton(android.R.string.ok, (dialog, id18) -> {
                                    dialog.dismiss();
                                    doJobCommand(command);
                                })
                                .setNegativeButton(android.R.string.cancel, (dialog, id18) -> {
                                    dialog.dismiss();
                                });
                        builder.create();
                        builder.show();
                    }
                }
            }
            if (runCommand) {
                doJobCommand(command);
            }
        }

        private void doJobCommand(String command) {
            switch (command) {
                case "CompileEnable":
                    if (mAdapter != null) startCompileEnableMode();
                    break;
                case "Disable":
                    if (mAdapter != null) startDisableNew();
                    break;
                case "Enable":
                    if (mAdapter != null) {
                        startEnableNew();
                    }
                    break;
            }
        }
    }

    public void setType1Value(String packageName, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            String type1PrefKey = key + ":" + packageName;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(type1PrefKey, value);
            editor.commit();
        }
    }

    public void setType2Value(String packageName, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            String type2PrefKey = key + ":" + packageName;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(type2PrefKey, value);
            editor.commit();
        }
    }

    public void setType3Value(int pos) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("type3_value", pos);
        editor.commit();
    }

    public void reloadList() {
        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            currentOverlay.updateEnabledOverlays(updateEnabledOverlays());
        }
        mAdapter.notifyDataSetChanged();
    }


    static class EnableDisableTheme extends AsyncTask<Pair<ArrayList<String>, ArrayList<String>>, Void, Void> {
        private WeakReference<Overlays> ref;
        private WeakReference<Context> refContext;
        private WeakReference<Activity> refActivity;

        EnableDisableTheme(Overlays fragment, Context context, Activity activity) {
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
            fragment.mProgressDialog = new ProgressDialog(context);
            fragment.mProgressDialog.setMessage(context.getResources().getString(R.string.overlay_enable_progress));
            fragment.mProgressDialog.setIndeterminate(true);
            fragment.mProgressDialog.setCancelable(false);
            fragment.mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            Overlays fragment = ref.get();
            try {
                fragment.reloadList();
                fragment.mProgressDialog.cancel();
            } catch (Exception e) {
            }
        }

        @Override
        protected Void doInBackground(Pair<ArrayList<String>, ArrayList<String>>... overlays) {
            Overlays fragment = ref.get();
            Context context = fragment.getContext();
            if (context == null) {
                context = refContext.get();
            }
            if (overlays[0].first.size() != 0) {
                if (overlays[0].second.size() != 0) {
                    ThemeManager.disableOverlaySilent(context, overlays[0].first);
                } else {
                    ThemeManager.disableOverlay(context, overlays[0].first);
                }
            }
            if (overlays[0].second.size() != 0) {
                ThemeManager.enableOverlay(context, overlays[0].second);
            }
            return null;
        }
    }

    public ArrayList<String> getDisableOverlays() {
        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
        List<String> stateEnabled = ThemeManager.listOverlays(getContext(), STATE_APPROVED_ENABLED);
        ArrayList<String> checkedOverlays = new ArrayList<>();
        ArrayList<String> enabledOverlays = new ArrayList<>(stateEnabled);

        OverlaysItem androidItem = null;
        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            if (currentOverlay.isAndroidPackage()) {
                androidItem = currentOverlay;
            }
            // silent always disables everything
            if (currentOverlay.isSelected()) {
                enabledOverlays.remove(currentOverlay.getFullOverlayParameters());
                for (int j = 0; j < stateEnabled.size(); j++) {
                    String current = stateEnabled.get(j);
                    // add any matching package
                    if (current.equals(currentOverlay.getFullOverlayParameters())) {
                        checkedOverlays.add(current);
                    }
                }
            }
        }

        // we disable android only if it the last one matching and no active enabled overlay is left
        // that is not checked in this operation - toogle all will always remove android
        if (!toggle_all.isChecked()) {
            if (checkedOverlays.size() > 1 || enabledOverlays.size() != 0) {
                checkedOverlays.remove(androidItem.getFullOverlayParameters());
            }
        }
        return checkedOverlays;
    }

    public boolean canEnableOverlays() {
        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
        ArrayList<String> checkedOverlays = new ArrayList<>();

        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            String currentFullName = currentOverlay.getFullOverlayParameters();
            if (currentOverlay.isAndroidPackage()) {
                if (!currentOverlay.isPackageInstalled(currentFullName)) {
                    // we must never enable anything if we dont have a valid android package
                    return false;
                }
            }
            // checked - package installed but not enabled
            if (currentOverlay.isSelected() && currentOverlay.isPackageInstalled(currentFullName) && !currentOverlay.isOverlayEnabled()) {
                checkedOverlays.add(currentFullName);
            }
        }
        return checkedOverlays.size() != 0;
    }

    private void setOverlaySelected(boolean isChecked) {
        try {
            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                currentOverlay.setSelected(isChecked);
                mAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Window has lost connection with the host.");
        }
    }

    public boolean canCompileOverlays() {
        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
        ArrayList<String> checkedOverlays = new ArrayList<>();

        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            String currentFullName = currentOverlay.getFullOverlayParameters();
            if (currentOverlay.isSelected() && !currentOverlay.isOverlayEnabled()) {
                checkedOverlays.add(currentFullName);
            }
        }
        return checkedOverlays.size() != 0;
    }
}
