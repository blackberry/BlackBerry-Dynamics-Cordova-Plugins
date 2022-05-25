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
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class JSArray extends JSONArray {

    public JSArray() {
        super();
    }

    public JSArray(String json) throws JSONException {
        super(json);
    }

    public JSArray(Collection copyFrom) {
        super(copyFrom);
    }

    public JSArray(Object array) throws JSONException {
        super(array);
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> toList() throws JSONException {
        List<E> items = new ArrayList<>();
        Object o = null;
        for (int i = 0; i < this.length(); i++) {
            o = this.get(i);
            try {
                items.add((E) this.get(i));
            } catch (Exception ex) {
                throw new JSONException("Not all items are instances of the given type");
            }
        }
        return items;
    }

    /**
     * Create a new JSArray without throwing a error
     */
    public static JSArray from(Object array) {
        try {
            return new JSArray(array);
        } catch (JSONException ex) {}
        return null;
    }
}
