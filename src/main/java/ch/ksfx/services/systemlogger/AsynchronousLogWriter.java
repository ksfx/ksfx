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

package ch.ksfx.services.systemlogger;

import ch.ksfx.dao.LogMessageDAO;
import ch.ksfx.dao.ebean.EbeanLogMessageDAO;
import ch.ksfx.model.logger.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class AsynchronousLogWriter extends Thread
{
    private BlockingQueue<LogMessage> queuedMessages;

    private Logger logger = LoggerFactory.getLogger(AsynchronousLogWriter.class);

    public AsynchronousLogWriter()
    {
        queuedMessages = new LinkedBlockingQueue<LogMessage>();
    }

    public void run()
    {
        while (true) {
            try {
                LogMessage logMessage = queuedMessages.take();

                LogMessageDAO logMessageDAO = new EbeanLogMessageDAO();

                logMessageDAO.saveOrUpdateLogMessage(logMessage);
            } catch (InterruptedException e) {
                logger.error("Error in Asynchronous Log Writer", e);
            }
        }
    }

    public BlockingQueue<LogMessage> getQueuedMessages()
    {
        return queuedMessages;
    }

    public void setQueuedMessages(BlockingQueue<LogMessage> queuedMessages)
    {
        this.queuedMessages = queuedMessages;
    }
}
