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
import ch.ksfx.web.services.springsecurity.RequestInvocationDefinition;
import ch.ksfx.web.services.springsecurity.SaltSourceService;
import ch.ksfx.web.services.springsecurity.SpringSecurityServices;
import ch.ksfx.web.services.springsecurity.internal.*;
import ch.ksfx.web.services.users.KsfxUserDetailsService;
import ch.ksfx.web.services.users.UserService;
import ch.ksfx.web.services.users.impl.UserServiceImpl;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.*;
import org.apache.tapestry5.ioc.services.ServiceOverride;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.access.intercept.RequestKey;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.util.AntUrlPathMatcher;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This module is automatically included as part of the Tapestry IoC Registry,
 *
 */
public class SecurityModule
{

    @SuppressWarnings("unchecked")
    public static void bind(final ServiceBinder binder)
    {
        binder.bind(LogoutService.class, LogoutServiceImpl.class).withMarker(SpringSecurityServices.class);

        binder.bind(AuthenticationTrustResolver.class, AuthenticationTrustResolverImpl.class)
                .withMarker(SpringSecurityServices.class);

        binder.bind(UserDAO.class, EbeanUserDAO.class);
        binder.bind(RoleDAO.class, EbeanRoleDAO.class);
        binder.bind(UserService.class, UserServiceImpl.class);
        binder.bind(UserDetailsService.class, KsfxUserDetailsService.class);
    }

    @Marker(SpringSecurityServices.class)
    public static PasswordEncoder buildPasswordEncoder()
    {
        return new ShaPasswordEncoder();
    }

