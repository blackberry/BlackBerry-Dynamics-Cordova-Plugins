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

import android.content.Context;

public class MailToInterceptor {

    private static final MailToInterceptor instance = new MailToInterceptor();
    private MailToProvider mailToProvider = new MailToProvider();

    /**
     * Provides instance of MailToInterceptor.
     *
     * @return instance of MailToInterceptor.
     */
    public static MailToInterceptor getInstance() {
        return instance;
    }

    /**
     * Checks whether url is mailTo, if true, parse url,
     * and starts Good Mail application
     *
     * @param url     Url to parse
     * @param context Activity context
     */
    public void interceptMailTo(final String url, final Context context) {

        BBDCordovaMailTo mailTo = mailToProvider.createMailTo(url, context);
        mailTo.sendEmailToMailToClientApp();

    }

    public void setMailToProvider(MailToProvider mtProvider){
        this.mailToProvider = mtProvider;
    }

}
