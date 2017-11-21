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

import ch.ksfx.model.PublishingStrategy
import ch.ksfx.model.publishing.PublishingSharedData
import ch.ksfx.util.PublishingDataShare
import org.apache.tapestry5.StreamResponse
import org.apache.tapestry5.ioc.ObjectLocator
import org.apache.tapestry5.util.TextStreamResponse

class DemoPublishingResource implements PublishingStrategy
{
    private ObjectLocator objectLocator;

    public DemoPublishingResource(ObjectLocator objectLocator)
    {
    	PublishingSharedData psd = new PublishingSharedData();
		psd.setTextData("Hello World Data");
		PublishingDataShare.addData("HelloWorldKey",psd);
		
		PublishingSharedData psdFromStore = PublishingDataShare.getData("HelloWorldKey");
    	
        this.objectLocator = objectLocator;
    }

    public StreamResponse getPublishingData()
    {
		return new TextStreamResponse("text/plain", "UTF-8","Hello World");
    }
}
