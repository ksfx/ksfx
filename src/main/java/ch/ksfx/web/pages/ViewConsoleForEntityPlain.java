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

package ch.ksfx.web.pages;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.dao.activity.ActivityInstanceDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.activity.ActivityInstance;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.List;


@Secured({"ROLE_ADMIN"})
public class ViewConsoleForEntityPlain
{
    @Inject
    private PublishingConfigurationDAO publishingConfigurationDAO;
    
    @Inject
    private ActivityInstanceDAO activityInstanceDAO;
    
    @Property
    private String console;

    @Property
    private Long entityId;

    @Property
    private String entityType;
	
	
	@Secured({"ROLE_ADMIN"})
	public void onActivate(Long entityId, String entityType)
	{
		this.entityId = entityId;
		this.entityType = entityType;
		
		if (entityType.equals("activityInstance")) {
			console = StringEscapeUtils.escapeHtml(activityInstanceDAO.getActivityInstanceForId(entityId).getConsole());
		} else if (entityType.equals("publishingConfiguration")) {
			console = StringEscapeUtils.escapeHtml(publishingConfigurationDAO.getPublishingConfigurationForId(entityId).getConsole());
		}
	}

    public void onActionFromClearConsole(Long entityId, String entityType)
    {
        if (entityType.equals("activityInstance")) {
            ActivityInstance ai = activityInstanceDAO.getActivityInstanceForId(entityId);
            ai.setConsole("");
            activityInstanceDAO.saveOrUpdateActivityInstance(ai);
        } else if (entityType.equals("publishingConfiguration")) {
            PublishingConfiguration pc = publishingConfigurationDAO.getPublishingConfigurationForId(entityId);
            pc.setConsole("");
            publishingConfigurationDAO.saveOrUpdatePublishingConfiguration(pc);
        }
    }
	
	public Object[] onPassivate()
	{
		List<Object> p = new ArrayList<Object>();
		p.add(entityId);
		p.add(entityType);
		
		return p.toArray(new Object[2]);
	}
}

