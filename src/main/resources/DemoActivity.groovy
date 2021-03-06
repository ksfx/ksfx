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

import ch.ksfx.model.activity.ActivityExecution
import ch.ksfx.model.activity.ActivityInstanceParameter
import ch.ksfx.model.activity.ActivityInstancePersistentData
import ch.ksfx.util.Console
import org.apache.tapestry5.ioc.ObjectLocator

class DemoActivity implements ActivityExecution
{
    private ObjectLocator objectLocator;

    public DemoActivity(ObjectLocator objectLocator)
    {
        this.objectLocator = objectLocator;
    }

    public List<ActivityInstancePersistentData> executeActivity(List<ActivityInstanceParameter> activityInstanceParameters)
    {
        List<ActivityInstancePersistentData> persistentData = new ArrayList<ActivityInstancePersistentData>();

        Console.write("Hello World Activity");

        return persistentData;
    }

    @Override
    void terminateActivity()
    {

    }
}
