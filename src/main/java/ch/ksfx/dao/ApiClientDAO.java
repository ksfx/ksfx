package ch.ksfx.dao;

import ch.ksfx.model.ApiClient;

import java.util.List;

public interface ApiClientDAO
{
    public void saveOrUpdateApiClient(ApiClient apiClient);
    public void deleteApiClient(ApiClient apiClient);
    public List<ApiClient> getAllApiClients();
    public ApiClient getApiClientForId(Long id);
    public ApiClient getApiClientForToken(String apiToken);
}
