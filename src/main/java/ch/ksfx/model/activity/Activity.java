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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Created by Kejo on 22.02.2015.
 */
@Entity
@Table(name = "activity")
public class Activity
{
    private Long id;

    @NotNull
    @Size(min=2, max=30)
    @NotEmpty
    private String name;
    private ActivityCategory activityCategory;
    private String cronSchedule;
    private boolean cronScheduleEnabled = false;
    private String groovyCode;

    @Deprecated
    private boolean requiresApproval = false;
    private List<RequiredActivity> requiredActivities;
    private List<TriggerActivity> triggerActivities;
    private ActivityApprovalStrategy activityApprovalStrategy;

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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn(name = "activity_category")
    public ActivityCategory getActivityCategory()
    {
        return activityCategory;
    }

    public void setActivityCategory(ActivityCategory activityCategory)
    {
        this.activityCategory = activityCategory;
    }

    public String getCronSchedule()
    {
        return cronSchedule;
    }

    public void setCronSchedule(String cronSchedule)
    {
        this.cronSchedule = cronSchedule;
    }

    @Lob
    public String getGroovyCode()
    {
        return groovyCode;
    }

    public void setGroovyCode(String groovyCode)
    {
        this.groovyCode = groovyCode;
    }

    public boolean getRequiresApproval()
    {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval)
    {
        this.requiresApproval = requiresApproval;
    }

    public boolean getCronScheduleEnabled()
    {
        return cronScheduleEnabled;
    }

    public void setCronScheduleEnabled(boolean cronScheduleEnabled)
    {
        this.cronScheduleEnabled = cronScheduleEnabled;
    }

    @OneToMany(mappedBy = "activity")
    public List<RequiredActivity> getRequiredActivities()
    {
        return requiredActivities;
    }

    public void setRequiredActivities(List<RequiredActivity> requiredActivities)
    {
        this.requiredActivities = requiredActivities;
    }

    @OneToMany(mappedBy = "activity")
    public List<TriggerActivity> getTriggerActivities()
    {
        return triggerActivities;
    }

    public void setTriggerActivities(List<TriggerActivity> triggerActivities)
    {
        this.triggerActivities = triggerActivities;
    }

    @ManyToOne
    @JoinColumn(name = "activity_approval_strategy")
    public ActivityApprovalStrategy getActivityApprovalStrategy()
    {
        return this.activityApprovalStrategy;
    }

    public void setActivityApprovalStrategy(ActivityApprovalStrategy activityApprovalStrategy)
    {
        this.activityApprovalStrategy = activityApprovalStrategy;
    }
}