    @Contribute(ServiceOverride.class)
    public static void setupApplicationServiceOverrides(MappedConfiguration<Class<?>, Object> configuration,
                                                        @SpringSecurityServices SaltSourceService saltSource,
                                                        @SpringSecurityServices UsernamePasswordAuthenticationFilter authenticationProcessingFilter)
    {
        configuration.add(SaltSourceService.class, saltSource);

        configuration.add(UsernamePasswordAuthenticationFilter.class, authenticationProcessingFilter);
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

    @Contribute(ComponentClassTransformWorker2.class)
    @Primary
    public static void addSpringSecurityWorker(
            OrderedConfiguration<ComponentClassTransformWorker2> configuration,
            SecurityChecker securityChecker)
    {
        configuration.add("SpringSecurity", new SpringSecurityWorker(securityChecker), "after:CleanupRender");
    }

    public static void contributeHttpServletRequestHandler(
            OrderedConfiguration<HttpServletRequestFilter> configuration,
            @InjectService("HttpSessionContextIntegrationFilter") HttpServletRequestFilter httpSessionContextIntegrationFilter,
            @InjectService("AuthenticationProcessingFilter") HttpServletRequestFilter authenticationProcessingFilter,
            // @InjectService("BasicProcessingFilter") HttpServletRequestFilter basicProcessingFilter,
            @InjectService("RememberMeProcessingFilter") HttpServletRequestFilter rememberMeProcessingFilter,
            @InjectService("SecurityContextHolderAwareRequestFilter") HttpServletRequestFilter securityContextHolderAwareRequestFilter,
            @InjectService("AnonymousProcessingFilter") HttpServletRequestFilter anonymousProcessingFilter,
            @InjectService("FilterSecurityInterceptor") HttpServletRequestFilter filterSecurityInterceptor,
            @InjectService("SpringSecurityExceptionFilter") SpringSecurityExceptionTranslationFilter springSecurityExceptionFilter)
    {
        // Provides the SecurityContext through the SecurityContextHolder
        configuration.add(
                "springSecurityHttpSessionContextIntegrationFilter",
                httpSessionContextIntegrationFilter,
                "before:springSecurity*");

        // Processes an authentication form
        configuration.add("springSecurityAuthenticationProcessingFilter", authenticationProcessingFilter);

        // Authentication by basic auth token
        // configuration.add("springSecurityBasicProcessingFilter", basicProcessingFilter,
        // "after:springSecurityAuthenticationProcessingFilter");

        // Authentication by remember-me authentication token
        configuration.add("springSecurityRememberMeProcessingFilter", rememberMeProcessingFilter);

        // Another wrapper for the ServletRequest
        configuration.add(
                "springSecuritySecurityContextHolderAwareRequestFilter",
                securityContextHolderAwareRequestFilter,
                "after:springSecurityRememberMeProcessingFilter");

        // If not yet authenticated, an anonymous <code>Authentication</code>
        // can be populated by this filter
        configuration.add(
                "springSecurityAnonymousProcessingFilter",
                anonymousProcessingFilter,
                "after:springSecurityRememberMeProcessingFilter",
                "after:springSecurityAuthenticationProcessingFilter");

        // Filter to catch <code>SpringSecurityException</code>s
        configuration.add("springSecurityExceptionFilter", new HttpServletRequestFilterWrapper(
                springSecurityExceptionFilter), "before:springSecurityFilterSecurityInterceptor");

        // Checks if access to the HTTP resource is allowed
        configuration.add(
                "springSecurityFilterSecurityInterceptor",
                filterSecurityInterceptor,
                "after:springSecurity*");
    }

    /**
    * Checks if access to the HTTP resource is allowed (
    * {@link org.springframework.security.access.intercept.AbstractSecurityInterceptor#beforeInvocation(Object)}
    * ) and throws {@link org.springframework.security.access.AccessDeniedException} otherwise
    */
    @Marker(SpringSecurityServices.class)
    public static HttpServletRequestFilter buildFilterSecurityInterceptor(
            @SpringSecurityServices final AccessDecisionManager accessDecisionManager,
            @SpringSecurityServices final AuthenticationManager manager,
            final Collection<RequestInvocationDefinition> contributions) throws Exception
    {
        FilterSecurityInterceptor interceptor = new FilterSecurityInterceptor();
        LinkedHashMap<RequestKey, Collection<ConfigAttribute>> requestMap = convertCollectionToLinkedHashMap(contributions);
        DefaultFilterInvocationSecurityMetadataSource source =
                new DefaultFilterInvocationSecurityMetadataSource(new AntUrlPathMatcher(true), requestMap);
        interceptor.setAccessDecisionManager(accessDecisionManager);
        interceptor.setAlwaysReauthenticate(false);
        interceptor.setAuthenticationManager(manager);
        interceptor.setSecurityMetadataSource(source);
        interceptor.setValidateConfigAttributes(true);
        interceptor.afterPropertiesSet();
        return new HttpServletRequestFilterWrapper(interceptor);
    }

    static LinkedHashMap<RequestKey, Collection<ConfigAttribute>> convertCollectionToLinkedHashMap(
            Collection<RequestInvocationDefinition> urls)
    {

        LinkedHashMap<RequestKey, Collection<ConfigAttribute>> requestMap = new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>();
        for (RequestInvocationDefinition url : urls) {

            requestMap.put(url.getRequestKey(), url.getConfigAttributeDefinition());
        }
        return requestMap;
    }

    /**
    * Provides the SecurityContext through the SecurityContextHolder
    */
    @Marker(SpringSecurityServices.class)
    public static HttpServletRequestFilter buildHttpSessionContextIntegrationFilter() throws Exception
    {

        SecurityContextPersistenceFilter filter = new SecurityContextPersistenceFilter();
        filter.setForceEagerSessionCreation(false);
        filter.afterPropertiesSet();
        return new HttpServletRequestFilterWrapper(filter);
    }

    /**
    * Checks credentials entered using a form.
    */
    @Marker(SpringSecurityServices.class)
    public static UsernamePasswordAuthenticationFilter buildRealAuthenticationProcessingFilter(
            @SpringSecurityServices final AuthenticationManager manager,
            @SpringSecurityServices final RememberMeServices rememberMeServices,
            @Inject @Value("${spring-security.check.url}") final String authUrl,
            @Inject @Value("${spring-security.target.url}") final String targetUrl,
            @Inject @Value("${spring-security.failure.url}") final String failureUrl,
            @Inject @Value("${spring-security.always.use.target.url}") final String alwaysUseTargetUrl) throws Exception
    {

        UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter();
        filter.setAuthenticationManager(manager);

        filter.setPostOnly(false);

        filter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler(failureUrl));

        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl(targetUrl);
        successHandler.setAlwaysUseDefaultTargetUrl(Boolean.parseBoolean(alwaysUseTargetUrl));
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setFilterProcessesUrl(targetUrl);
        filter.setFilterProcessesUrl(authUrl);
        filter.setRememberMeServices(rememberMeServices);

        filter.afterPropertiesSet();
        return filter;
    }

    /**
    * Wrapper for
    * {@link #buildRealAuthenticationProcessingFilter(org.springframework.security.authentication.AuthenticationManager, org.springframework.security.web.authentication.RememberMeServices, String, String, String, String)}
    */
    @Marker(SpringSecurityServices.class)
    public static HttpServletRequestFilter buildAuthenticationProcessingFilter(
            final UsernamePasswordAuthenticationFilter filter) throws Exception
    {
        return new HttpServletRequestFilterWrapper(filter);
    }

    /**
    * Tries authentication using the basic authentication token
    */
    // @Marker(SpringSecurityServices.class)
    // public static HttpServletRequestFilter buildBasicProcessingFilter(
    // @SpringSecurityServices final AuthenticationManager manager,
    // @SpringSecurityServices final RememberMeServices rememberMeServices,
    // final AuthenticationEntryPoint entryPoint) throws Exception
    // {
    // BasicProcessingFilter filter = new BasicProcessingFilter();
    // filter.setAuthenticationManager(manager);
    // filter.setAuthenticationEntryPoint(entryPoint);
    // filter.setRememberMeServices(rememberMeServices);
    // filter.afterPropertiesSet();
    // return new HttpServletRequestFilterWrapper(filter);
    // }

