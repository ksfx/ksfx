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

package ch.ksfx.services.user;

import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class KsfxUserDetailsService implements UserDetailsService
{
    private UserDAO userDAO;
    private static Logger logger = LoggerFactory.getLogger(KsfxUserDetailsService.class);

    public KsfxUserDetailsService(UserDAO userDao)
    {
        this.userDAO = userDao;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        User user = userDAO.getUserByName(username);

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        logger.info("======= Loaded user: " + user.getUsername());

        try {
            logger.info("======= Authorities " + user.getAuthorities());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return user;
    }
}
