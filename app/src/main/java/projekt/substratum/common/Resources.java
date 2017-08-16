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

package projekt.substratum.common;

import static projekt.substratum.common.References.INTERFACER_PACKAGE;

public class Resources {

    // Filter to adjust Settings elements
    public static final String[] ALLOWED_SETTINGS_ELEMENTS = {
            "com.android.settings.icons",
    };

    // Default core packages
    public static final String[] CORE_SYSTEM_PACKAGES = {

            // Core AOSP System Packages
            "android",
            "com.android.browser",
            "com.android.calculator2",
            "com.android.calendar",
            "com.android.cellbroadcastreceiver",
            "com.android.contacts",
            "com.android.deskclock",
            "com.android.dialer",
            "com.android.documentsui",
            "com.android.emergency",
            "com.android.gallery3d",
            "com.android.inputmethod.latin",
            "com.android.launcher3",
            "com.android.messaging",
            "com.android.mms",
            "com.android.musicfx",
            "com.android.packageinstaller",
            "com.android.phone",
            "com.android.providers.media",
            "com.android.server.telecom",
            "com.android.settings",
            "com.android.systemui",

            // Google Packages
            "com.google.android.apps.nexuslauncher",
            "com.google.android.calculator",
            "com.google.android.contacts",
            "com.google.android.deskclock",
            "com.google.android.dialer",
            "com.google.android.packageinstaller",
            "com.google.android.tts",

            // Organization Packages
            "org.omnirom.substratum",
    };
    // Filter to adjust SystemUI elements
    static final String[] ALLOWED_SYSTEMUI_ELEMENTS = {
            "com.android.systemui.headers",
            "com.android.systemui.navbars",
            "com.android.systemui.statusbars",
            "com.android.systemui.tiles"
    };

    // Do not theme these packages on icon pack studio
    static final String[] BLACKLIST_STUDIO_TARGET_APPS = {
            "com.keramidas.TitaniumBackup",
            "com.android.cts.verifier",
            INTERFACER_PACKAGE
    };
    // Do not theme these packages
    static final String[] BLACKLIST_THEME_TARGET_APPS = {
            "com.android.cts.verifier",
            INTERFACER_PACKAGE
    };
}