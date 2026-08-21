package ch.ksfx.services.security;

import ch.ksfx.dao.ApiClientDAO;
import ch.ksfx.model.ApiClient;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Resolves the bearer token carried by an unauthenticated {@link ApiClientAuthenticationToken} (see
 * {@link ApiTokenAuthenticationFilter}) against {@link ApiClientDAO}, mirroring how
 * DaoAuthenticationProvider resolves form-login credentials against KsfxUserDetailsService - the
 * difference being this principal is an {@link ApiClient}, not a Spring Security UserDetails/User.
 */
public class ApiClientAuthenticationProvider implements AuthenticationProvider
{
    private final ApiClientDAO apiClientDAO;

    public ApiClientAuthenticationProvider(ApiClientDAO apiClientDAO)
    {
        this.apiClientDAO = apiClientDAO;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        String token = (String) authentication.getCredentials();

        ApiClient apiClient = token == null || token.isEmpty() ? null : apiClientDAO.getApiClientForToken(token);

        if (apiClient == null || !apiClient.getEnabled()) {
            throw new BadCredentialsException("Invalid or disabled API token");
        }

        return new ApiClientAuthenticationToken(apiClient);
    }

    @Override
    public boolean supports(Class<?> authentication)
    {
        return ApiClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
