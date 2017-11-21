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

import ch.ksfx.dao.UserDAO;
import ch.ksfx.model.user.User;
import io.ebean.Ebean;

import java.util.List;


public class EbeanUserDAO implements UserDAO
{
    @Override
    public User getUser(Long id)
    {
        return Ebean.find(User.class, id);
    }

    @Override
    public User getUserByName(String name) {
        return Ebean.find(User.class).where().eq("username", name).findUnique();
    }

    @Override
    public List<User> getAllUsers()
    {
        return Ebean.find(User.class).findList();
    }

    @Override
    public void save(User user)
    {
        if (user.getId() != null) {
            Ebean.update(user);
        } else {
            Ebean.save(user);
        }
    }

    @Override
    public void delete(User user)
    {
        Ebean.delete(user);
    }
}
