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

package ch.ksfx.dao.ebean;

import ch.ksfx.dao.LogMessageDAO;
import ch.ksfx.model.logger.LogMessage;
import ch.ksfx.model.spidering.ResultVerifierConfiguration;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import io.ebean.SqlRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Repository
public class EbeanLogMessageDAO implements LogMessageDAO
{
    @Override
    public void cleanupLogMessages()
    {
        String sql =    " DELETE FROM log_message " +
                        " WHERE id NOT IN (" +
                        " SELECT id" +
                        " FROM (" +
                        " SELECT id" +
                        " FROM log_message" +
                        " ORDER BY id DESC" +
                        " LIMIT 50000" +
                        " ) foo" +
                        ")";

        Ebean.createSqlUpdate(sql).execute();
    }

    @Override
    public List<String> getLogMessageTags()
    {
        List<String> tags = new ArrayList<String>();
        List<SqlRow> rows = Ebean.createSqlQuery("SELECT DISTINCT tag FROM log_message").findList();

        for (SqlRow r : rows) {
            tags.add(r.getString("tag"));
        }

        return tags;
    }

    @Override
    public List<LogMessage> getLogMessagesForTag(String tag)
    {
        return Ebean.find(LogMessage.class).where().eq("tag", tag).findList();
    }

    @Override
    public List<LogMessage> getAllLogMessages()
    {
        return Ebean.find(LogMessage.class).findList();
    }

    @Override
    public void saveOrUpdateLogMessage(LogMessage logMessage)
    {
        if (logMessage.getId() != null) {
            Ebean.update(logMessage);
        } else {
            Ebean.save(logMessage);
        }
    }

    @Override
    public void clearLogMessages()
    {
        Ebean.createSqlUpdate("DELETE FROM log_message").execute();
    }

    @Override
    public Page<LogMessage> getLogMessagesForPageableAndTag(Pageable pageable, String tag)
    {
        ExpressionList expressionList = Ebean.find(LogMessage.class).where();

        expressionList.setFirstRow(new Long(pageable.getOffset()).intValue());
        expressionList.setMaxRows(pageable.getPageSize());

        if (tag != null && !tag.isEmpty()) {
            expressionList.eq("tag", tag);
        }

        boolean hasOrder = false;

        if (!pageable.getSort().isUnsorted()) {
            Iterator<Sort.Order> orderIterator = pageable.getSort().iterator();
            while (orderIterator.hasNext()) {
                Sort.Order order = orderIterator.next();

                if (!order.getProperty().equals("UNSORTED")) {
                    if (order.isAscending()) {
                        expressionList.order().asc(order.getProperty());
                        hasOrder = true;
                    }

                    if (order.isDescending()) {
                        expressionList.order().desc(order.getProperty());
                        hasOrder = true;
                    }
                }
            }
        }

        if (hasOrder == false) {
            expressionList.order().desc("date");
        }

        Page<LogMessage> page = new PageImpl<LogMessage>(expressionList.findList(), pageable, expressionList.findCount());

        return page;
    }

    /*
    @Override
    public GridDataSource getDataSourceForTag(String tag)
    {
        ExpressionList expressionList = Ebean.find(LogMessage.class).where();

        if (tag != null && !tag.isEmpty()) {
            expressionList.eq("tag", tag);
        }

        return new EbeanGridDataSource(expressionList, LogMessage.class);
    }
    */
}
