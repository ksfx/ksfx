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

package ch.ksfx.web.services;

import ch.ksfx.dao.RoleDAO;
import ch.ksfx.dao.UserDAO;
import ch.ksfx.dao.ebean.EbeanRoleDAO;
import ch.ksfx.dao.ebean.EbeanUserDAO;
import ch.ksfx.web.services.springsecurity.LogoutService;
import ch.ksfx.web.services.springsecurity.SaltSourceService;
import ch.ksfx.web.services.springsecurity.SpringSecurityServices;
import ch.ksfx.web.services.springsecurity.internal.LogoutServiceImpl;
import ch.ksfx.web.services.springsecurity.internal.SaltSourceImpl;
import ch.ksfx.web.services.springsecurity.internal.SecurityChecker;
import ch.ksfx.web.services.users.UserService;
import ch.ksfx.web.services.users.impl.MockUserServiceImpl;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Marker;
import org.apache.tapestry5.ioc.annotations.Value;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;


public class TestSecurityModule /*extends SecurityModule*/
{
    public static void bind(final ServiceBinder binder)
    {
        binder.bind(LogoutService.class, LogoutServiceImpl.class).withMarker(
                SpringSecurityServices.class);

        binder.bind(AuthenticationTrustResolver.class, AuthenticationTrustResolverImpl.class).withMarker(SpringSecurityServices.class);

        binder.bind(UserDAO.class, EbeanUserDAO.class).withMarker(SpringSecurityServices.class);
        binder.bind(RoleDAO.class, EbeanRoleDAO.class).withMarker(SpringSecurityServices.class);
        
        //Mock out user service
        binder.bind(UserService.class, MockUserServiceImpl.class);
    }

    public static void contributeFactoryDefaults(final MappedConfiguration<String, String> configuration)
    {
        configuration.add("spring-security.check.url", "/j_spring_security_check");
        configuration.add("spring-security.failure.url", "/login/failed");
        configuration.add("spring-security.target.url", "/");
        configuration.add("spring-security.afterlogout.url", "/");
        configuration.add("spring-security.accessDenied.url", "");
        configuration.add("spring-security.force.ssl.login", "false");
        configuration.add("spring-security.rememberme.key", "REMEMBERMEKEY");
        configuration.add("spring-security.loginform.url", "/login");
        configuration.add("spring-security.anonymous.key", "spring_anonymous");
        configuration.add("spring-security.anonymous.attribute", "anonymous,ROLE_ANONYMOUS");
        configuration.add("spring-security.password.salt", "DEADBEEF");
        configuration.add("spring-security.always.use.target.url", "false");
    }
/*
    public static void contributeComponentClassTransformWorker2(
            OrderedConfiguration<ComponentClassTransformWorker2> configuration,
            SecurityChecker securityChecker)
    {
        //MOCK this out, will anyway not work without a proper Servlet context
    }
    */

    public static PasswordEncoder buildPasswordEncoder()
    {
        return new ShaPasswordEncoder();
    }

    @Marker(SpringSecurityServices.class)
    public static SaltSourceService buildSaltSource(
            @Inject @Value("${spring-security.password.salt}") final String salt) throws Exception
    {
        SaltSourceImpl saltSource = new SaltSourceImpl();
        saltSource.setSystemWideSalt(salt);
        saltSource.afterPropertiesSet();
        return saltSource;
    }
}
