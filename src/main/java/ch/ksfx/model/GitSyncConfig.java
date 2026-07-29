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

package ch.ksfx.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Per-instance configuration of the private Git repository that holds Activity and CodeLib
 * source code. There is exactly one row per KSFX instance (its own DB), each instance can point
 * at a different private repository. Lives in the top-level model package (not under
 * ch.ksfx.model.activity) since it covers more than just Activities.
 */
@Entity
@Table(name = "git_sync_config")
public class GitSyncConfig
{
    private Long id;
    private String repoUrl;
    private String branch = "master";
    private String accessToken;
    private String localClonePath;
    private String lastSyncedCommit;
    private Date lastSyncedAt;
    private boolean enabled = false;

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

    public String getRepoUrl()
    {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl)
    {
        this.repoUrl = repoUrl;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    @Lob
    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public String getLocalClonePath()
    {
        return localClonePath;
    }

    public void setLocalClonePath(String localClonePath)
    {
        this.localClonePath = localClonePath;
    }

    public String getLastSyncedCommit()
    {
        return lastSyncedCommit;
    }

    public void setLastSyncedCommit(String lastSyncedCommit)
    {
        this.lastSyncedCommit = lastSyncedCommit;
    }

    public Date getLastSyncedAt()
    {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Date lastSyncedAt)
    {
        this.lastSyncedAt = lastSyncedAt;
    }

    public boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
