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

package ch.ksfx.web.services.spidering.http;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HttpClientHelper
{
    //TODO ms?
    private int retryDelay = 1000;
    private HttpClient httpClient;
    private HttpUserAgentHeaders headers;
    private int retryCount;
    private boolean torify;

    private static Logger logger = Logger.getLogger(HttpClientHelper.class.getName());
    private static final int DEFAULT_RETRY_DELAY = 10000;

    /**
     * Creates a new Http Client Helper
     *
     * @param headers User-Agent HTTP headers sent by the request
     */
    public HttpClientHelper(HttpUserAgentHeaders headers, int httpSocketTimeout, int retryCount, boolean torify)
    {
        this.torify = torify;
        this.httpClient = new HttpClient();
        this.httpClient.getParams().setSoTimeout(httpSocketTimeout);
        this.httpClient.setConnectionTimeout(httpSocketTimeout);
        this.httpClient.setHttpConnectionFactoryTimeout(httpSocketTimeout);

        httpClient.getParams().setParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        this.headers = headers;
        this.retryCount = retryCount;
    }

    public HttpClient getHttpClient()
    {
        return httpClient;
    }

    /**
     * Returns an executed GetMethod object with the given URL
     *
     * @param url URL for HTTP GET request
     * @return executed GetMethod object
     */
    public GetMethod executeGetMethod(String url)
    {
            url = encodeURL(url);
            GetMethod getMethod = new GetMethod(url);
            getMethod.setFollowRedirects(true);
            getMethod = (GetMethod) executeMethod(getMethod);
            return getMethod;
    }

    /**
     * Returns an executed PostMethod object with the given URL
     *
     * @param url URL for HTTP POST request
     * @return executed PostMethod object
     */
    public PostMethod executePostMethod(String url)
    {
        try {
            url = encodeURL(url);
            PostMethod postMethod = new PostMethod(url);
            postMethod.setFollowRedirects(true);
            postMethod = (PostMethod) executeMethod(postMethod);
            return postMethod;
        } catch(Exception e) {
            logger.severe("Error while generating POST method: " + e);
            return null;
        }
    }

    public PostMethod executePostMethod(String url, NameValuePair[] nameValuePairs)
    {
        try {
            url = encodeURL(url);
            PostMethod postMethod = new PostMethod(url);
            postMethod.setFollowRedirects(false);
            postMethod.setRequestBody(nameValuePairs);
            postMethod = (PostMethod) executeMethod(postMethod);
            return postMethod;
        } catch(Exception e) {
            logger.severe("Error while generating POST method: " + e);
            return null;
        }
    }

    /**
     * Returns an executed PostMethod object with the given URL and the given
     * RequestEntity object in the request body
     *
     * @param url URL for HTTP POST request
     * @param requestEntity RequestEntity for request body
     * @return executed PostMethod object
     */
    public PostMethod executePostMethod(String url, RequestEntity requestEntity)
    {
        try {
            url = encodeURL(url);
            PostMethod postMethod = new PostMethod(url);
            if(requestEntity != null) {
                postMethod.setFollowRedirects(false);
                postMethod.setRequestEntity(requestEntity);
            } else {
                postMethod.setFollowRedirects(true);
            }
            postMethod = (PostMethod) executeMethod(postMethod);
            return postMethod;
        } catch(Exception e) {
            logger.severe("Error while generating POST method: " + e);
            return null;
        }
    }

    private HttpMethod executeMethod(HttpMethod httpMethod)
    {
        for(Header header : this.headers.getHeaders()) {
            httpMethod.setRequestHeader(header);
        }
        
        httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                                            new KsfxHttpRetryHandler(retryCount, retryDelay));
        
        try {
            int tryCount = 0;
            int statusCode;
            do {
                if(tryCount > 1) {
                    httpMethod = createNewHttpMethod(httpMethod);
                    try {
                        if(retryDelay == 0) {
                            retryDelay = DEFAULT_RETRY_DELAY;
                        }
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        logger.severe("InterruptedException");
                    }
                }

                //PROXY Configuration
                /*
                if (torify) {
                    
                    String proxyHost = "";
                    Integer proxyPort = 0;

                    try {
                            proxyHost = SpiderConfiguration.getConfiguration().getString("torifyHost");
                            proxyPort = SpiderConfiguration.getConfiguration().getInt("torifyPort");
                    } catch (Exception e) {
                        logger.severe("Cannot get Proxy information");
                    }

                    this.httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
                }
                */

                statusCode = this.httpClient.executeMethod(httpMethod);
                tryCount++;
            } while(!(statusCode == HttpStatus.SC_OK ||
                      statusCode == HttpStatus.SC_FORBIDDEN ||
                      statusCode == HttpStatus.SC_NOT_FOUND) &&
                    tryCount < retryCount);
            if(statusCode != HttpStatus.SC_OK) {
                System.out.println("HTTP method failed: " + httpMethod.getStatusLine() + " - " + httpMethod.getURI().toString());
            }
        } catch(HttpException e) {
            e.printStackTrace();
            httpMethod.abort();
            try {
                logger.log(Level.SEVERE, "Redirrex " + e.getClass(), e);
                if (e.getClass().equals(RedirectException.class)) {
                    logger.log(Level.SEVERE, "Is real redirect exception", e);
                    throw new RuntimeException("HttpRedirectException");
                }
                logger.log(Level.SEVERE, "HTTP protocol error for URL: " + httpMethod.getURI().toString(), e);
            } catch (URIException e1) {
            	e.printStackTrace();
                logger.log(Level.SEVERE, "URI exception", e);
            }
            throw new RuntimeException("HttpException");
        } catch(IOException e) {
            try {
            	e.printStackTrace();
                logger.log(Level.SEVERE, "HTTP transport error for URL: " + httpMethod.getURI().toString(), e);
                
            } catch (URIException e1) {
            	e.printStackTrace();
                logger.log(Level.SEVERE, "URI exception", e);
                
            }
            throw new RuntimeException("IOException");
        }
        return httpMethod;
    }

    private String encodeURL(String url)
    {
        return URLEncoder.getURLEncoder().encode(url);
    }

    private HttpMethod createNewHttpMethod(HttpMethod oldMethod) throws URIException
    {
        HttpMethod httpMethod;
        if(oldMethod instanceof GetMethod) {
            httpMethod = new GetMethod();
        } else {
            httpMethod = new PostMethod();
            ((PostMethod) httpMethod).setRequestEntity(((PostMethod) oldMethod).getRequestEntity());
            httpMethod.setParams(oldMethod.getParams());
        }
        httpMethod.setURI(oldMethod.getURI());
        httpMethod.setFollowRedirects(oldMethod.getFollowRedirects());
        return httpMethod;
    }
}
