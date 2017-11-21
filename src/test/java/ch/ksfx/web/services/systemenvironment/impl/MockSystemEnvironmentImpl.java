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

package ch.ksfx.web.services.systemenvironment.impl;

import java.io.File;


public class MockSystemEnvironmentImpl extends SystemEnvironmentImpl
{
    public MockSystemEnvironmentImpl()
    {
        super();
    }

    @Override
    public String getApplicationHomeDirectoryPath()
    {
        String tmpdir = System.getProperty("java.io.tmpdir") + "/ksfx";
        File file = new File(tmpdir);

        if (!file.exists()) {
            file.mkdirs();
        }

        return tmpdir;
    }
}
