package ch.ksfx.controller;

import ch.ksfx.dao.ApiClientDAO;
import ch.ksfx.model.ApiClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Date;
import java.util.UUID;

/**
 * Management UI for ApiClient credentials - the /api/** bearer-token principals (see
 * ch.ksfx.services.security.ApiClientAuthenticationProvider). No role-based restriction beyond
 * normal login, consistent with the rest of the app's current security model.
 */
@Controller
@RequestMapping("/apiclients")
public class ApiClientController
{
    private final ApiClientDAO apiClientDAO;

    public ApiClientController(ApiClientDAO apiClientDAO)
    {
        this.apiClientDAO = apiClientDAO;
    }

    @GetMapping("/")
    public String index(Model model)
    {
        model.addAttribute("apiClients", apiClientDAO.getAllApiClients());

        return "apiclient/apiclient_list";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long id, Model model)
    {
        ApiClient apiClient = id != null ? apiClientDAO.getApiClientForId(id) : new ApiClient();

        model.addAttribute("apiClient", apiClient);

        return "apiclient/apiclient_edit";
    }

    @PostMapping({"/edit", "/edit/{id}"})
    public String submit(@PathVariable(value = "id", required = false) Long id, @Valid @ModelAttribute ApiClient apiClient, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            return "apiclient/apiclient_edit";
        }

        boolean isNew = apiClient.getId() == null;

        // apiToken/createdAt are read-only display values, not <input>s - without this they'd be
        // silently wiped to null on every edit (same class of bug fixed for AgentController/submit).
        if (!isNew) {
            ApiClient previous = apiClientDAO.getApiClientForId(apiClient.getId());
            apiClient.setApiToken(previous.getApiToken());
            apiClient.setCreatedAt(previous.getCreatedAt());
        } else {
            apiClient.setCreatedAt(new Date());
            apiClient.setApiToken(UUID.randomUUID().toString().replace("-", ""));
        }

        apiClientDAO.saveOrUpdateApiClient(apiClient);

        return "redirect:/apiclients/edit/" + apiClient.getId();
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable(value = "id") Long id, RedirectAttributes redirectAttributes)
    {
        ApiClient apiClient = apiClientDAO.getApiClientForId(id);

        apiClientDAO.deleteApiClient(apiClient);

        redirectAttributes.addFlashAttribute("resultMessage", "API Client deleted.");

        return "redirect:/apiclients/";
    }
}
