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

package ch.ksfx.model.spidering;

import ch.ksfx.model.activity.Activity;

import javax.persistence.*;

/**
 * Created by Kejo on 09.04.2015.
 */
@Entity
@Table(name = "spidering_post_activity")
public class SpideringPostActivity
{
    public Long id;
    public SpideringConfiguration spideringConfiguration;
    public Activity activity;

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
    @JoinColumn(name = "spidering_configuration")
    public SpideringConfiguration getSpideringConfiguration()
    {
        return this.spideringConfiguration;
    }

    public void setSpideringConfiguration(SpideringConfiguration spideringConfiguration)
    {
        this.spideringConfiguration = spideringConfiguration;
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
}
