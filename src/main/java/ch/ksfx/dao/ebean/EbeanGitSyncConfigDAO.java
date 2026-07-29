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

package ch.ksfx.dao.ebean;

import ch.ksfx.dao.GitSyncConfigDAO;
import ch.ksfx.model.GitSyncConfig;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * There is exactly one {@link GitSyncConfig} row per KSFX instance, describing which private Git
 * repository this instance's Activity/CodeLib code is stored in.
 */
@Repository
public class EbeanGitSyncConfigDAO implements GitSyncConfigDAO
{
    @Override
    public GitSyncConfig getGitSyncConfig()
    {
        List<GitSyncConfig> configs = Ebean.find(GitSyncConfig.class).setMaxRows(1).findList();

        return configs.isEmpty() ? null : configs.get(0);
    }

    @Override
    public void saveOrUpdateGitSyncConfig(GitSyncConfig gitSyncConfig)
    {
        if (gitSyncConfig.getId() != null) {
            Ebean.update(gitSyncConfig);
        } else {
            Ebean.save(gitSyncConfig);
        }
    }
}
