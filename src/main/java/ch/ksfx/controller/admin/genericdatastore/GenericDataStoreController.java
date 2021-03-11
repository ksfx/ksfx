package ch.ksfx.controller.admin.genericdatastore;

import ch.ksfx.dao.GenericDataStoreDAO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/genericdatastore")
public class GenericDataStoreController
{
    private GenericDataStoreDAO genericDataStoreDAO;

    public GenericDataStoreController(GenericDataStoreDAO genericDataStoreDAO)
    {
        this.genericDataStoreDAO = genericDataStoreDAO;
    }

    @GetMapping("/")
    public String genericDataStoreIndex(Pageable pageable, Model model)
    {
        model.addAttribute("genericDataStoresPage", genericDataStoreDAO.getGenericDataStoresForPageable(pageable));

        return "admin/genericdatastore/generic_data_store";
    }
}