    /**
    * Checks for remember-me token
    */
    @Marker(SpringSecurityServices.class)
    public static HttpServletRequestFilter buildRememberMeProcessingFilter(
            @SpringSecurityServices final RememberMeServices rememberMe,
            @SpringSecurityServices final AuthenticationManager authManager) throws Exception
    {

        RememberMeAuthenticationFilter filter = new RememberMeAuthenticationFilter();
        filter.setRememberMeServices(rememberMe);
        filter.setAuthenticationManager(authManager);
        filter.afterPropertiesSet();
        return new HttpServletRequestFilterWrapper(filter);
    }

    @Marker(SpringSecurityServices.class)
    public static HttpServletRequestFilter buildSecurityContextHolderAwareRequestFilter()
    {
        return new HttpServletRequestFilterWrapper(new SecurityContextHolderAwareRequestFilter());
    }

    /**
    * Detects if there is no <code>Authentication</code> object in the
    * <code>SecurityContextHolder</code>, and populates it with one if needed.
    */
    @Marker(SpringSecurityServices.class)
    public static HttpServletRequestFilter buildAnonymousProcessingFilter(
            @Inject @Value("${spring-security.anonymous.attribute}") final String anonymousAttr,
            @Inject @Value("${spring-security.anonymous.key}") final String anonymousKey) throws Exception
    {
        AnonymousAuthenticationFilter filter = new AnonymousAuthenticationFilter();
        filter.setKey(anonymousKey);
        UserAttributeEditor attrEditor = new UserAttributeEditor();
        attrEditor.setAsText(anonymousAttr);
        UserAttribute attr = (UserAttribute) attrEditor.getValue();
        filter.setUserAttribute(attr);
        filter.afterPropertiesSet();
        return new HttpServletRequestFilterWrapper(filter);
    }

    /**
    * Identifies previously remembered users by a Base-64 encoded cookie.
    */
    @Marker(SpringSecurityServices.class)
    public static RememberMeServices build(
            final UserDetailsService userDetailsService,
            @Inject @Value("${spring-security.rememberme.key}") final String rememberMeKey)
    {
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices();
        rememberMe.setUserDetailsService(userDetailsService);
        rememberMe.setKey(rememberMeKey);
        return rememberMe;
    }

    /**
    * Same as {@link #build(UserDetailsService, String)}. Not sure why it has to be a service of
    * its own.
    * {@link TokenBasedRememberMeServices#afterPropertiesSet()}
    * only does some assertions.
    */
    @Marker(SpringSecurityServices.class)
    public static LogoutHandler buildRememberMeLogoutHandler(
            final UserDetailsService userDetailsService,
            @Inject @Value("${spring-security.rememberme.key}") final String rememberMeKey) throws Exception
    {
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices();
        rememberMe.setUserDetailsService(userDetailsService);
        rememberMe.setKey(rememberMeKey);
        rememberMe.afterPropertiesSet();
        return rememberMe;
    }

    /**
    * Configuration for LogoutService created in {@link #bind(ServiceBinder)}
    */
    public static void contributeLogoutService(
            final OrderedConfiguration<LogoutHandler> cfg,
            @Inject RequestGlobals globals,
            @InjectService("RememberMeLogoutHandler") final LogoutHandler rememberMeLogoutHandler)
    {
        cfg.add("securityContextLogoutHandler", new SecurityContextLogoutHandler());
        cfg.add("rememberMeLogoutHandler", rememberMeLogoutHandler);
        cfg.add("tapestryLogoutHandler", new TapestryLogoutHandler(globals), new String[0]);
    }

    /**
    * Iterates an <code>Authentication</code> request through a list of
    * <code>AuthenticationProvider</code>s.
    */
    @Marker(SpringSecurityServices.class)
    public static AuthenticationManager buildProviderManager(final List<AuthenticationProvider> providers)
            throws Exception
    {
        ProviderManager manager = new ProviderManager();
        manager.setProviders(providers);
        manager.afterPropertiesSet();
        return manager;
    }

    /**
    * Validates <code>AnonymousAuthenticationToken</code>s
    */
    @Marker(SpringSecurityServices.class)
    public final AuthenticationProvider buildAnonymousAuthenticationProvider(
            @Inject @Value("${spring-security.anonymous.key}") final String anonymousKey) throws Exception
    {

        AnonymousAuthenticationProvider provider = new AnonymousAuthenticationProvider();
        provider.setKey(anonymousKey);
        provider.afterPropertiesSet();
        return provider;
    }

