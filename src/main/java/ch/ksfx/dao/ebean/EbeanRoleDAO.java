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

import ch.ksfx.dao.RoleDAO;
import ch.ksfx.model.user.Role;
import io.ebean.Ebean;

import java.util.List;


public class EbeanRoleDAO implements RoleDAO
{
    @Override
    public Role getRoleById(Long id)
    {
        return Ebean.find(Role.class, id);
    }

    @Override
    public Role getRoleByName(String name)
    {
        return Ebean.find(Role.class).where().eq("name",name).findUnique();
    }

    @Override
    public List<Role> getRoles()
    {
        return Ebean.find(Role.class).findList();
    }

    @Override
    public void save(Role role)
    {
        if (role.getId() != null) {
            Ebean.update(role);
        } else {
            Ebean.save(role);
        }
    }

    @Override
    public void delete(Role role)
    {
        Ebean.delete(role);
    }
}
