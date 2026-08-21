package ch.ksfx.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Credential for an external caller of the /api/** REST API - distinct from {@link Agent} (which
 * authenticates the app's own agentic subsystem under /agentic/api/**) and from
 * {@link ch.ksfx.model.user.User} (form-login human accounts). Authenticated via
 * ch.ksfx.services.security.ApiClientAuthenticationProvider.
 */
@Entity
@Table(name = "api_client")
public class ApiClient
{
    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    @NotEmpty
    private String name;
    private String apiToken;
    private boolean enabled = true;
    private Date createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Bearer token for this client's /api/** calls. Generated once at creation; no rotation in v1
     * (same pattern as {@link Agent#getApiToken()}).
     */
    public String getApiToken()
    {
        return apiToken;
    }

    public void setApiToken(String apiToken)
    {
        this.apiToken = apiToken;
    }

    public boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }
}
