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

import java.lang.reflect.Method;

public class PluginMethodHandle {

    // The reflect method reference
    private final Method method;
    // The name of the method
    private final String name;
    // The return type of the method (see PluginMethod for constants)
    private final String returnType;

    public PluginMethodHandle(Method method, PluginMethod methodDecorator) {
        this.method = method;

        this.name = method.getName();

        this.returnType = methodDecorator.returnType();
    }

    public String getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public Method getMethod() {
        return method;
    }
}
