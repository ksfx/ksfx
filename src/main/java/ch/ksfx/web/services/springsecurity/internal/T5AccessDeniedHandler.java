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

package ch.ksfx.web.services.springsecurity.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.PortMapper;
import org.springframework.security.web.PortMapperImpl;
import org.springframework.security.web.PortResolver;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.RedirectUrlBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



/**
 * Enables Tapestry to handle AccessDenied Exceptions. So you can show some
 * additional information on your Page.
 * 
 * TODO At the time only the implied PortMapping(80:443) is supported.
 * 
 * @author Michael Gerzabek
 * 
 */
public class T5AccessDeniedHandler implements AccessDeniedHandler
{
    private static final Logger logger = LoggerFactory.getLogger(T5AccessDeniedHandler.class);

    private String errorPage = "/";
    private boolean forceHttps = false;
    private PortMapper portMapper = new PortMapperImpl();
    private PortResolver portResolver = new PortResolverImpl();

    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException,
            ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String redirectUrl = buildRedirectUrlToLoginPage(httpRequest,
                httpResponse, accessDeniedException);
        httpResponse.sendRedirect(httpResponse.encodeRedirectURL(redirectUrl));
    }

    protected String buildRedirectUrlToLoginPage(HttpServletRequest request,
            HttpServletResponse response, AccessDeniedException authException)
    {
        String loginForm = errorPage;
        int serverPort = portResolver.getServerPort(request);
        String scheme = request.getScheme();

        RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();

        urlBuilder.setScheme(scheme);
        urlBuilder.setServerName(request.getServerName());
        urlBuilder.setPort(serverPort);
        urlBuilder.setContextPath(request.getContextPath());
        urlBuilder.setPathInfo(loginForm);

        if (forceHttps && "http".equals(scheme)) {
            Integer httpsPort = portMapper.lookupHttpsPort(new Integer(
                    serverPort));

            if (httpsPort != null) {
                // Overwrite scheme and port in the redirect URL
                urlBuilder.setScheme("https");
                urlBuilder.setPort(httpsPort.intValue());
            } else {
                logger
                        .warn("Unable to redirect to HTTPS as no port mapping found for HTTP port "
                                + serverPort);
            }
        }

        return urlBuilder.getUrl();
    }

    public void setErrorPage(String errorPage)
    {
        if ((errorPage != null) && !errorPage.startsWith("/")) {
            throw new IllegalArgumentException("errorPage must begin with '/'");
        }

        this.errorPage = errorPage;
    }

    public void setForceHttps(boolean forceHttps)
    {
        this.forceHttps = forceHttps;
    }
}
