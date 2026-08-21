package ch.ksfx.services.security;

import ch.ksfx.model.ApiClient;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * Authentication carrier for /api/** bearer-token requests. Unauthenticated instances hold the raw
 * token as credentials (built by {@link ApiTokenAuthenticationFilter}); {@link ApiClientAuthenticationProvider}
 * exchanges it for an authenticated instance carrying the resolved {@link ApiClient} as principal.
 */
public class ApiClientAuthenticationToken extends AbstractAuthenticationToken
{
    private final ApiClient principal;
    private final String credentials;

    public ApiClientAuthenticationToken(String rawToken)
    {
        super(null);
        this.principal = null;
        this.credentials = rawToken;
        setAuthenticated(false);
    }

    public ApiClientAuthenticationToken(ApiClient apiClient)
    {
        super(AuthorityUtils.createAuthorityList("ROLE_API_CLIENT"));
        this.principal = apiClient;
        this.credentials = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials()
    {
        return credentials;
    }

    @Override
    public Object getPrincipal()
    {
        return principal;
    }
}
