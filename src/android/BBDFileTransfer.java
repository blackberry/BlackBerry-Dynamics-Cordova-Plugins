/*
       Copyright (c) 2021 BlackBerry Limited. All Rights Reserved.
       Some modifications to the original Cordova FileTransfer plugin
       from https://github.com/apache/cordova-plugin-file-transfer/

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

package com.blackberry.bbd.cordova.plugins.filetransfer;

import com.blackberry.bbd.cordova.plugins.file.FileUtils;

import com.blackberry.bbd.cordova.apache.http.entity.GzipDecompressingEntity;

import com.good.gd.apache.http.Header;
import com.good.gd.apache.http.HeaderElement;
import com.good.gd.apache.http.HttpEntity;
import com.good.gd.apache.http.HttpResponse;
import com.good.gd.apache.http.HttpResponseInterceptor;
import com.good.gd.apache.http.client.HttpClient;
import com.good.gd.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import com.good.gd.apache.http.client.methods.HttpGet;
import com.good.gd.apache.http.client.methods.HttpPost;
import com.good.gd.apache.http.client.methods.HttpPut;
import com.good.gd.apache.http.client.methods.HttpUriRequest;
import com.good.gd.apache.http.entity.ContentType;
import com.good.gd.apache.http.entity.mime.MultipartEntityBuilder;
import com.good.gd.apache.http.impl.client.DefaultProxyAuthenticationHandler;
import com.good.gd.apache.http.impl.client.DefaultTargetAuthenticationHandler;
import com.good.gd.apache.http.protocol.HttpContext;
import com.good.gd.apache.http.util.EntityUtils;

import com.good.gd.cordova.core.utils.GDFileUtils;
import com.good.gd.cordova.plugins.GDBasePlugin;
import com.good.gd.cordova.plugins.helpers.delegates.GDFileSystemDelegate;
import com.good.gd.cordova.plugins.helpers.delegates.GDFileTransferDelegate;
import com.good.gd.cordova.plugins.helpers.delegates.GDHttpRequestDelegate;

import com.good.gd.file.GDFileSystem;
import com.good.gd.net.GDHttpClient;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaResourceApi.OpenForReadResult;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;;
import org.apache.cordova.PluginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import android.net.Uri;

public class BBDFileTransfer extends GDBasePlugin {

    private static final String LOG_TAG = "BBDFileTransfer";

    public static int FILE_NOT_FOUND_ERR = 1;
    public static int INVALID_URL_ERR = 2;
    public static int CONNECTION_ERR = 3;
    public static int ABORTED_ERR = 4;
    public static int NOT_MODIFIED_ERR = 5;



    private static HashMap<String, RequestContext> activeRequests = new HashMap<String, RequestContext>();
    private static final int MAX_BUFFER_SIZE = 16 * 1024;

    protected GDFileSystemDelegate gdFileSystemDelegate = new GDFileSystemDelegate();
    private GDFileTransferDelegate gdFileTransferDelegate = new GDFileTransferDelegate();
    private GDHttpRequestDelegate gdHttpRequestDelegate = new GDHttpRequestDelegate();

    private static final class RequestContext {
        String source;
        String target;
        File targetFile;
        CallbackContext callbackContext;
        HttpUriRequest httpRequest;
        long bytesSent;
        long contentLength;
        boolean aborted;

        RequestContext(String source, String target, CallbackContext callbackContext) {
            this.source = source;
            this.target = target;
            this.callbackContext = callbackContext;
            this.bytesSent = 0;
            this.contentLength = 0;
        }
        void sendPluginResult(PluginResult pluginResult) {
            synchronized (this) {
                if (!aborted) {
                    callbackContext.sendPluginResult(pluginResult);
                }
            }
        }
    }


    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("upload") || action.equals("download")) {
            String source = args.getString(0);
            String target = args.getString(1);

            if (action.equals("upload")) {
                upload(source, target, args, callbackContext);
            } else {
                download(source, target, args, callbackContext);
            }
            return true;
        } else if (action.equals("abort")) {
            String objectId = args.getString(0);
            abort(objectId);
            callbackContext.success();
            return true;
        }
        return false;
    }

    /**
     * Uploads the specified file to the server URL provided using an HTTP multipart request.
     * @param source        Full path of the file on the file system
     * @param target        URL of the server to receive the file
     * @param args          JSON Array of args
     * @param callbackContext    callback id for optional progress reports
     *
     * args[0]  filePath      Full path of the file on the file system
     * args[1]  server        URL of the server to receive the file
     * args[2]  fileKey       Name of file request parameter
     * args[3]  fileName      File name to be used on server
     * args[4]  mimeType      Describes file content type
     * args[5]  params        key:value pairs of user-defined parameters
     * args[6]  trustAllHosts Optional trust all hosts, defaults to false
     * args[7]  chunkedMode   Whether to upload the data in chunked streaming mode
     * args[8]  headers       A map of header name/header values
     * args[9]  id            Id of this requested object
     * args[10] httpMethod    HTTP method to use - either PUT or POST
     *
     * @return FileUploadResult containing result of upload request
     */
    private void upload(final String source, final String target, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(LOG_TAG, "upload " + source + " to " +  target);

        final String objectId = args.getString(9);

        final RequestContext context = new RequestContext(source, target, callbackContext);
        synchronized (activeRequests) {
            activeRequests.put(objectId, context);
        }

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (context.aborted) {
                    return;
                }
                uploadFile(context, args);

                synchronized (activeRequests) {
                    activeRequests.remove(objectId);
                }
            }
        });
    }

    private void uploadFile(final RequestContext context, final JSONArray args) {

        int httpStatus = 0;
        String entityData = null;

        try {
            // Setup the options
            final String fileKey = getArgument(args, 2, "file");
            final String fileName = getArgument(args, 3, "image.jpg");
            String mimeType = getArgument(args, 4, "image/jpeg");
            final JSONObject params = args.optJSONObject(5) == null ? new JSONObject() : args.optJSONObject(5);
            // Always use chunked mode unless set to false as per API
            boolean chunkedMode = args.optBoolean(7) || args.isNull(7);
            // Look for headers on the params map for backwards compatibility with older Cordova versions.
            final JSONObject headers = args.optJSONObject(8) == null ? params.optJSONObject("headers") : args.optJSONObject(8);
            final String httpMethod = getArgument(args, 10, "post");

            FileUtils filePlugin = getFilePlugin(webView);
            if (filePlugin == null) {
                LOG.e(LOG_TAG, "File plugin not found; cannot upload file");
                context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "File plugin not found; cannot upload file"));
                return;
            }

            final CordovaResourceApi resourceApi = webView.getResourceApi();
            final Uri targetUri = Uri.parse(context.target);

            int uriType = CordovaResourceApi.getUriType(targetUri);
            final boolean useHttps = uriType == CordovaResourceApi.URI_TYPE_HTTPS;
            if (uriType != CordovaResourceApi.URI_TYPE_HTTP && !useHttps) {
                JSONObject error = createFileTransferError(INVALID_URL_ERR, context.source, context.target, entityData, httpStatus, null);
                LOG.e(LOG_TAG, "Unsupported URI: " + targetUri);
                context.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, error));
                return;
            }

            // We should call remapUri on background thread otherwise it throws
            // IllegalStateException when trying to remap 'cdvfile://localhost/content/...' URIs
            // via ContentFilesystem (see https://issues.apache.org/jira/browse/CB-9022)
            Uri tmpSrc = Uri.parse(context.source);
            final Uri sourceUri = resourceApi.remapUri(
                    tmpSrc.getScheme() != null ? tmpSrc : Uri.fromFile(new File(context.source)));

            boolean islocalFilesystem = false;
            if (CordovaResourceApi.getUriType(sourceUri) == CordovaResourceApi.URI_TYPE_DATA) {
                chunkedMode = true; // data: must be handled with resourceApi.openForRead()
                String dataMimeType = resourceApi.getMimeType(sourceUri);
                if (dataMimeType != null) {
                    mimeType =  dataMimeType;
                }
            }
            else {
                final JSONObject fileSystemMetadata = filePlugin.getFilesystemMetadata(sourceUri);
                if (fileSystemMetadata == null) {
                    throw new IOException("File System not found by sourceUri");
                }
                String name = fileSystemMetadata.getString("name");
                islocalFilesystem = fileSystemMetadata.getBoolean("local");
                int type = fileSystemMetadata.getInt("type");
                String rootUri = fileSystemMetadata.getString("rootUri");
                String localPath = fileSystemMetadata.getString("localPath");
            }

            HttpEntity requestEntity = null;
            final HttpUriRequest httpRequest;
            if (httpMethod.toLowerCase().equals("post")) {
                httpRequest = new HttpPost(targetUri.toString());
            } else {
                httpRequest = new HttpPut(targetUri.toString());
            }
            MultipartEntityBuilder multipartEntityBuilder = null;
            if (headers == null || !(headers.has("Content-Type") || headers.has("content-type"))) {
                multipartEntityBuilder = MultipartEntityBuilder.create();
                // emulates browser compatibility
                multipartEntityBuilder.setLaxMode();

                final Iterator jsonKeys = params.keys();
                while (jsonKeys.hasNext()) {
                    final String key = (String) jsonKeys.next();
                    final String value = params.getString(key);
                    multipartEntityBuilder.addTextBody(key, value);
                }
            }

            final OnProgressEvent progressEventCallback = new OnProgressEvent() {
                @Override
                public void publishProgress(final int progress, final long loaded, final long total) {
                    try {
                        // Create return object
                        final FileProgressResult fileProgress = new FileProgressResult();
                        long contentLength = progress != 0 ? total : context.contentLength;
                        if (contentLength > 0) {
                            fileProgress.setLengthComputable(true);
                            fileProgress.setTotal(contentLength);
                        }
                        fileProgress.setLoaded(loaded);

                        synchronized (context) {
                            context.bytesSent = loaded;
                        }

                        final PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, fileProgress.toJSONObject());
                        pluginResult.setKeepCallback(true);
                        context.sendPluginResult(pluginResult);
                    } catch (final JSONException jsonException) {
                        throw new Error("Impossible case, cause of JSON key is not null");
                    }
                }
            };

            if (chunkedMode) {
                InputStream inputStream = null;
                long contentLength = 0;
                if (islocalFilesystem) {
                    final String realPath = GDFileUtils.getRealPath(sourceUri, cordova);
                    // Get a input stream of the file on the secure container
                    inputStream = gdFileSystemDelegate.openFileInput(realPath);

                    try {
                        contentLength = inputStream.available();
                    } 
                    catch (Exception e) {
                        // ignore
                    }

                } else {
                    // Get a input stream of the file on the phone
                    OpenForReadResult readResult = resourceApi.openForRead(sourceUri);
                    inputStream = readResult.inputStream;
                    contentLength = readResult.length;
                }

                if (contentLength > 0) {
                    synchronized (context) {
                        context.contentLength = contentLength;
                    }
                }

                if (multipartEntityBuilder != null) {
                    final InputStreamBody inputStreamBody = new InputStreamBody(inputStream, ContentType.create(mimeType), fileName, progressEventCallback);
                    multipartEntityBuilder.addPart(fileKey, inputStreamBody);
                    requestEntity = multipartEntityBuilder.build();
                }
                else {
                    requestEntity = new ProgressInputStreamEntity(inputStream, progressEventCallback);
                }

            } else {
                final String realPath = GDFileUtils.getRealPath(sourceUri, cordova);
                File file = null;
                if (islocalFilesystem) {
                    // Get a file on the secure container
                    file = gdFileSystemDelegate.createFile(realPath);
                }
                else {
                    // Get a file on the phone
                    file = new File(realPath);
                }
                if (!file.exists()) {
                    throw new IOException("upload: File not exist for sourceUri");
                }

                if (multipartEntityBuilder != null) {
                    final FileBody fileBody = new FileBody(file, ContentType.create(mimeType), fileName, progressEventCallback);
                    multipartEntityBuilder.addPart(fileKey, fileBody);
                    requestEntity = multipartEntityBuilder.build();
                }
                else {
                    String contentType = "text/plain";
                    if (headers.has("Content-Type")) {
                        contentType = headers.getString("Content-Type");
                    }
                    else if (headers.has("content-type")) {
                        contentType = headers.getString("content-type");
                    }
                    requestEntity = new ProgressFileEntity(file, contentType, progressEventCallback);
                }
            }

            ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(requestEntity);
            final HttpClient httpclient = gdFileTransferDelegate.getHttpClient();

            // disable built-in TargetAuthenticationHandler
            ((GDHttpClient) httpclient).setTargetAuthenticationHandler(new DefaultTargetAuthenticationHandler() {
                @Override
                public boolean isAuthenticationRequested(
                        final HttpResponse response,
                        final HttpContext context) {
                    return false;
                }
            });

            // disable built-in ProxyAuthenticationHandler
            ((GDHttpClient) httpclient).setProxyAuthenticationHandler(new DefaultProxyAuthenticationHandler() {
                @Override
                public boolean isAuthenticationRequested(
                        final HttpResponse response,
                        final HttpContext context) {
                    return false;
                }
            });

            if (headers != null) {
                for (Iterator<String> iter = headers.keys(); iter.hasNext(); ) {
                    String key = iter.next();
                    httpRequest.setHeader(key, headers.getString(key));
                }
            }
            httpRequest.setHeader("X-Requested-With", "XMLHttpRequest");

            synchronized (context) {
                context.httpRequest = httpRequest;
            }

            final HttpResponse response = httpclient.execute(httpRequest);
            httpStatus = response.getStatusLine().getStatusCode();

            if (httpRequest.isAborted()) {
                throw new IOException("upload: http request was aborted");
            }

            synchronized (context) {
                context.httpRequest = null;
            }

            final HttpEntity entity = response.getEntity();
            long bytesSent = requestEntity.getContentLength();
            if (bytesSent == -1) {
                bytesSent = context.bytesSent;
            }

            try {
                entityData = entity == null ? "" : gdFileTransferDelegate.getHttpEntityAsString(entity);
            }
            catch (IOException e) {
                // an unexpected exception, cannot read entity data
                LOG.e(LOG_TAG, "cannot read entity data", e);
                entityData = null;
                httpStatus = -1;
            }

            if (httpStatus != 200) {
                throw new IOException("upload: http error response");
            }

            // Create return object
            final FileUploadResult result = new FileUploadResult();
            // send request and retrieve response
            result.setResponseCode(httpStatus);
            result.setBytesSent(bytesSent);
            result.setResponse(entityData);

            context.sendPluginResult(new PluginResult(PluginResult.Status.OK, result.toJSONObject()));
        } catch (FileNotFoundException e) {
            JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, context.source, context.target, entityData, httpStatus, e);
            LOG.e(LOG_TAG, error.toString(), e);
            context.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, error));
        } catch (final IOException e) {
            int errorCode = CONNECTION_ERR;
            if (httpStatus == 401) {
                errorCode = INVALID_URL_ERR;
            }
            JSONObject error = createFileTransferError(errorCode, context.source, context.target, entityData, httpStatus, e);
            LOG.e(LOG_TAG, error.toString(), e);
            context.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, error));
        } catch (final JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
            context.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
        } catch (final Throwable t) {
            // Shouldn't happen, but will
            JSONObject error = createFileTransferError(CONNECTION_ERR, context.source, context.target, null, httpStatus, t);
            LOG.e(LOG_TAG, error.toString(), t);
            context.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, error));
        }
    }

    /**
     * Downloads a file form a given URL and saves it to the specified directory.
     *
     * @param source        URL of the server to receive the file
     * @param target        Full path of the file on the file system
     * @param args          JSON Array of args
     * @param callbackContext    callback id for optional progress reports
     *
     * args[0]  source        URL of the server to receive the file
     * args[1]  target        Full path of the file on the file system
     * args[2]  trustAllHosts Optional trust all hosts, defaults to false
     * args[3]  id            Id of this requested object
     * args[4]  headers       A map of header name/header values
    */
    private void download(final String source, final String target, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(LOG_TAG, "download " + source + " to " +  target);

        final String objectId = args.getString(3);

        final RequestContext context = new RequestContext(source, target, callbackContext);
        synchronized (activeRequests) {
            activeRequests.put(objectId, context);
        }

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (context.aborted) {
                    return;
                }
                downloadFile(context, args);

                synchronized (activeRequests) {
                    activeRequests.remove(objectId);
                }
            }
        });
    }

    private void downloadFile(final RequestContext context, final JSONArray args) {

        PluginResult result = null;
        FileProgressResult progress = new FileProgressResult();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        int httpStatus = 0;
        String entityData = null;
        File file = null;
        FileUtils filePlugin = getFilePlugin(webView);
        if (filePlugin == null) {
            LOG.e(LOG_TAG, "File plugin not found; cannot save downloaded file");
            context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "File plugin not found; cannot save downloaded file"));
            return;
        }

        try {
            final CordovaResourceApi resourceApi = webView.getResourceApi();

            final Uri sourceUri = resourceApi.remapUri(Uri.parse(context.source));
            int uriType = CordovaResourceApi.getUriType(sourceUri);
            final boolean useHttps = uriType == CordovaResourceApi.URI_TYPE_HTTPS;
            final boolean isLocalTransfer = !useHttps && uriType != CordovaResourceApi.URI_TYPE_HTTP;
            if (uriType == CordovaResourceApi.URI_TYPE_UNKNOWN) {
                JSONObject error = createFileTransferError(INVALID_URL_ERR, context.source, context.target, null, 0, null);
                LOG.e(LOG_TAG, "Unsupported URI: " + sourceUri);
                context.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, error));
                return;
            }

            LOG.d(LOG_TAG, "Download file:" + sourceUri);

            if (isLocalTransfer) {
                boolean islocalFilesystem = false;
                // data: must be handled with resourceApi.openForRead()
                if (CordovaResourceApi.getUriType(sourceUri) != CordovaResourceApi.URI_TYPE_DATA) {
                    final JSONObject fileSystemMetadata = filePlugin.getFilesystemMetadata(sourceUri);
                    if (fileSystemMetadata == null) {
                        throw new IOException("download: File System not found by sourceUri");
                    }
                    String name = fileSystemMetadata.getString("name");
                    islocalFilesystem = fileSystemMetadata.getBoolean("local");
                    int type = fileSystemMetadata.getInt("type");
                    String rootUri = fileSystemMetadata.getString("rootUri");
                    String localPath = fileSystemMetadata.getString("localPath");
                }

                if (islocalFilesystem) {
                    final String realPath = GDFileUtils.getRealPath(sourceUri, cordova);
                    // Get a input stream of the file on the secure container
                    inputStream = gdFileSystemDelegate.openFileInput(realPath);
                    try {
                        long length = inputStream.available();
                        if (length > 0) {
                            progress.setLengthComputable(true);
                            progress.setTotal(length);
                        }
                    }
                    catch (Exception e) {
                        // ignore
                    }
                }
                else {
                    // Get a input stream of the file on the phone
                    OpenForReadResult readResult = resourceApi.openForRead(sourceUri);
                    if (readResult.length > 0) {
                        progress.setLengthComputable(true);
                        progress.setTotal(readResult.length);
                    }
                    inputStream = readResult.inputStream;
                }
            }
            else {
                final HttpClient httpclient = gdFileTransferDelegate.getHttpClient();

                ((GDHttpClient)httpclient).addResponseInterceptor(new HttpResponseInterceptor() {
                    @Override
                    public void process(final HttpResponse response, final HttpContext context) throws IOException {
                        // interceptor process would be called for received response headers or entity
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            Header ceheader = entity.getContentEncoding();
                            if (ceheader != null) {
                                HeaderElement[] codecs = ceheader.getElements();
                                for (int i = 0; i < codecs.length; i++) {
                                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                        response.setEntity( new GzipDecompressingEntity(response.getEntity()));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                });


                // disable built-in TargetAuthenticationHandler
                ((GDHttpClient)httpclient).setTargetAuthenticationHandler(new DefaultTargetAuthenticationHandler() {
                    @Override
                    public boolean isAuthenticationRequested(
                        final HttpResponse response,
                        final HttpContext context) {
                        return false;
                    }
                });

                // disable built-in ProxyAuthenticationHandler
                ((GDHttpClient)httpclient).setProxyAuthenticationHandler(new DefaultProxyAuthenticationHandler() {
                    @Override
                    public boolean isAuthenticationRequested(
                        final HttpResponse response,
                        final HttpContext context) {
                        return false;
                    }
                });

                final HttpUriRequest httpRequest = new HttpGet(sourceUri.toString());
                final JSONObject headers = args.optJSONObject(4);

                if (headers != null) {
                    for (Iterator<String> iter = headers.keys();iter.hasNext();) {
                        String key = iter.next();
                        httpRequest.setHeader(key, headers.getString(key));
                    }
                }

                httpRequest.setHeader("Accept-Encoding", "gzip");

                synchronized (context) {
                    context.httpRequest = httpRequest;
                }

                final HttpResponse response = httpclient.execute(httpRequest);
                httpStatus = response.getStatusLine().getStatusCode();

                synchronized (context) {
                    context.httpRequest = null;
                }

                if (httpRequest.isAborted()) {
                    throw new IOException("download: http request was aborted");
                }

                final HttpEntity entity = response.getEntity();

                if (httpStatus != 200) {
                    if (entity != null) {
                        entityData = gdHttpRequestDelegate.getHttpEntityAsString(entity);
                    }
                    throw new IOException("download: http error response");
                }
                if (entity == null) {
                    throw new IOException("download: no downloaded entity");
                }

                final byte[] rawData = EntityUtils.toByteArray(entity);
                progress.setLengthComputable(true);
                progress.setTotal(rawData.length);
                inputStream = new ByteArrayInputStream(rawData);
            }

            // Accept a path or a URI for the source.
            Uri tmpTarget = Uri.parse(context.target);
            Uri targetUri = resourceApi.remapUri(
                        tmpTarget.getScheme() != null ? tmpTarget : Uri.fromFile(new File(context.target)));

            final JSONObject fileSystemMetadata = filePlugin.getFilesystemMetadata(targetUri);
            if (fileSystemMetadata == null) {
                throw new IOException("File System not found by targetUri");
            }

            String name = fileSystemMetadata.getString("name");
            boolean islocalFilesystem = fileSystemMetadata.getBoolean("local");
            int type = fileSystemMetadata.getInt("type");
            String rootUri = fileSystemMetadata.getString("rootUri");
            String localPath = fileSystemMetadata.getString("localPath");

            String filePath = null;
            if (islocalFilesystem) {
                // Get a output stream of the file on the secure container
                filePath = GDFileUtils.getRealPath(targetUri.toString(), cordova);
                outputStream = gdFileSystemDelegate.openFileOutput(filePath, GDFileSystem.MODE_PRIVATE);
                file = gdFileSystemDelegate.createFile(filePath);
            }
            else {
                // Get a output stream of the file on the phone
                filePath = targetUri.getPath();
                outputStream = resourceApi.openOutputStream(targetUri);
                file = new File(filePath);
            }

            synchronized (context) {
                if (context.aborted) {
                    throw new IOException("download: force aborted");
                }
                context.targetFile = file;
            }

            LOG.d(LOG_TAG, "Saved file: " + filePath);

            // write bytes to file
            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            int bytesRead = 0;
            long bytesLoaded = 0;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
                // Send a progress event.
                bytesLoaded += bytesRead;
                progress.setLoaded(bytesLoaded);
                PluginResult progressResult = new PluginResult(PluginResult.Status.OK, progress.toJSONObject());
                progressResult.setKeepCallback(true);
                context.sendPluginResult(progressResult);
            }

            final JSONObject fileEntry = new JSONObject();
            fileEntry.put("isFile", true);
            fileEntry.put("isDirectory", false);
            fileEntry.put("name", file.getName());
            fileEntry.put("fullPath", localPath);
            fileEntry.put("filesystem", type);
            fileEntry.put("filesystemName", name);

            result = new PluginResult(PluginResult.Status.OK, fileEntry);

        } catch (final FileNotFoundException e) {
            JSONObject error = createFileTransferError(FILE_NOT_FOUND_ERR, context.source, context.target, entityData, httpStatus, e);
            LOG.e(LOG_TAG, error.toString(), e);
            result = new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } catch (final JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
            result = new PluginResult(PluginResult.Status.JSON_EXCEPTION);
        } catch (final IOException e) {
            int errorCode = CONNECTION_ERR;
            if (httpStatus == 404) {
                errorCode = FILE_NOT_FOUND_ERR;
            }
            else if (httpStatus == 304) {
                errorCode = NOT_MODIFIED_ERR;
            }
            JSONObject error = createFileTransferError(errorCode, context.source, context.target, entityData, httpStatus, e);
            LOG.e(LOG_TAG, error.toString(), e);
            result = new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } catch (final Throwable t) {
            JSONObject error = createFileTransferError(CONNECTION_ERR, context.source, context.target, entityData, httpStatus, t);
            LOG.e(LOG_TAG, error.toString(), t);
            result = new PluginResult(PluginResult.Status.IO_EXCEPTION, error);
        } finally {
            safeClose(inputStream);
            safeClose(outputStream);

            if (result == null) {
                result = new PluginResult(PluginResult.Status.ERROR, createFileTransferError(CONNECTION_ERR, context.source, context.target, null, httpStatus, null));
            }
            // Remove incomplete download.
            if (result.getStatus() != PluginResult.Status.OK.ordinal() && file != null) {
                file.delete();
            }
            context.sendPluginResult(result);
        }
    }

    /**
     * Abort an ongoing upload or download.
     */
    private void abort(String objectId) {
        final RequestContext context;
        synchronized (activeRequests) {
            context = activeRequests.remove(objectId);
        }
        if (context != null) {
            // Closing the streams can block, so execute on a background thread.
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    synchronized (context) {
                        File file = context.targetFile;
                        if (file != null) {
                            file.delete();
                        }
                        // Trigger the abort callback immediately to minimize latency between it and abort() being called.
                        JSONObject error = createFileTransferError(ABORTED_ERR, context.source, context.target, null, -1, null);
                        context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, error));
                        context.aborted = true;
                        if (context.httpRequest != null) {
                            try {
                                context.httpRequest.abort();
                            } catch (Exception e) {
                                LOG.e(LOG_TAG, "unexpected fatal exception", e);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Create an error object based on the passed in errorCode
     * @param errorCode      the error
     * @return JSONObject containing the error
     */
    private static JSONObject createFileTransferError(int errorCode, String source, String target, String body, Integer httpStatus, Throwable throwable) {
        JSONObject error = null;
        try {
            error = new JSONObject();
            error.put("code", errorCode);
            error.put("source", source);
            error.put("target", target);
            if(body != null)
            {
                error.put("body", body);
            }
            if (httpStatus != null) {
                error.put("http_status", httpStatus);
            }
            if (throwable != null) {
                String msg = throwable.getMessage();
                if (msg == null || "".equals(msg)) {
                    msg = throwable.toString();
                }
                error.put("exception", msg);
            }
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        return error;
    }

    /**
     * Convenience method to read a parameter from the list of JSON args.
     * @param args          the args passed to the Plugin
     * @param position      the position to retrieve the arg from
     * @param defaultString the default to be used if the arg does not exist
     * @return String with the retrieved value
     */
    private static String getArgument(JSONArray args, int position, String defaultString) {
        String arg = defaultString;
        if (args.length() > position) {
            arg = args.optString(position);
            if (arg == null || "null".equals(arg)) {
                arg = defaultString;
            }
        }
        return arg;
    }

    private static void safeClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    private static FileUtils getFilePlugin(CordovaWebView webView) {
        final CordovaResourceApi resourceApi = webView.getResourceApi();
        Class webViewClass = webView.getClass();
        PluginManager pm = null;
        CordovaPlugin filePlugin = null;
        try {
            Method gpm = webViewClass.getMethod("getPluginManager");
            pm = (PluginManager) gpm.invoke(webView);
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        if (pm == null) {
            try {
                Field pmf = webViewClass.getField("pluginManager");
                pm = (PluginManager)pmf.get(webView);
            } catch (NoSuchFieldException e) {
            } catch (IllegalAccessException e) {
            }
        }
        if (pm != null) {
            filePlugin = pm.getPlugin("BBDFile");
            if (filePlugin != null && (filePlugin instanceof FileUtils)) {
                return (FileUtils)filePlugin;
            }
        }
        return null;
    }

}
