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


@Entity
@Table(name = "required_activity")
public class RequiredActivity
{
    private Long id;
    private Activity activity;
    private Activity requiredActivity;

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
    @JoinColumn(name = "activity")
    public Activity getActivity()
    {
        return activity;
    }

    public void setActivity(Activity activity)
    {
        this.activity = activity;
    }

    @ManyToOne
    @JoinColumn(name = "required_activity")
    public Activity getRequiredActivity()
    {
        return requiredActivity;
    }

    public void setRequiredActivity(Activity requiredActivity)
    {
        this.requiredActivity = requiredActivity;
    }
}
