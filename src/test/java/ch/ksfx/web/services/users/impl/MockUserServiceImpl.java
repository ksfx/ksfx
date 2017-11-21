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

package ch.ksfx.web.services.users.impl;

import ch.ksfx.dao.UserDAO;
import ch.ksfx.model.user.User;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Value;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;


public class MockUserServiceImpl extends UserServiceImpl
{
    public MockUserServiceImpl(UserDAO userDao, PasswordEncoder encoder, SaltSource salt,
            @Inject @Value("${spring-security.anonymous.attribute}") String anonymousAttribute)
    {
        super(userDao, encoder, salt, anonymousAttribute);
    }
    
    @Override
    public User getCurrentUser()
    {
        return new User();
    }
    
    
}
