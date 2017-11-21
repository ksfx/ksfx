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

package ch.ksfx.web.pages.admin.user;

import ch.ksfx.dao.UserDAO;
import ch.ksfx.model.user.User;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;

@Secured({"ROLE_ADMIN"})
public class ManageUser
{
    private static final Long ADMIN_USER = 1l;

    @Inject
    private UserDAO userDAO;

    @Property
    private User user;

    @Property
    private String password;

    @Property
    private String passwordConfirm;

    @InjectComponent
    private Form userForm;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private SaltSource saltSource;

    @Secured({"ROLE_ADMIN"})
    public void onActivate()
    {
        user = userDAO.getUser(ADMIN_USER);
    }

    public void onSuccessFromUserForm()
    {
        user.setPassword(passwordEncoder.encodePassword(password, saltSource.getSalt(user)));
        userDAO.save(user);
    }

    public void onValidateFromUserForm()
    {
        if (password.length() < 5) {
            userForm.recordError("The password needs to be at least 5 characters long");
        }

        if (password == null || passwordConfirm == null || !passwordConfirm.equals(password)) {
            userForm.recordError("The passwords are not equal");
        }
    }
}
