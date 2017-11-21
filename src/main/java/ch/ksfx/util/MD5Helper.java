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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Helper {

    /**
     * Returns CRC32 hexadecimal hash of given byte array
     *
     * @param bytes given byte array
     * @return CRC32 hexadecimal hash as String
     */
    public static String md5HexHash(final byte[] bytes) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return toHexString(md5.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            // never thrown
        }
        return null;
    }

    /**
     * Returns CRC32 hexadecimal hash of given String
     *
     * @param string given String
     * @return CRC32 hexadecimal hash as String
     */
    public static String md5HexHash(final String string) {
        return MD5Helper.md5HexHash(string.getBytes());
    }

    private static String toHexString(byte bytes[]) {
        StringBuffer retString = new StringBuffer();
        for(byte aByte : bytes) {
            retString.append(Integer.toHexString(0x0100 + (aByte & 0x00FF)).substring(1));
        }
        return retString.toString();
    }
}
