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

package ch.ksfx.web.services.logger;

import ch.ksfx.model.logger.LogMessage;
import ch.ksfx.util.StacktraceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;


public class SystemLogger
{
    private AsynchronousLogWriter asynchronousLogWriter;
    private Logger logger = LoggerFactory.getLogger(SystemLogger.class);

    public SystemLogger()
    {
        this.asynchronousLogWriter = new AsynchronousLogWriter();

        this.asynchronousLogWriter.start();
    }

    public void logMessage(String tag, String messages)
    {
        this.logMessage(tag, messages, null);
    }

    public void logMessage(String tag, String messages, Throwable aThrowable)
    {
        try {
            LogMessage logMessage = new LogMessage();
            logMessage.setDate(new Date());
            logMessage.setTag(tag);
            logMessage.setMessage(messages);

            if (aThrowable != null) {
                logMessage.setStackTrace(StacktraceUtil.getStackTrace(aThrowable));
            }

            asynchronousLogWriter.getQueuedMessages().add(logMessage);
        } catch (Exception e) {
            logger.error("Error in Systemlogger", e);
        }
    }
}
