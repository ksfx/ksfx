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

package ch.ksfx.util;

import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class GenericStreamResponse implements StreamResponse
{
    private byte[] byteData;
    private String contentType;
    private String fileName;
    private boolean isDownload;

    public GenericStreamResponse(byte[] byteData, final String fileName, final String contentType, final boolean isDownload)
  	{
        this.contentType = contentType;
        this.byteData = byteData;
        this.fileName = fileName;
        this.isDownload = isDownload;
    }

    public String getContentType()
    {
        return contentType;
    }

    public InputStream getStream() throws IOException
    {
        return new ByteArrayInputStream(byteData);
    }

    @Override
    public void prepareResponse(Response response)
    {
    	if (isDownload) {
			response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
			response.setHeader("Expires", "0");
	        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
	        response.setHeader("Pragma", "public");
	        response.setContentLength(byteData.length);
    	}
    }
}
