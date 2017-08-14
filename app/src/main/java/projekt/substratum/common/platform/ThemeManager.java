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

package projekt.substratum.common.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.om.OverlayInfo;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static android.content.om.OverlayInfo.STATE_NOT_APPROVED_DANGEROUS_OVERLAY;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;

public class ThemeManager {

    /**
     * Begin interaction with the OverlayManagerService binaries.
     * <p>
     * These methods will concurrently list all the possible functions that is open to Substratum
     * for usage with the OMS7 and OMS7-R systems.
     * <p>
     * NOTE: Deprecation at the OMS3 level. We no longer support OMS3 commands.
     */
    private static final String[] blacklistedPackages = new String[]{
            INTERFACER_PACKAGE,
    };

    public static boolean blacklisted(String packageName, Boolean unsupportedSamsung) {
        List<String> blacklisted = new ArrayList<>(Arrays.asList(blacklistedPackages));
        if (unsupportedSamsung) {
            blacklisted.addAll(new ArrayList<>(Arrays.asList(Resources.ALLOWED_SETTINGS_ELEMENTS)));
            blacklisted.add("android");
            blacklisted.add("com.android.settings");
            blacklisted.add("com.android.settings.icons");
            blacklisted.add("com.android.systemui");
            blacklisted.add("com.android.systemui.headers");
            blacklisted.add("com.android.systemui.tiles");
        }
        return blacklisted.contains(packageName);
    }

    public static void enableOverlay(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_APPROVED_ENABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.enableOverlays(context, overlays, shouldRestartUI(context,
                    overlays));
        }
    }

    public static void disableOverlay(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_APPROVED_DISABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.disableOverlays(context, overlays, shouldRestartUI(context,
                    overlays));
        }
    }

    public static void disableOverlaySilent(Context context, ArrayList<String> overlays) {
        if (overlays.isEmpty()) return;
        overlays.removeAll(listOverlays(context, STATE_APPROVED_DISABLED));
        if (overlays.isEmpty()) return;

        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.disableOverlays(context, overlays, false);
        }
    }

    public static void setPriority(Context context, ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.setPriority(
                    context, overlays, shouldRestartUI(context, overlays));
        }
    }

    public static void disableAllThemeOverlays(Context context) {
        List<String> list = ThemeManager.listOverlays(context, STATE_APPROVED_ENABLED).stream()
                .filter(o -> References.grabOverlayParent(context, o) != null)
                .collect(Collectors.toList());
        ThemeManager.disableOverlay(context, new ArrayList<>(list));
    }

    public static void restartSystemUI(Context context) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.restartSystemUI(context);
        }
    }

    public static void forceStopService(Context context) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.forceStopService(context);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> listAllOverlays(Context context) {
        List<String> list = new ArrayList<>();
        try {
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                for (String targetPackageName : set) {
                    for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                        if (oi.isApproved()) {
                            list.add(oi.packageName);
                        }
                    }
                }
            }
        } catch (Exception | NoSuchMethodError e) {
            // At this point, we probably ran into a legacy command or stock OMS
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<String> listOverlays(Context context, int state) {
        List<String> list = new ArrayList<>();
        try {
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                for (String targetPackageName : set) {
                    for (OverlayInfo oi : allOverlays.get(targetPackageName)) {
                        if (state == STATE_APPROVED_ENABLED && oi.isEnabled()) {
                            list.add(oi.packageName);
                        } else if (state == STATE_APPROVED_DISABLED && !oi.isEnabled()) {
                            list.add(oi.packageName);
                        } else if (state <= STATE_NOT_APPROVED_DANGEROUS_OVERLAY &&
                                !oi.isApproved()) {
                            list.add(oi.packageName);
                        }
                    }
                }
            }
        } catch (Exception | NoSuchMethodError e) {
            // At this point, we probably ran into a legacy command or stock OMS
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<String> listTargetWithMultipleOverlaysEnabled(Context context) {
        List<String> list = new ArrayList<>();
        try {
            Map<String, List<OverlayInfo>> allOverlays = OverlayManagerService.getAllOverlays();
            if (allOverlays != null) {
                Set<String> set = allOverlays.keySet();
                for (String targetPackageName : set) {
                    List<OverlayInfo> targetOverlays = allOverlays.get(targetPackageName);
                    int targetOverlaysSize = targetOverlays.size();
                    int count = 0;

                    for (OverlayInfo oi : targetOverlays) {
                        if (oi.isEnabled()) {
                            count++;
                        }
                    }
                    if (targetOverlaysSize > 1 && count > 1) {
                        list.add(targetPackageName);
                    }
                }
            }
        } catch (Exception | NoSuchMethodError e) {
        }
        return list;
    }

    public static boolean isOverlay(Context context, String target) {
        List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i).equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> listOverlaysByTheme(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            if (References.grabOverlayParent(context, overlays.get(i)).equals(target)) {
                list.add(overlays.get(i));
            }
        }
        return list;
    }

    public static List<String> listOverlaysForTarget(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listAllOverlays(context);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static List<String> listEnabledOverlaysForTarget(Context context, String target) {
        List<String> list = new ArrayList<>();
        List<String> overlays = listOverlays(context, STATE_APPROVED_ENABLED);
        list.addAll(overlays.stream().filter(o -> o.startsWith(target))
                .collect(Collectors.toList()));
        return list;
    }

    public static boolean isOverlayEnabled(Context context, String overlayName) {
        List<String> enabledOverlays = ThemeManager.listOverlays(context, STATE_APPROVED_ENABLED);
        for (String o : enabledOverlays) {
            if (o.equals(overlayName)) return true;
        }
        return false;
    }

    /*
        Begin interaction with the ThemeInterfacerService or the PackageManager binaries.

        These methods will handle all possible commands to be sent to PackageManager when handling
        an overlay, such as installing and uninstalling APKs directly on the device.
     */

    public static void installOverlay(Context context, String overlay) {
        if (checkThemeInterfacer(context)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(overlay);
            ThemeInterfacerService.installOverlays(context, list);
        }
    }

    public static void installOverlay(Context context, ArrayList<String> overlays) {
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.installOverlays(context, overlays);
        }
    }

    public static void uninstallOverlay(Context context,
                                        ArrayList<String> overlays) {
        ArrayList<String> temp = new ArrayList<>(overlays);
        temp.removeAll(listOverlays(context, STATE_APPROVED_DISABLED));
        disableOverlay(context, temp);

        // if enabled list is not contains any overlays
        if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.uninstallOverlays(
                    context,
                    overlays,
                    false);
        }
    }

    public static boolean shouldRestartUI(Context context, ArrayList<String> overlays) {
        if (checkOMS(context)) {
            for (String o : overlays) {
                if (o.startsWith("com.android.systemui")) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Substratum.getInstance().getApplicationContext());
                    return prefs.getBoolean("enable_restart_systemui", true);
                }
            }
        }
        return false;
    }

    public static boolean willRestartUI(Context context, ArrayList<String> overlays) {
        if (checkOMS(context)) {
            for (String o : overlays) {
                if (o.startsWith("com.android.systemui")) {
                    return true;
                }
            }
        }
        return false;
    }
}