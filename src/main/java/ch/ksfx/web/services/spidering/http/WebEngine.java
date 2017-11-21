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


import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.ResponseHeader;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Pure Java WebEngine class
 * <br><br>
 * The HTTP communication occurs with Commons HTTP Client.
 *
 * @author mvw
 * @version 1.0
 * @since 1.0
 */
//TODO Abstract class WebEngine is useless here
public class WebEngine
{
    private HttpUserAgentHeaders headers;

    private static final String NAME = "Java";

    private HttpClientHelper httpClientHelper;

    private static Logger logger = Logger.getLogger(WebEngine.class.getName());

    /**
     * Creates a new Java WebEngine
     */
    public WebEngine(HttpUserAgentHeaders headers)
    {
        this.headers = headers;

        boolean proxyfy = false;
        this.httpClientHelper = new HttpClientHelper(headers, 30000, 10, proxyfy);
    }

    public void loadResource(Resource resource) {
        HttpMethod httpMethod;

        try {
            if (/*resource.getHttpMethod().equals(GET)*/true) {
                String url = resource.getUrl();

                if (url != null) {
                   url = url.replaceAll("&amp;","&");
                   url = url.replaceAll("&quot;","\"");
                }

                httpMethod = this.httpClientHelper.executeGetMethod(url);
            } else {
                //TODO implement POST functionality
                /*
                NameValuePair[] nameValuePairs = new NameValuePair[resource.getPostData().size()];
                for(int i = 0; i < resource.getPostData().size(); i++) {
                    nameValuePairs[i] = new NameValuePair(resource.getPostData().get(i).getName(),
                                                          resource.getPostData().get(i).getValue());
                }

                String url = resource.getURL().toString();

                if (url != null) {
                   url = url.replaceAll("&amp;","&");
                   url = url.replaceAll("&quot;","\"");
                }

                httpMethod = this.httpClientHelper.executePostMethod(url,
                                                                     nameValuePairs);
                 */
            }

        } catch (Exception e) {
            resource.setLoadSucceed(false);
            resource.setHttpStatusCode(222);

            logger.log(Level.SEVERE, "Unable to load resource", e);
            return;
        }

        if(httpMethod == null) {
            resource.setLoadSucceed(false);
            return;
        }

        if(httpMethod.getStatusCode() != HttpStatus.SC_OK) {
            try {
                resource.setUrl(httpMethod.getURI().toString());

                for (Header header : httpMethod.getResponseHeaders()) {
                    resource.addResponseHeader(new ResponseHeader(header.getName(), header.getValue()));
                }
                resource.setHttpStatusCode(httpMethod.getStatusCode());
            } catch (Exception e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }

            return;
        }

        try {
            if (httpMethod.getResponseHeader("Content-Encoding") != null && httpMethod.getResponseHeader("Content-Encoding").getValue().contains("gzip")) {
                BufferedInputStream in = new BufferedInputStream(new GZIPInputStream(httpMethod.getResponseBodyAsStream()));
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, length);
                }

                resource.setRawContent(out.toByteArray());
                resource.setSize(new Long(out.toByteArray().length));
            } else {
                BufferedInputStream in = new BufferedInputStream(httpMethod.getResponseBodyAsStream());
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, length);
                }

                resource.setRawContent(out.toByteArray());
                resource.setSize(new Long(out.toByteArray().length));
            }

            resource.setHttpStatusCode(httpMethod.getStatusCode());
            resource.setLoadSucceed(true);

            resource.setMimeType(recognizeMimeType(resource, httpMethod));
            if(resource.getMimeType().startsWith("text") || resource.getMimeType().equals("application/json")) {

                resource.setContent(EncodingHelper.encode(resource.getRawContent(),
                       resource.getEncoding(),
                       ((HttpMethodBase) httpMethod).getResponseCharSet()));
            } else {
                resource.setIsBinary(true);
            }
            
        } catch (IOException e) {
        	e.printStackTrace();
            logger.log(Level.SEVERE, "Unable to load resource", e);
        } finally {
        	httpMethod.releaseConnection();	
        }
    }


    //TODO Make plugin resources available
    /*
    public void loadPluginResource(Resource resource)
     {
        try {
            GetMethod method = httpClientHelper.executeGetMethod(resource.getURL().toString());
            GroovyClassLoader gcl = new GroovyClassLoader();
            Class modifierClass = gcl.parseClass(method.getResponseBodyAsString());
            ResourceLoaderPlugin rlp = (ResourceLoaderPlugin) modifierClass.newInstance();
            method.releaseConnection();

            String parameters = null;

            if (resource.getURL().toString().contains("?")) {
                parameters = resource.getURL().toString().substring(resource.getURL().toString().indexOf("?"));
            }

            byte[] response = rlp.getRawContent(parameters);

            resource.setRawContent(response);

             resource.setLoadSucceed(true);
             resource.setHttpStatusCode(rlp.getStatusCode());

            resource.setBinary(rlp.getBinary());
            resource.setMimeType(rlp.getMimeType());

            if (!rlp.getBinary()) {
                resource.setEvaluatedContent(new String(response));
            }

        } catch (Exception e) {
            resource.setLoadSucceed(false);
            logger.log(Level.SEVERE, "Unable to load PLUGIN resource", e);
            e.printStackTrace();
        }
     }
     */

    //TODO Ouw my god, this mime type checking is a desaster....
    private String recognizeMimeType(Resource resource, HttpMethod httpMethod) {
        String headerMimeType = null;
        try {
            headerMimeType = httpMethod.getResponseHeader("Content-Type").getValue().split(";")[0];
        } catch(Exception e) {
            logger.fine("No content type header found.");
        }

        //TODO Ouw my god, this mime type checking is a desaster....
        if(headerMimeType == null || (!headerMimeType.startsWith("text") && !isBinaryFile(resource.getRawContent()))) {
            logger.warning("no content-type found try guessing with file extension");
            String urlString = resource.getUrl();
            if (urlString.endsWith(".htm") ||
                urlString.endsWith(".html")) {
                logger.fine("resource seems to be html");
                headerMimeType = "text/html";
            } else if (urlString.endsWith(".xml")) {
                logger.fine("resource seems to be html");
                headerMimeType = "text/xml";
            } else if (urlString.endsWith(".csv")) {
                logger.fine("resource seems to be html");
                headerMimeType = "text/csv";
            } else if (urlString.endsWith(".jpg") ||
                       urlString.endsWith(".jpeg")) {
                logger.fine("resource seem to be jpeg");
                headerMimeType = "image/jpeg";
            } else if (urlString.endsWith(".gif")) {
                logger.fine("resource seem to be gif");
                headerMimeType = "image/gif";
            } else if (urlString.endsWith(".png")) {
                logger.fine("resource seem to be png");
                headerMimeType = "image/png";
            } else {
                logger.severe("unable to determine content-type; use application/octet-stream instead");
                headerMimeType = "application/octet-stream";                
            }
        }

        // check if file is an image
        ImageInfo imageInfo = new ImageInfo();

        imageInfo.setInput(new ByteArrayInputStream(resource.getRawContent()));
        if(imageInfo.check()) {
            // image found
            if(!imageInfo.getMimeType().equals(headerMimeType)) {
                logger.warning("Recognized image type of resource: " + resource.getUrl() +
                               "does not match header mime type. using " + imageInfo.getMimeType() +
                               " instead.");
            }
            headerMimeType = imageInfo.getMimeType();
        } else {
            if(headerMimeType.startsWith("image")) {
                logger.severe("file extension recognized as image, but content was not.");
                headerMimeType = "application/octet-stream";
            }
        }
        return headerMimeType;
    }

    //TODO Ouw my god, this mime type checking is a desaster....
    public static boolean isBinaryFile( byte[] data )  {

        int ascii = 0;
        int other = 0;

        for( int i = 0; i < data.length; i++ ) {
            byte b = data[i];
            if( b < 0x09 ) return true;

            if( b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D ) ascii++;
            else if( (b >= 0x20)  &&  (b <= 0x7E) ) ascii++;
            else other++;
        }

        if( other == 0 ) return false;

        return 100 * other / (ascii + other) > 95;
    }
}
