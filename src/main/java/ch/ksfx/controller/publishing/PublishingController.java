package ch.ksfx.controller.publishing;

import ch.ksfx.dao.PublishingConfigurationDAO;
import ch.ksfx.model.PublishingConfiguration;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/publishing")
public class PublishingController
{
    private PublishingConfigurationDAO publishingConfigurationDAO;

    public PublishingController(PublishingConfigurationDAO publishingConfigurationDAO)
    {
        this.publishingConfigurationDAO = publishingConfigurationDAO;
    }

    @GetMapping("/")
    public String publishingIndex(Pageable pageable, Model model)
    {
        Page<PublishingConfiguration> publishingConfigurationsPage = publishingConfigurationDAO.getPublishingConfigutationsForPageableAndPublishingCategory(pageable, null);

        model.addAttribute("publishingConfigurationsPage", publishingConfigurationsPage);

        return "publishing/publishing";
    }
}
