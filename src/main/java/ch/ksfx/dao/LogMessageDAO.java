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

package ch.ksfx.dao;

import ch.ksfx.model.logger.LogMessage;
import ch.ksfx.model.spidering.ResultVerifierConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface LogMessageDAO
{
    public void cleanupLogMessages();
    public List<String> getLogMessageTags();
    public List<LogMessage> getLogMessagesForTag(String tag);
    public List<LogMessage> getAllLogMessages();
    public void saveOrUpdateLogMessage(LogMessage logMessage);
    public void clearLogMessages();
//    public GridDataSource getDataSourceForTag(String tag);
    public Page<LogMessage> getLogMessagesForPageableAndTag(Pageable pageable, String tag);
}
