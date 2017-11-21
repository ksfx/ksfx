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

package ch.ksfx.web.components;

import ch.ksfx.util.feedback.FeedbackManager;
import ch.ksfx.util.feedback.FeedbackMessage;
import ch.ksfx.util.feedback.FeedbackType;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class Feedback
{
    @SessionState
    private FeedbackManager manager;
    @Property
    private FeedbackMessage message;
    private final Logger logger = LoggerFactory.getLogger(Feedback.class);

    public void add(FeedbackMessage message)
    {
        logger.info("[" + message.getType() + "] " + message.getMessage());
        manager.add(message);
    }

    public void add(String message, FeedbackType type)
    {
        add(new FeedbackMessage(message, type));
    }

    public void add(String message, String comment, FeedbackType type)
    {
        add(new FeedbackMessage(message, comment, type));
    }

    public void success(String message)
    {
        add(message, FeedbackType.SUCCESS);
    }

    public void success(String message, String comment)
    {
        add(message, comment, FeedbackType.SUCCESS);
    }

    public void warning(String message)
    {
        add(message, FeedbackType.WARNING);
    }

    public void warning(String message, String comment)
    {
        add(message, comment, FeedbackType.WARNING);
    }

    public void error(String message)
    {
        add(message, FeedbackType.ERROR);
    }

    public void error(String message, String comment)
    {
        add(message, comment, FeedbackType.ERROR);
    }

    public void info(String message)
    {
        add(message, FeedbackType.INFO);
    }

    public void info(String message, String comment)
    {
        add(message, comment, FeedbackType.INFO);
    }

    public List<FeedbackMessage> getMessages()
    {
        return manager.getMessages();
    }

    public boolean isEmpty()
    {
        return manager.hasMessages() == false;
    }

    public String getCssClass()
    {
        if (message == null) {
            throw new IllegalStateException("Not inside messages loop");
        }
        return "feedback-" + message.getType().toString().toLowerCase();
    }
}
