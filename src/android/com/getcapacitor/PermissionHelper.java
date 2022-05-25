/*
       Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
       Some modifications to the original capacitor-android project:
       https://github.com/ionic-team/capacitor/tree/main/android/capacitor/src/main/java/com/getcapacitor

       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.good.gd.cordova.capacitor;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A helper class for checking permissions.
 *
 * @since 3.0.0
 */
public class PermissionHelper {

    /**
     * Checks if a list of given permissions are all granted by the user
     *
     * @since 3.0.0
     * @param permissions Permissions to check.
     * @return True if all permissions are granted, false if at least one is not.
     */
    public static boolean hasPermissions(Context context, String[] permissions) {
        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether the given permission has been defined in the AndroidManifest.xml
     *
     * @since 3.0.0
     * @param permission A permission to check.
     * @return True if the permission has been defined in the Manifest, false if not.
     */
    public static boolean hasDefinedPermission(Context context, String permission) {
        boolean hasPermission = false;
        String[] requestedPermissions = PermissionHelper.getManifestPermissions(context);
        if (requestedPermissions != null && requestedPermissions.length > 0) {
            List<String> requestedPermissionsList = Arrays.asList(requestedPermissions);
            ArrayList<String> requestedPermissionsArrayList = new ArrayList<>(requestedPermissionsList);
            if (requestedPermissionsArrayList.contains(permission)) {
                hasPermission = true;
            }
        }
        return hasPermission;
    }

    /**
     * Check whether all of the given permissions have been defined in the AndroidManifest.xml
     * @param context the app context
     * @param permissions a list of permissions
     * @return true only if all permissions are defined in the AndroidManifest.xml
     */
    public static boolean hasDefinedPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!PermissionHelper.hasDefinedPermission(context, permission)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the permissions defined in AndroidManifest.xml
     *
     * @since 3.0.0
     * @return The permissions defined in AndroidManifest.xml
     */
    public static String[] getManifestPermissions(Context context) {
        String[] requestedPermissions = null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

            if (packageInfo != null) {
                requestedPermissions = packageInfo.requestedPermissions;
            }
        } catch (Exception ex) {}
        return requestedPermissions;
    }

    /**
     * Given a list of permissions, return a new list with the ones not present in AndroidManifest.xml
     *
     * @since 3.0.0
     * @param neededPermissions The permissions needed.
     * @return The permissions not present in AndroidManifest.xml
     */
    public static String[] getUndefinedPermissions(Context context, String[] neededPermissions) {
        ArrayList<String> undefinedPermissions = new ArrayList<>();
        String[] requestedPermissions = getManifestPermissions(context);
        if (requestedPermissions != null && requestedPermissions.length > 0) {
            List<String> requestedPermissionsList = Arrays.asList(requestedPermissions);
            ArrayList<String> requestedPermissionsArrayList = new ArrayList<>(requestedPermissionsList);
            for (String permission : neededPermissions) {
                if (!requestedPermissionsArrayList.contains(permission)) {
                    undefinedPermissions.add(permission);
                }
            }
            String[] undefinedPermissionArray = new String[undefinedPermissions.size()];
            undefinedPermissionArray = undefinedPermissions.toArray(undefinedPermissionArray);

            return undefinedPermissionArray;
        }
        return neededPermissions;
    }
}
