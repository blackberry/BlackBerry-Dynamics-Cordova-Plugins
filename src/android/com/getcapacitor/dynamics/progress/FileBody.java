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

package com.good.gd.cordova.core.progress;

import com.good.gd.file.FileInputStream;

import com.good.gd.apache.http.entity.ContentType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Delegate FileBody class, to override writeTo method.
 */
public class FileBody extends com.good.gd.apache.http.entity.mime.content.FileBody {

    private static final int COMPLETE_PROGRESS = 100;

    private final File fileToUpload;

    private final OnProgressEvent progressEvent;

    private long writtenLength;

    private OutputStreamProgress outputStreamProgress;

    /**
     * Create new file body.
     *
     * @param file          file object which will be uploaded.
     * @param contentType   content type of file body.
     * @param mimeType      mime type of file body.
     * @param progressEvent callback for publishing result.
     */
    public FileBody(final File file,
                    final ContentType contentType,
                    final String mimeType,
                    final OnProgressEvent progressEvent) {
        super(file, contentType, mimeType);

        this.fileToUpload = file;
        this.progressEvent = progressEvent;
    }

    @Override
    public void writeTo(final OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        outputStreamProgress = new OutputStreamProgress(outputStream);

        final FileInputStream inputStream = new FileInputStream(fileToUpload);
        try {
            final byte[] buffer = new byte[4096]; //Change this buffer, to see some progress on small files.
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStreamProgress.write(buffer, 0, length);
                progressEvent.publishProgress(getProgress(),
                        writtenLength, getContentLength());
            }
            outputStream.flush();
        } finally {
            inputStream.close();
        }
    }

    private int getProgress() {
        final long contentLength = getContentLength();
        if (contentLength <= 0) { // Prevent division by zero and negative values
            return 0;
        }
        writtenLength = outputStreamProgress.getWrittenLength();

        return (int) (COMPLETE_PROGRESS * writtenLength / contentLength);
    }

    /**
     * Callback for publishing progress while uploading a file.
     */
    public interface OnProgressEvent {


        /**
         * Publish progress while uploading a file.
         *
         * @param progress progress of upload. Progress range is 1-100.
         * @param loaded   total loaded bytes.
         * @param total    total bytes size.
         */
        void publishProgress(final int progress, final long loaded,
                             final long total);
    }
}
