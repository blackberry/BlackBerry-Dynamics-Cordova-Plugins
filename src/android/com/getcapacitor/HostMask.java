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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface HostMask {
    boolean matches(String host);

    class Parser {

        private static HostMask NOTHING = new Nothing();

        public static HostMask parse(String[] masks) {
            return masks == null ? NOTHING : HostMask.Any.parse(masks);
        }

        public static HostMask parse(String mask) {
            return mask == null ? NOTHING : HostMask.Simple.parse(mask);
        }
    }

    class Simple implements HostMask {

        private final List<String> maskParts;

        private Simple(List<String> maskParts) {
            if (maskParts == null) {
                throw new IllegalArgumentException("Mask parts can not be null");
            }
            this.maskParts = maskParts;
        }

        static Simple parse(String mask) {
            List<String> parts = Util.splitAndReverse(mask);
            return new Simple(parts);
        }

        @Override
        public boolean matches(String host) {
            if (host == null) {
                return false;
            }
            List<String> hostParts = Util.splitAndReverse(host);
            int hostSize = hostParts.size();
            int maskSize = maskParts.size();
            if (maskSize > 1 && hostSize != maskSize) {
                return false;
            }

            int minSize = Math.min(hostSize, maskSize);

            for (int i = 0; i < minSize; i++) {
                String maskPart = maskParts.get(i);
                String hostPart = hostParts.get(i);
                if (!Util.matches(maskPart, hostPart)) {
                    return false;
                }
            }
            return true;
        }
    }

    class Any implements HostMask {

        private final List<? extends HostMask> masks;

        Any(List<? extends HostMask> masks) {
            this.masks = masks;
        }

        @Override
        public boolean matches(String host) {
            for (HostMask mask : masks) {
                if (mask.matches(host)) {
                    return true;
                }
            }
            return false;
        }

        static Any parse(String... rawMasks) {
            List<HostMask.Simple> masks = new ArrayList<>();
            for (String raw : rawMasks) {
                masks.add(HostMask.Simple.parse(raw));
            }
            return new Any(masks);
        }
    }

    class Nothing implements HostMask {

        @Override
        public boolean matches(String host) {
            return false;
        }
    }

    class Util {

        static boolean matches(String mask, String string) {
            if (mask == null) {
                return false;
            } else if ("*".equals(mask)) {
                return true;
            } else if (string == null) {
                return false;
            } else {
                return mask.toUpperCase().equals(string.toUpperCase());
            }
        }

        static List<String> splitAndReverse(String string) {
            if (string == null) {
                throw new IllegalArgumentException("Can not split null argument");
            }
            List<String> parts = Arrays.asList(string.split("\\."));
            Collections.reverse(parts);
            return parts;
        }
    }
}
