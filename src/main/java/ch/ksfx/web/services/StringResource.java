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

package ch.ksfx.web.services;

import org.apache.tapestry5.ioc.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;


public class StringResource implements Resource {

    private String tml;
    private URL cacheKey;

    /**
     * @param tml - template to be rendered
     * @param lastModificationTime - parameter to watch, to reset cache it changes
     */
    public StringResource(String tml, Date lastModificationTime) {
        this.tml = tml;
        try {
            this.cacheKey = new URL("http://dynamic/" + lastModificationTime.getTime());
        } catch (MalformedURLException e) {
            this.cacheKey = null;
        }
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public InputStream openStream() throws IOException {
        return new ByteArrayInputStream(tml.getBytes("UTF-8"));
    }

    @Override
    public URL toURL() {
        return cacheKey;
    }

    @Override
    public Resource forLocale(Locale locale) {
        return null;
    }

    @Override
    public Resource forFile(String relativePath) {
        return null;
    }

    @Override
    public Resource withExtension(String extension) {
        return null;
    }

    @Override
    public String getFolder() {
        return null;
    }

    @Override
    public String getFile() {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }
}
