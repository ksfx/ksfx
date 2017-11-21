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
import org.jfree.chart.ChartUtilities;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class ImageStreamResponse implements StreamResponse
{
    private BufferedImage bufferedImage;
    private String contentType;

    public ImageStreamResponse(final String contentType, BufferedImage bufferedImage)
    {
        this.contentType = contentType;
        this.bufferedImage = bufferedImage;
    }

    public String getContentType()
    {
        return contentType;
    }

    public InputStream getStream() throws IOException
    {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream() ;
        ChartUtilities.encodeAsPNG(bufferedImage) ;

        return new ByteArrayInputStream(ChartUtilities.encodeAsPNG(bufferedImage));
    }

    @Override
    public void prepareResponse(Response response)
    {

    }
}
