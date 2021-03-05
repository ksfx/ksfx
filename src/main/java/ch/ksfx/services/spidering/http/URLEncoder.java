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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;

/**
 * This helper encodes special characters in URL and URI strings,
 * but leave characters with special meanings like ?, & and others
 * as they are
 *
 * @author mvw
 * @version 1.0
 * @since 1.0
 */
public class URLEncoder {
    private static URLEncoder urlEncoder;

    public static URLEncoder getURLEncoder() {
        if(urlEncoder == null) {
            urlEncoder = new URLEncoder();
        }
        return urlEncoder;
    }

    private BitSet safeAndReservedCharacters;

    private URLEncoder() {
        safeAndReservedCharacters = new BitSet(256);

        // alpha characters
        for (int i = 'a'; i <= 'z'; i++) {
            safeAndReservedCharacters.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            safeAndReservedCharacters.set(i);
        }
        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            safeAndReservedCharacters.set(i);
        }
        // special characters
        safeAndReservedCharacters.set('-');
        safeAndReservedCharacters.set('_');
        safeAndReservedCharacters.set('.');
        safeAndReservedCharacters.set('*');
        safeAndReservedCharacters.set('$');
        safeAndReservedCharacters.set('&');
        safeAndReservedCharacters.set('+');
        safeAndReservedCharacters.set(',');
        safeAndReservedCharacters.set('/');
        safeAndReservedCharacters.set(':');
        safeAndReservedCharacters.set(';');
        safeAndReservedCharacters.set('?');
        safeAndReservedCharacters.set('@');
        safeAndReservedCharacters.set('=');
        safeAndReservedCharacters.set('#');
        safeAndReservedCharacters.set('%');
    }

    /**
     * Encode a given url
     *
     * @param url url to encode
     * @return encoded url
     */
    public String encode(String url) {
        byte[] bytes = new byte[0];
        try {
            bytes = url.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // never thrown
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int b : bytes) {
            if (b < 0) {
                b = 256 + b;
            }
            if (safeAndReservedCharacters.get(b)) {
                buffer.write(b);
            } else {
                buffer.write('%');
                char hex1 = Character.toUpperCase(
                        Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(
                        Character.forDigit(b & 0xF, 16));
                buffer.write(hex1);
                buffer.write(hex2);
            }
        }
        return new String(buffer.toByteArray());
    }
}
