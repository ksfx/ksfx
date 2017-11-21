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

package ch.ksfx.util.feedback;

import java.util.ArrayList;
import java.util.List;


public class FeedbackManager
{
    private List<FeedbackMessage> messages;

    public FeedbackManager()
    {
        clear();
    }

    public void add(FeedbackMessage message)
    {
        messages.add(message);
    }

    public void add(String message, FeedbackType type)
    {
        add(new FeedbackMessage(message, type));
    }

    public void add(String message, String comment, FeedbackType type)
    {
        add(new FeedbackMessage(message, comment, type));
    }

    public List<FeedbackMessage> getMessages()
    {
        List<FeedbackMessage> msgs = new ArrayList<FeedbackMessage>();
        for (FeedbackMessage m : messages) {
            msgs.add(m);
        }
        clear();
        return msgs;
    }

    public boolean hasMessages()
    {
        return messages.isEmpty() == false;
    }

    public void clear()
    {
        messages = new ArrayList<FeedbackMessage>();
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
}
