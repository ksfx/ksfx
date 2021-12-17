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

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.dao.ebean.EbeanPublishingConfigurationDAO;
import ch.ksfx.dao.ebean.activity.EbeanActivityInstanceDAO;
import ch.ksfx.model.publishing.PublishingConfiguration;
import ch.ksfx.model.activity.ActivityInstance;


public class Console
{
        public static final Integer CONSOLE_LIMIT = 40000;

        private static final ThreadLocal<Long> publishingConfigurationId = new ThreadLocal<Long>();
        private static final ThreadLocal<Long> activityInstanceId = new ThreadLocal<Long>();
       
        public static void startConsole(PublishingConfiguration publishingConfiguration)
        {
                publishingConfigurationId.set(publishingConfiguration.getId());
        }
       
        public static void startConsole(ActivityInstance activityInstance)
        {
                activityInstanceId.set(activityInstance.getId());
        }
       
        public static void write(String data)
        {
        	System.out.println("Writing data " + data + " for activityInstanceId: " + activityInstanceId.get()+ " for publishingConfigurationId: " + publishingConfigurationId.get());
        	
                if (publishingConfigurationId.get() != null) {
                        //Publication shared data table with Key value???
					PublishingConfigurationDAO publishingConfigurationDAO = new EbeanPublishingConfigurationDAO();
                
                	publishingConfigurationDAO.appendConsole(publishingConfigurationId.get(), data);
                } else if (activityInstanceId.get() != null) {
					ActivityInstanceDAO activityInstanceDAO = new EbeanActivityInstanceDAO();
					
					activityInstanceDAO.appendConsole(activityInstanceId.get(), data);
                } else {
                        System.out.println(data);
                }
        }
       
        public static void writeln(String data)
        {
                write(data);
                write(System.getProperty("line.separator"));
        }
       
        public static void endConsole()
        {
                publishingConfigurationId.remove();
                activityInstanceId.remove();
        }
}