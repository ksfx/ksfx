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

import ch.ksfx.model.publishing.PublishingStrategy
import ch.ksfx.model.publishing.PublishingSharedData
import ch.ksfx.services.ServiceProvider
import ch.ksfx.util.Console
import ch.ksfx.util.GenericResponse
import ch.ksfx.util.PublishingDataShare

class DemoPublishingStrategy implements PublishingStrategy
{
    private ServiceProvider serviceProvider;

    public DemoPublishingStrategy(ServiceProvider serviceProvider)
    {
        this.serviceProvider = serviceProvider;
    }

    public GenericResponse getPublishingData()
    {
    	Console.write("Hello World Publishing Configuration");
    	
    	PublishingSharedData psd = new PublishingSharedData();
		psd.setTextData("Hello World Data");
		PublishingDataShare.addData("HelloWorldKey",psd);
		
		PublishingSharedData psdFromStore = PublishingDataShare.getData("HelloWorldKey");
    	
		return new GenericResponse("Hello World".getBytes("UTF-8"), "", "text/plain", false);
    }
}