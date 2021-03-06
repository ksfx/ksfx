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

package ch.ksfx.model.activity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Kejo on 26.03.2015.
 */
@Entity
@Table(name = "activity_queue_activity_instance")
public class ActivityQueueActivityInstance
{
    private Long id;
    private ActivityInstance activityInstance;
    private ActivityQueue activityQueue;
    private Date queued;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "activity_instance")
    public ActivityInstance getActivityInstance()
    {
        return activityInstance;
    }

    public void setActivityInstance(ActivityInstance activityInstance)
    {
        this.activityInstance = activityInstance;
    }

    @ManyToOne
    @JoinColumn(name = "activity_queue")
    public ActivityQueue getActivityQueue()
    {
        return activityQueue;
    }

    public void setActivityQueue(ActivityQueue activityQueue)
    {
        this.activityQueue = activityQueue;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getQueued()
    {
        return queued;
    }

    public void setQueued(Date queued)
    {
        this.queued = queued;
    }
}
