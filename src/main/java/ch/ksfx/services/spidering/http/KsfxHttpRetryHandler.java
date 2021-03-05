/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.services.spidering.http;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.NoHttpResponseException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.logging.Logger;


public class KsfxHttpRetryHandler implements HttpMethodRetryHandler
{
    private int retryCount;
    private int retryDelay;
    private Logger logger = Logger.getLogger("LambaHttpRetryHandler.class");

    public KsfxHttpRetryHandler(int retryCount, int retryDelay) {
        this.retryCount = retryCount;
        this.retryDelay = retryDelay;
    }

    public boolean retryMethod(final HttpMethod method,
                               final IOException exception,
                               int executionCount) {
        if (method == null) {
            throw new IllegalArgumentException("HTTP method may not be null");
        }
        if (exception == null) {
            throw new IllegalArgumentException("Exception parameter may not be null");
        }
        // HttpMethod interface is the WORST thing ever done to HttpClient
        if (method instanceof HttpMethodBase) {
            if (((HttpMethodBase) method).isAborted()) {
                return false;
            }
        }
        if (executionCount > this.retryCount) {
            // Do not retry if over max retry count
            return false;
        }
        if (exception instanceof NoHttpResponseException) {
            // Retry if the server dropped connection on us
            sleep();
            return true;
        }
        if (exception instanceof InterruptedIOException) {
            // Timeout
            sleep();
            return true;
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof NoRouteToHostException) {
            // Host unreachable
            sleep();
            return true;
        }
        // otherwise do not retry
        sleep();
        return true;
    }

    private void sleep() {
        try {
            Thread.sleep(this.retryDelay);
        } catch (InterruptedException e) {
            logger.severe("Sleep interrupted");
        }
    }
}
