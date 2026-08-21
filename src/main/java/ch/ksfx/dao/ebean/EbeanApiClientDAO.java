package ch.ksfx.dao.ebean;

import ch.ksfx.dao.ApiClientDAO;
import ch.ksfx.model.ApiClient;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanApiClientDAO implements ApiClientDAO
{
    @Override
    public void saveOrUpdateApiClient(ApiClient apiClient)
    {
        if (apiClient.getId() != null) {
            Ebean.update(apiClient);
        } else {
            Ebean.save(apiClient);
        }
    }

    @Override
    public void deleteApiClient(ApiClient apiClient)
    {
        Ebean.delete(apiClient);
    }

    @Override
    public List<ApiClient> getAllApiClients()
    {
        return Ebean.find(ApiClient.class).order().asc("name").findList();
    }

    @Override
    public ApiClient getApiClientForId(Long id)
    {
        return Ebean.find(ApiClient.class, id);
    }

    @Override
    public ApiClient getApiClientForToken(String apiToken)
    {
        return Ebean.find(ApiClient.class).where().eq("apiToken", apiToken).findUnique();
    }
}
