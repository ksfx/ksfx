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

package ch.ksfx.web.pages.admin.systemlog;

import ch.ksfx.dao.LogMessageDAO;
import ch.ksfx.model.logger.LogMessage;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;

import java.text.SimpleDateFormat;
import java.util.List;


@Secured({"ROLE_ADMIN"})
public class SystemLogIndex
{
    @Inject
    private LogMessageDAO logMessageDAO;

    @Property
    @Persist
    private String tag;

    @Property
    private LogMessage logMessage;

    @InjectComponent
    private Grid logMessagesGrid;

    @SetupRender
    public void onSetupRender()
    {
        if (getAllPossibleTags() != null && getAllPossibleTags().size() > 0) {
            if (logMessagesGrid.getSortModel().getSortConstraints().isEmpty()) {
                logMessagesGrid.getSortModel().updateSort("id"); //was date

                ColumnSort colSort = logMessagesGrid.getSortModel().getColumnSort("id");

                if (!colSort.equals(ColumnSort.DESCENDING)) {
                     logMessagesGrid.getSortModel().updateSort("id");
                }
            }
        }
    }

    public List<String> getAllPossibleTags()
    {
        return logMessageDAO.getLogMessageTags();
    }

    public GridDataSource getLogMessages()
    {
        return logMessageDAO.getDataSourceForTag(tag);
    }

    public String getFormattedDate()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");

        if (logMessage != null && logMessage.getDate() != null) {
            return sdf.format(logMessage.getDate());
        }

        return "";
    }

    public void onActionFromClearLogs()
    {
        logMessageDAO.clearLogMessages();
    }
}
