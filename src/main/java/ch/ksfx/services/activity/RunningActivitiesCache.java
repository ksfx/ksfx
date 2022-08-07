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

package ch.ksfx.services.activity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kejo on 15.12.2014.
 */
public class RunningActivitiesCache
{
    public static Map<Long, ActivityInstanceRun> runningActivities = new HashMap<Long, ActivityInstanceRun>();
}