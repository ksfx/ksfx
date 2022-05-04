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

import java.io.IOException;


public class GenericResponse
{
    private byte[] byteData;
    private String contentType;
    private String fileName;
    private boolean isDownload;

    public GenericResponse(byte[] byteData, final String fileName, final String contentType, final boolean isDownload)
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

    public byte[] getResponse() throws IOException
    {
        return byteData;
    }

    public String getFileName() throws IOException
    {
        return fileName;
    }
}
