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

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class helps to encode byte arrays
 *
 * @author mvw
 * @version 1.0
 * @since 1.0
 */
public class EncodingHelper {
    /** default character encoding (ISO-8859-1) */
    public static final String DEFAULT_CHARACTER_ENCODING = "windows-1252";

    private static Pattern characterEncodingPattern = Pattern.compile("charset=([.[^; ]]*)\"");
    private static CharsetDetector charsetDetector = new CharsetDetector();
    private static final int charsetDetectorBufferSize = 8000;

    private static Logger logger = Logger.getLogger("");

    /**
     * This method tries to encode the given byte array with the following rules:
     * <ol>
     *  <li>Encoding with the given force character set parameter</li>
     *  <li>Encoding with the given HTTP Header character set</li>
     *  <li>Detection of character set in HTTP-EQUIV META tag</li>
     *  <li>Detection of character set with heuristic methods (guessing)</li>
     *  <li>Encoding with the default character set (ISO-8859-1)</li>
     * </ol>
     *
     * @param data data to encode
     * @param forceCharset character set to encode first
     * @param httpHeaderCharset http
     * @return encoded data as String
     */
    public static String encode(byte[] data, String forceCharset, String httpHeaderCharset) {
        try {
            if(forceCharset == null) {
                throw new UnsupportedEncodingException();
            }
            if(forceCharset.toLowerCase().equals("iso-8859-1")) {
                logger.info("ISO-8859-1 forced, usind windows-1252 instead");
                forceCharset = DEFAULT_CHARACTER_ENCODING;
            }
            return new String(data, forceCharset);
        } catch(UnsupportedEncodingException e1) {
            try {                
                if(httpHeaderCharset == null) {
                    throw new UnsupportedEncodingException();
                }
                return new String(data, httpHeaderCharset);
            } catch(UnsupportedEncodingException e2) {
                try {
                    return encodeDataWithCharsetInMetaTag(data);
                } catch(UnsupportedEncodingException e3) {
                    try {
                        return encodeDataWithCharsetGuessing(data);
                    } catch(UnsupportedEncodingException e4) {
                        try {
                            return new String(data, DEFAULT_CHARACTER_ENCODING);
                        } catch(UnsupportedEncodingException e5) {
                            logger.severe("windows-1252 charset not supported, using latin-1 instead");
                            try {
                                return new String(data, "ISO-8859-1");
                            } catch (UnsupportedEncodingException e) {
                                logger.severe("ISO-8859-1 charset not supported");
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String encodeDataWithCharsetInMetaTag(byte[] data) throws UnsupportedEncodingException {
        Matcher m = characterEncodingPattern.matcher(new String(data, "ISO-8859-1"));
		if (m.find()) {
            String foundCharset = m.group(1);
            if(foundCharset.toLowerCase().equals("iso-8859-1")) {
                logger.info("ISO-8859-1 found in meta tag, usind windows-1252 instead");
                foundCharset = DEFAULT_CHARACTER_ENCODING;
            }
            return new String(data, foundCharset);
		} else {
            throw new UnsupportedEncodingException();
        }
    }

    private static String encodeDataWithCharsetGuessing(byte[] data) throws UnsupportedEncodingException {
        try {
            byte[] testData;
            if(data.length > charsetDetectorBufferSize) {
                testData = new byte[charsetDetectorBufferSize];
                System.arraycopy(data, 0, testData, 0, charsetDetectorBufferSize);
            } else {
                testData = data;
            }
            charsetDetector.setText(testData);
            charsetDetector.enableInputFilter(true);
            CharsetMatch charsetMatch = charsetDetector.detect();
            if(charsetMatch.getConfidence() < 40) {
                throw new UnsupportedEncodingException();
            }
            String foundCharset = charsetMatch.getName();
            if(foundCharset.toLowerCase().equals("iso-8859-1")) {
                logger.info("ISO-8859-1 guessed, usind windows-1252 instead");
                foundCharset = DEFAULT_CHARACTER_ENCODING;
            }
            return new String(data, foundCharset);
        } catch(IOException e) {
            throw new UnsupportedEncodingException();
        }
    }
}
