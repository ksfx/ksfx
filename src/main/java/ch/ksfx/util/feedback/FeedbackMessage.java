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


public class FeedbackMessage
{
    private String message;
    private String comment;
    private FeedbackType type = FeedbackType.INFO;

    public FeedbackMessage(String message)
    {
        super();
        this.message = message;
    }

    public FeedbackMessage(String message, FeedbackType type)
    {
        super();
        this.message = message;
        this.type = type;
    }

    public FeedbackMessage(String message, String comment, FeedbackType type)
    {
        super();
        this.message = message;
        this.comment = comment;
        this.type = type;
    }

    public String toString()
    {
        return getMessage();
    }

    public String getMessage()
    {
        return message;
    }

    public String getComment()
    {
        return comment;
    }

    public FeedbackType getType()
    {
        return type;
    }
}
