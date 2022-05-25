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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import capacitor.android.plugins.R;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BridgeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BridgeFragment extends Fragment {

    private static final String ARG_START_DIR = "startDir";

    protected Bridge bridge;
    protected boolean keepRunning = true;

    private final List<Class<? extends Plugin>> initialPlugins = new ArrayList<>();
    private CapConfig config = null;

    private final List<WebViewListener> webViewListeners = new ArrayList<>();

    public BridgeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param startDir the directory to serve content from
     * @return A new instance of fragment BridgeFragment.
     */
    public static BridgeFragment newInstance(String startDir) {
        BridgeFragment fragment = new BridgeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_START_DIR, startDir);
        fragment.setArguments(args);
        return fragment;
    }

    public void addPlugin(Class<? extends Plugin> plugin) {
        this.initialPlugins.add(plugin);
    }

    public void setConfig(CapConfig config) {
        this.config = config;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void addWebViewListener(WebViewListener webViewListener) {
        webViewListeners.add(webViewListener);
    }

    /**
     * Load the WebView and create the Bridge
     */
    protected void load(Bundle savedInstanceState) {
        Logger.debug("Loading Bridge with BridgeFragment");

        Bundle args = getArguments();
        String startDir = null;

        if (args != null) {
            startDir = getArguments().getString(ARG_START_DIR);
        }

        bridge =
            new Bridge.Builder(this)
                .setInstanceState(savedInstanceState)
                .setPlugins(initialPlugins)
                .setConfig(config)
                .addWebViewListeners(webViewListeners)
                .create();

        if (startDir != null) {
            bridge.setServerAssetPath(startDir);
        }

        this.keepRunning = bridge.shouldKeepRunning();
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.bb_bridge_fragment);
        CharSequence c = a.getString(R.styleable.bb_bridge_fragment_start_dir);

        if (c != null) {
            String startDir = c.toString();
            Bundle args = new Bundle();
            args.putString(ARG_START_DIR, startDir);
            setArguments(args);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.bb_fragment_bridge, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.load(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.bridge != null) {
            this.bridge.onDestroy();
        }
    }
}
