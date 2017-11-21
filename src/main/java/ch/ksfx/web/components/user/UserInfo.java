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

package ch.ksfx.web.components.user;

import ch.ksfx.model.user.User;
import ch.ksfx.web.services.users.UserService;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;


@Import(stylesheet = { "userinfo.css" })
public class UserInfo
{
    @SuppressWarnings("unused")
    @Property
    private User currentUser;
    
    @Inject
    private UserService userService;

    @SetupRender
    public boolean setupRender()
    {
        currentUser = userService.getCurrentUser();
        return true;
    }
}
