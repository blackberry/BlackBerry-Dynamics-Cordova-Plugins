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

package com.good.gd.cordova.core.mailto;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.good.gd.GDAndroid;
import com.good.gd.GDServiceProvider;
import com.good.gd.GDServiceType;
import com.good.gd.cordova.plugins.helpers.CordovaGDServiceHelper;
import com.good.gd.icc.GDServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MailTo plugin for sending secure emails without attachments to GFE or BB Work
 */

public class BBDCordovaMailTo {

    private static final String EMAIL_TO = "to";
    private static final String EMAIL_CC = "cc";
    private static final String EMAIL_BCC = "bcc";
    private static final String EMAIL_SUBJECT = "subject";
    private static final String EMAIL_BODY = "body";
    private static final String SEND_EMAIL_SERVICE_NAME = "com.good.gfeservice.send-email";
    private static final String SEND_EMAIL_SERVICE_VERSION = "1.0.0.0";
    private static final String SEND_EMAIL_METHOD_NAME = "sendEmail";
    private static final int MAX_MESSAGE_LENGTH = 20000;
    private final List<GDServiceProvider> BBD_APPS_LIST = getListOfInstalledBBDApps();
    private static final String LOG_TAG = BBDCordovaMailTo.class.getSimpleName();

    private final Context context;
    private final BBDMailTo mailTo;
    private String[] attachments;

    protected BBDCordovaMailTo(String url, Context context) {
        this.mailTo = new BBDMailTo(url);
        this.context = context;
        this.attachments = null;
    }

    protected void collectAttachments() {
        this.attachments = mailTo.getAttachments();
    }

    void sendEmailToMailToClientApp() {
        if (isDataLeakagePreventionOff()) {
            sendEmailViaIntent();

            return;
        }
        if (BBD_APPS_LIST == null) {
            showWarningDialog();

            return;
        }
        if (BBD_APPS_LIST.size() == 1) {
            final String appAddress = BBD_APPS_LIST.get(0).getAddress();

            sendEmailViaBBDApp(appAddress);

            return;
        }

        showDialogToSelectApp();
    }

    //  Return List Of Installed BlackBerry Dynamics Applications
    private List<GDServiceProvider> getListOfInstalledBBDApps() {

        final List<GDServiceProvider> appDetails = GDAndroid.getInstance().getServiceProvidersFor(
                SEND_EMAIL_SERVICE_NAME,
                SEND_EMAIL_SERVICE_VERSION,
                GDServiceType.GD_SERVICE_TYPE_APPLICATION
        );

        return appDetails == null || appDetails.isEmpty() ? null : appDetails;
    }

    //  Sending email via BBD application
    private void sendEmailViaBBDApp(final String appAddress) {
        try {
            CordovaGDServiceHelper.getInstance().sendTo(
                    appAddress,
                    SEND_EMAIL_SERVICE_NAME,
                    SEND_EMAIL_SERVICE_VERSION,
                    SEND_EMAIL_METHOD_NAME,
                    createParams(),
                    attachments
            );
        } catch (final GDServiceException exception) {
            Log.e(LOG_TAG, "Failed to send email via Dynamics application", exception);
        }
    }

    //  Sending email from native dialog applications list
    private void sendEmailViaIntent() {

        final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        String subject = mailTo.getSubject();
        String body = mailTo.getBody();

        // max size of content which can be transported via Intent is 1 Mib. Dependent on the string
        // encoding, this size is equal to string with the length of 520000 symbols. In different
        // cases the additional bytes will be used to store the string references, to store the
        // 'to', 'cc', 'bcc' values. So the subject and body content length will be cut to 20 000.
        // According to "Internet Message Format" RFC 5322, the length of the body, subject and other
        // fields can't be longer than 998 symbols. Now, the email client will display first 20 000
        // characters but will send only first 998 characters to the receiver (RFC 5322).

        if (subject.length() > MAX_MESSAGE_LENGTH) {
            subject = subject.substring(0, MAX_MESSAGE_LENGTH);
        }

        if (body.length() > MAX_MESSAGE_LENGTH) {
            body = body.substring(0, MAX_MESSAGE_LENGTH);
        }

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_EMAIL, mailTo.getRecipients(EMAIL_TO));
        intent.putExtra(Intent.EXTRA_CC, mailTo.getRecipients(EMAIL_CC));
        intent.putExtra(Intent.EXTRA_BCC, mailTo.getRecipients(EMAIL_BCC));

        context.startActivity(Intent.createChooser(intent, null));

    }

    //  Dialog window to select application to send email
    private void showDialogToSelectApp() {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        final String[] BBDAppNames = new String[BBD_APPS_LIST.size()];

        for (int i = 0, size = BBD_APPS_LIST.size(); i < size; i++)  {
            BBDAppNames[i] = BBD_APPS_LIST.get(i).getName();
        }

        final int[] checkedItem = { 0 };

        alertDialogBuilder.setTitle("Choose Application");
        alertDialogBuilder.setSingleChoiceItems(BBDAppNames, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem[0] = which;
                Log.d(LOG_TAG, "The application to create the template in has been chosen");
            }
        });

        // add OK and Cancel buttons
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String appAddress = BBD_APPS_LIST.get(checkedItem[0]).getAddress();

                sendEmailViaBBDApp(appAddress);
            }
        });

        alertDialogBuilder.setNegativeButton("Cancel", null);
        alertDialogBuilder.create().show();

    }

    private HashMap<String, Object> createParams() {

        final HashMap<String, Object> params = new HashMap<>();
        final ArrayList<String> toList = new ArrayList<>(Arrays.asList(mailTo.getRecipients(EMAIL_TO)));
        final ArrayList<String> ccList = new ArrayList<>(Arrays.asList(mailTo.getRecipients(EMAIL_CC)));
        final ArrayList<String> bccList = new ArrayList<>(Arrays.asList(mailTo.getRecipients(EMAIL_BCC)));

        params.put(EMAIL_SUBJECT, mailTo.getSubject());
        params.put(EMAIL_BODY, mailTo.getBody());
        params.put(EMAIL_TO, toList);
        params.put(EMAIL_CC, ccList);
        params.put(EMAIL_BCC, bccList);

        return params;
    }

    private void showWarningDialog() {
        final String message = "This command is not allowed under the current IT security settings";

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context).setTitle("Warning")
                .setMessage(message).setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        alertDialogBuilder.create().show();

    }


    private boolean isDataLeakagePreventionOff() {
        Map<String, Object> appConfig = GDAndroid.getInstance().getApplicationConfig();
        boolean dlpStatus = (boolean) appConfig.get(GDAndroid.GDAppConfigKeyPreventDataLeakageOut);

        return !dlpStatus;
    }
}
