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

package ch.ksfx.configuration;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;


public class SetupTest
{
    public static String testDatabaseConnectionString;
    public static String testDatabaseName;
    public static Boolean testDatabaseRecreate;
    public static String testDatabaseUser;
    public static String testDatabasePassword;

    @BeforeSuite
    @Parameters({"testDatabaseConnectionString","testDatabaseName","testDatabaseRecreate","testDatabaseDefaultUser","testDatabaseDefaultPassword"})
    public void prepareTestSuit(String testDatabaseConnectionString, String testDatabaseName, String testDatabaseRecreate, String testDatabaseDefaultUser, String testDatabaseDefaultPassword)
    {
        SetupTest.testDatabaseConnectionString = testDatabaseConnectionString;
        SetupTest.testDatabaseName = testDatabaseName;
        SetupTest.testDatabaseRecreate = Boolean.parseBoolean(testDatabaseRecreate);

        SetupTest.testDatabaseUser = System.getProperty("testDatabaseUser", testDatabaseDefaultUser);
        SetupTest.testDatabasePassword = System.getProperty("testDatabasePassword", testDatabaseDefaultPassword);
    }
}