    /**
    * Validates <code>RememberMeAuthenticationToken</code>s
    */
    @Marker(SpringSecurityServices.class)
    public final AuthenticationProvider buildRememberMeAuthenticationProvider(
            @Inject @Value("${spring-security.rememberme.key}") final String rememberMeKey) throws Exception
    {
        RememberMeAuthenticationProvider provider = new RememberMeAuthenticationProvider();
        provider.setKey(rememberMeKey);
        provider.afterPropertiesSet();
        return provider;
    }

    /**
    * Retrieves user details from an <code>UserDetailsService</code>
    */
    @Marker(SpringSecurityServices.class)
    public final AuthenticationProvider buildDaoAuthenticationProvider(
            final UserDetailsService userDetailsService,
            final PasswordEncoder passwordEncoder,
            final SaltSourceService saltSource) throws Exception
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setSaltSource(saltSource);
        provider.afterPropertiesSet();
        return provider;
    }

    public final void contributeProviderManager(
            final OrderedConfiguration<AuthenticationProvider> configuration,
            @InjectService("AnonymousAuthenticationProvider") final AuthenticationProvider anonymousAuthenticationProvider,
            @InjectService("RememberMeAuthenticationProvider") final AuthenticationProvider rememberMeAuthenticationProvider)
    {
        configuration.add("anonymousAuthenticationProvider", anonymousAuthenticationProvider);
        configuration.add("rememberMeAuthenticationProvider", rememberMeAuthenticationProvider);
    }

    /**
    * Decides uppon access to an object. It passes the object to a list of <code>RoleVoter</code>s.
    * Based on the AccessDecisionManager's implementation, access is granted or denied.
    */
    @Marker(SpringSecurityServices.class)
    public final AccessDecisionManager buildAccessDecisionManager(final List<AccessDecisionVoter> voters)
            throws Exception
    {
        AffirmativeBased manager = new AffirmativeBased();
        manager.setDecisionVoters(voters);
        manager.afterPropertiesSet();
        return manager;
    }

    /**
    * Used by the AccessDecisionManager
    */
    public final void contributeAccessDecisionManager(final OrderedConfiguration<AccessDecisionVoter> configuration)
    {
        configuration.add("RoleVoter", new RoleVoter());
    }

    @Marker(SpringSecurityServices.class)
    public static SecurityChecker buildSecurityChecker(
            @SpringSecurityServices final AccessDecisionManager accessDecisionManager,
            @SpringSecurityServices final AuthenticationManager authenticationManager) throws Exception
    {
        StaticSecurityChecker checker = new StaticSecurityChecker();

        checker.setAccessDecisionManager(accessDecisionManager);
        checker.setAuthenticationManager(authenticationManager);
        checker.afterPropertiesSet();
        return checker;
    }

    /**
    * Web form <code>AuthenticationEntryPoint</code>
    */
    @Marker(SpringSecurityServices.class)
    public static AuthenticationEntryPoint buildAuthenticationEntryPoint(
            @Inject @Value("${spring-security.loginform.url}") final String loginFormUrl,
            @Inject @Value("${spring-security.force.ssl.login}") final String forceHttps) throws Exception
    {
        LoginUrlAuthenticationEntryPoint entryPoint = new LoginUrlAuthenticationEntryPoint();
        entryPoint.setLoginFormUrl(loginFormUrl);
        entryPoint.afterPropertiesSet();
        boolean forceSSL = Boolean.parseBoolean(forceHttps);
        entryPoint.setForceHttps(forceSSL);
        return entryPoint;
    }

    /**
    * Filter to catch <code>SpringSecurityException</code>s
    */
    public static SpringSecurityExceptionTranslationFilter buildSpringSecurityExceptionFilter(
            final AuthenticationEntryPoint aep,
            @Inject @Value("${spring-security.accessDenied.url}") final String accessDeniedUrl) throws Exception
    {
        SpringSecurityExceptionTranslationFilter filter = new SpringSecurityExceptionTranslationFilter();
        filter.setAuthenticationEntryPoint(aep);
        if (!accessDeniedUrl.equals("")) {
            T5AccessDeniedHandler accessDeniedHandler = new T5AccessDeniedHandler();
            accessDeniedHandler.setErrorPage(accessDeniedUrl);
            filter.setAccessDeniedHandler(accessDeniedHandler);
        }
        filter.afterPropertiesSet();
        return filter;
    }

    public static void contributeRequestHandler(
            final OrderedConfiguration<RequestFilter> configuration,
            final RequestGlobals globals,
            @InjectService("SpringSecurityExceptionFilter") final SpringSecurityExceptionTranslationFilter springSecurityExceptionFilter)
    {
        configuration.add("SpringSecurityExceptionFilter", new RequestFilterWrapper(
                globals,
                springSecurityExceptionFilter), "after:ErrorFilter");
    }
}