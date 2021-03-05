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

package ch.ksfx.services.spidering;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class simulates the syntax of PCRE library
 */
public class PcreRegExSyntax {
    private static Logger logger = Logger.getLogger("");

    public static Pattern convertPcrePattern(String pattern) {
        char delimiter = pattern.charAt(0);
        if(Character.isLetterOrDigit(delimiter) || delimiter == '\\') {
            logger.severe("Invalid pattern: " + pattern);
            return null;
        }
        String patternContent = pattern.substring(1, pattern.lastIndexOf(delimiter));
        patternContent = patternContent.replace("\\" + delimiter, String.valueOf(delimiter));
        String patternFlags = pattern.substring(pattern.lastIndexOf(delimiter) + 1);
        int flags = 0;
        if(patternFlags.length() > 0) {
            for(char patternModifier : patternFlags.toCharArray()) {
                if(patternModifier == 'i') {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                if(patternModifier == 'm') {
                    flags |= Pattern.MULTILINE;
                }
                if(patternModifier == 's') {
                    flags |= Pattern.DOTALL;
                }
                if(patternModifier == 'u') {
                    flags |= Pattern.UNICODE_CASE;
                }
                if(patternModifier == 'x') {
                    flags |= Pattern.COMMENTS;
                }
            }
        }
        return Pattern.compile(patternContent, flags);
    }
}
