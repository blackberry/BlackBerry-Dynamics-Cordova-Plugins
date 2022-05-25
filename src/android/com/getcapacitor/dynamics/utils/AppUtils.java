/*
       Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.

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

package com.good.gd.cordova.core.utils;

import android.os.Build;

public class AppUtils {

  private static final String OLD_EMULATOR_IDENTIFIER = "goldfish";
  private static final String NEW_EMULATOR_IDENTIFIER = "ranchu";

  public static final String VERSION_NAME = "BUILD_NUMBER";

  /**
   * Used to check if application is ran on device or emulator.
   *
   * @return true if emulator, otherwise false
   */
  public static boolean isEmulator() {
    return OLD_EMULATOR_IDENTIFIER.equals(Build.HARDWARE) || NEW_EMULATOR_IDENTIFIER.equals(Build.HARDWARE);
  }
}
