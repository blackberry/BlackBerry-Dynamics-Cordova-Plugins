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

import android.net.MailTo;
import java.util.Map;

/**
 *
 * BBD MailTo URL parser
 *
 * This class parses a mailto scheme URL and then can be queried for
 * the parsed parameters.
 *
 */
public class BBDMailTo {

    private final MailTo mailto;
    private final Map<String, String> url_headers;

    private static final String TO_RECIPIENT = "to";
    private static final String CC_RECIPIENT = "cc";
    private static final String BCC_RECIPIENT = "bcc";
    private static final String ATTACHMENT_HEADER_KEY = "attachment";

    private static final String COMA_SEPARATOR = ",";

    public BBDMailTo(String url) {
        // DEVNOTE:
        // We need to replace all occurrences of "file://" because MailTo.parse works incorrectly with "file://" scheme
        mailto = MailTo.parse(url.replace("file://", ""));
        url_headers = mailto.getHeaders();
    }

    String[] getRecipients(String type) {
        String recipients;

        switch (type) {
            case TO_RECIPIENT:
                recipients = mailto.getTo();
                break;
            case CC_RECIPIENT:
                recipients = mailto.getCc();
                break;
            case BCC_RECIPIENT:
                recipients = url_headers.get(BCC_RECIPIENT);
                break;
            default:
                recipients = "";
        }

        if (recipients == null) {
            recipients = "";
        }

        return trimStringEntriesInArray(recipients.split(COMA_SEPARATOR));
    }

    String getSubject() {
        String subject = mailto.getSubject();

        return subject == null ? "" : subject;
    }

    public String getBody() {
        String body = mailto.getBody();

        return body == null ? "" : body;
    }

    String[] getAttachments() {
        String attachments = url_headers.get(ATTACHMENT_HEADER_KEY);
        if (attachments == null) {
            return new String[] {};
        }

        String[] attachmentsArr = attachments.split(COMA_SEPARATOR);

        return trimStringEntriesInArray(attachmentsArr);
    }

    private String[] trimStringEntriesInArray(String[] arrayToTrimEntries) {
        for (int i = 0, entriesCount = arrayToTrimEntries.length; i < entriesCount; i++) {
            arrayToTrimEntries[i] = arrayToTrimEntries[i].trim();
        }

        return arrayToTrimEntries;
    }
}
