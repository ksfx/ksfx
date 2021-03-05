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

import org.apache.commons.httpclient.Header;

import java.util.ArrayList;

/**
 * @author mvw
 * @version 1.0
 * @since 1.0
 */
public class HttpUserAgentHeaders {
    private ArrayList<Header> headers;

    public HttpUserAgentHeaders() {
        this.headers = new ArrayList<Header>();
    }

    public void addHeader(String headerName, String headerValue) {
        this.headers.add(new Header(headerName, headerValue));
    }

    public void addHeader(Header header) {
        this.headers.add(header);
    }

    public ArrayList<Header> getHeaders() {
        return this.headers;
    }

    public static HttpUserAgentHeaders getDefaultHeaders() {
        HttpUserAgentHeaders headers = new HttpUserAgentHeaders();
        headers.addHeader(new Header("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)"));
        headers.addHeader(new Header("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, */*"));
        headers.addHeader(new Header("Accept-Language", "de-ch"));
        headers.addHeader(new Header("Accept-Encoding", "gzip, deflate"));
        headers.addHeader(new Header("Connection", "Keep-Alive"));
        return headers;
    }
}
