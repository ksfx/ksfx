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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatUtil
{
	static String iso8601TimeAndDatePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	static String lexicographicallySortableTimeAndDatePattern = "yyyyMMddHHmmss";
    static String simpleTimeAndDatePattern = "yyyy-MM-dd'T'HH:mm:ss";
    static String simpleDatePattern = "yyyy-MM-dd";
    static String simpleGermanDatePattern = "dd.MM.yyyy";
	
	public static String formatToISO8601TimeAndDateString(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(iso8601TimeAndDatePattern);
		return sdf.format(date);
	}
	
	public static Date parseISO8601TimeAndDateString(String dateString)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(iso8601TimeAndDatePattern);
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public static String formatToLexicographicallySortableTimeAndDateString(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(lexicographicallySortableTimeAndDatePattern);
		return sdf.format(date);
	}
	
	public static Date parseLexicographicallySortableTimeAndDateString(String dateString)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(lexicographicallySortableTimeAndDatePattern);
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String formatSimpleTimeAndDateString(Date date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleTimeAndDatePattern);
        return sdf.format(date);
    }

    public static Date parseSimpleTimeAndDateString(String dateString)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleTimeAndDatePattern);
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String formatSimpleDateString(Date date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleDatePattern);
        return sdf.format(date);
    }

    public static Date parseSimpleDateString(String dateString)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleDatePattern);
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String formatSimpleGermanDateString(Date date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleGermanDatePattern);
        return sdf.format(date);
    }

    public static Date parseSimpleGermanDateString(String dateString)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleGermanDatePattern);
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}