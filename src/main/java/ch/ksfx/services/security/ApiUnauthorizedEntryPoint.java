package ch.ksfx.services.security;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Returns 401 + JSON for unauthenticated /api/** requests instead of the app-wide default of
 * redirecting to /login, which only makes sense for a browser session, not a bearer-token caller.
 * Wired in via WebSecurityConfig's exceptionHandling().defaultAuthenticationEntryPointFor(...).
 */
public class ApiUnauthorizedEntryPoint implements AuthenticationEntryPoint
{
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
}
