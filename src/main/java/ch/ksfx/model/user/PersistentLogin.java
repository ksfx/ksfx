package ch.ksfx.model.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/** One remember-me token series ("Angemeldet bleiben") - see EbeanPersistentTokenRepository. */
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin
{
    private String series;
    private String username;
    private String token;
    private Date lastUsed;

    @Id
    @Column(name = "series")
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    @Column(name = "username")
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Column(name = "token")
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    @Column(name = "last_used")
    public Date getLastUsed() { return lastUsed; }
    public void setLastUsed(Date lastUsed) { this.lastUsed = lastUsed; }
}
