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
import java.util.List;

/**
 * Created by Kejo on 27.02.2015.
 */
@Entity
@Table(name = "activity_instance")
public class ActivityInstance
{
    private Long id;
    private Date started;
    private Date finished;
    private boolean approved = false;
    private Activity activity;
    private List<ActivityInstancePersistentData> activityInstancePersistentDatas;
    private List<ActivityInstanceParameter> activityInstanceParameters;
    private String console;

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

    @Temporal(TemporalType.TIMESTAMP)
    public Date getStarted()
    {
        return started;
    }

    public void setStarted(Date started)
    {
        this.started = started;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getFinished()
    {
        return finished;
    }

    public void setFinished(Date finished)
    {
        this.finished = finished;
    }

    @ManyToOne
    @JoinColumn(name = "activity")
    public Activity getActivity()
    {
        return activity;
    }

    public void setActivity(Activity activity)
    {
        this.activity = activity;
    }

    @OneToMany(mappedBy = "activityInstance")
    public List<ActivityInstancePersistentData> getActivityInstancePersistentDatas()
    {
        return activityInstancePersistentDatas;
    }

    public void setActivityInstancePersistentDatas(List<ActivityInstancePersistentData> activityInstancePersistentDatas)
    {
        this.activityInstancePersistentDatas = activityInstancePersistentDatas;
    }

    @OneToMany(mappedBy = "activityInstance")
    public List<ActivityInstanceParameter> getActivityInstanceParameters()
    {
        return activityInstanceParameters;
    }

    public void setActivityInstanceParameters(List<ActivityInstanceParameter> activityInstanceParameters)
    {
        this.activityInstanceParameters = activityInstanceParameters;
    }

    public boolean getApproved()
    {
        return approved;
    }

    public void setApproved(boolean approved)
    {
        this.approved = approved;
    }
    
    public String getConsole()
    {
    	return console;
    }
    
    public void setConsole(String console)
    {
    	this.console = console;
    }
}
