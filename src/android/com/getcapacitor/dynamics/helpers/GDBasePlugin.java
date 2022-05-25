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

package com.good.gd.cordova.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

/**
 * Base class for all GD plugin objects.
 */
public class GDBasePlugin extends CordovaPlugin {

  public static final String FILE_TRANSFER_SERVICE_NAME = "com.good.gdservice.transfer-file";
  public static final String FILE_TRANSFER_SERVICE_VERSION = "1.0.0.0";
  public static final String FILE_TRANSFER_METHOD = "transferFile";

  private static final String INVALID_ARGUMENT_MESSAGE = "Invalid argument passed";
  private static final String INVALID_METHOD_NAME_MESSAGE = "Invalid method name message";

  /**
   * Default exception handler.
   *
   * @param throwable       error or exception thrown.
   * @param callbackContext plugin callback context.
   * @return false by default, which means plugin call failed.
   */
  protected boolean handleException(final Throwable throwable,
                                    final CallbackContext callbackContext) {
    callbackContext.error(throwable.getMessage());

    return false;
  }

  /**
   * Exception which describes situation when invalid argument was passed to plugin.
   */
  public static class InvalidMethodNamePassedException extends RuntimeException {

    @Override
    public String getMessage() {
      return INVALID_METHOD_NAME_MESSAGE;
    }

    @Override
    public String getLocalizedMessage() {
      return getMessage();
    }
  }

  /**
   * Exception which describes situation when invalid argument was passed to plugin.
   */
  public static class InvalidArgumentPassedException extends RuntimeException {

    @Override
    public String getMessage() {
      return INVALID_ARGUMENT_MESSAGE;
    }

    @Override
    public String getLocalizedMessage() {
      return getMessage();
    }
  }

  /**
   * Error which occurs when invalid method was successfully passed to plugin.
   */
  public static class InvalidMethodNameError extends Error {

    @Override
    public String getLocalizedMessage() {
      return getMessage();
    }

    @Override
    public String getMessage() {
      return "Invalid method name. This case shouldn't be possible";
    }
  }

}
