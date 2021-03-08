package ch.ksfx.controller.informationretrieval;

import ch.ksfx.dao.spidering.ResourceDAO;
import ch.ksfx.dao.spidering.SpideringDAO;
import ch.ksfx.model.spidering.Resource;
import ch.ksfx.model.spidering.Result;
import ch.ksfx.model.spidering.Spidering;
import ch.ksfx.model.spidering.SpideringConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/informationretrieval/resource")
public class ResourceController
{
    private ResourceDAO resourceDAO;
    private SpideringDAO spideringDAO;

    public ResourceController(ResourceDAO resourceDAO, SpideringDAO spideringDAO)
    {
        this.resourceDAO = resourceDAO;
        this.spideringDAO = spideringDAO;
    }

    @GetMapping("/{spideringid}")
    public String resourceIndex(@PathVariable(value = "spideringid", required = true) Long spideringId, Pageable pageable, Model model)
    {
        Spidering spidering = spideringDAO.getSpideringForId(spideringId);
        Page<Resource> resourcesPage = resourceDAO.getResourcesForPageableAndSpidering(pageable, spidering);

        model.addAttribute("spidering", spidering);
        model.addAttribute("resourcesPage", resourcesPage);

        return "informationretrieval/resource";
    }
}
