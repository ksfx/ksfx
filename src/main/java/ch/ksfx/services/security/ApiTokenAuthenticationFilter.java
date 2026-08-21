package ch.ksfx.services.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Populates the SecurityContext for /api/** requests from an "Authorization: Bearer &lt;token&gt;"
 * header, delegating the actual check to {@link ApiClientAuthenticationProvider} via the shared
 * AuthenticationManager - unlike the older /agentic/api/** controllers, which check tokens inline
 * per-request and bypass Spring Security's authentication machinery entirely.
 *
 * Never writes the response itself: on a missing/invalid token it just leaves the context empty and
 * continues the chain, letting WebSecurityConfig's anyRequest().authenticated() plus
 * ApiUnauthorizedEntryPoint produce the 401.
 */
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter
{
    private final AuthenticationManager authenticationManager;
    private final RequestMatcher apiMatcher = new AntPathRequestMatcher("/api/**");

    public ApiTokenAuthenticationFilter(AuthenticationManager authenticationManager)
    {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException
    {
        if (!apiMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();

            try {
                Authentication authentication = authenticationManager.authenticate(new ApiClientAuthenticationToken(token));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthenticationException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
