package ch.ksfx.controller.admin.genericdatastore;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class GenericDataStoreController
{
    private GenericDataStoreDAO genericDataStoreDAO;

    public GenericDataStoreController(GenericDataStoreDAO genericDataStoreDAO)
    {
        this.genericDataStoreDAO = genericDataStoreDAO;
    }

    @GetMapping("/genericdatastore")
    public String genericDataStoreIndex(Pageable pageable, Model model)
    {
        model.addAttribute("nonPersistentStore", GenericDataStore.nonPersistentStore);
        model.addAttribute("genericDataStoresPage", genericDataStoreDAO.getGenericDataStoresForPageable(pageable));

        return "admin/genericdatastore/generic_data_store";
    }

    @GetMapping("/genericdatastoredelete/{genericDataStoreId}")
    public String genericDataStoreDelete(@PathVariable(value = "genericDataStoreId", required = true) Long genericDataStoreId)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForId(genericDataStoreId);
        genericDataStoreDAO.deleteGenericDataStore(genericDataStore);

        return "redirect:/admin/genericdatastore";
    }

    @GetMapping("/nonpersistentstoredelete/{nonpersistentstorekey}")
    public String nonPersistentStoreDelete(@PathVariable(value = "nonpersistentstorekey", required = true) String nonPersistentStoreKey)
    {
        GenericDataStore.nonPersistentStore.remove(nonPersistentStoreKey);

        return "redirect:/admin/genericdatastore";
    }
}
