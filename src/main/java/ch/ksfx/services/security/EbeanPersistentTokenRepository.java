package ch.ksfx.services.security;

import ch.ksfx.model.user.PersistentLogin;
import io.ebean.Ebean;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Remember-me token store for "Angemeldet bleiben" (table persistent_logins), through Ebean like
 * every other KSFX table - Spring's JdbcTokenRepositoryImpl would need a Spring DataSource, which
 * KSFX does not configure (its datasource comes from conf.xml via ConfigurationDatabaseProvider).
 */
@Service
public class EbeanPersistentTokenRepository implements PersistentTokenRepository
{
    @Override
    public void createNewToken(PersistentRememberMeToken token)
    {
        PersistentLogin login = new PersistentLogin();
        login.setSeries(token.getSeries());
        login.setUsername(token.getUsername());
        login.setToken(token.getTokenValue());
        login.setLastUsed(token.getDate());
        Ebean.insert(login);
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed)
    {
        PersistentLogin login = Ebean.find(PersistentLogin.class, series);

        if (login != null) {
            login.setToken(tokenValue);
            login.setLastUsed(lastUsed);
            Ebean.update(login);
        }
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId)
    {
        PersistentLogin login = Ebean.find(PersistentLogin.class, seriesId);

        return login == null ? null : new PersistentRememberMeToken(login.getUsername(), login.getSeries(), login.getToken(), login.getLastUsed());
    }

    /** All devices of this user - logout, password change, user deletion, "Auf allen Geräten abmelden". */
    @Override
    public void removeUserTokens(String username)
    {
        Ebean.find(PersistentLogin.class).where().eq("username", username).delete();
    }
}
