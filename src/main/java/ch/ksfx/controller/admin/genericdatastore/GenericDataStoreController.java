package ch.ksfx.controller.admin.genericdatastore;

import ch.ksfx.dao.GenericDataStoreDAO;
import ch.ksfx.model.GenericDataStore;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin screen for the Generic Data Store - the key/value table Activities read their
 * configuration from (GenericDataStoreDAO#getGenericDataStoreForKey). Until 2026-10-06 the UI
 * could only list and delete; entries were created by Activities themselves or by hand in the
 * database. Now: add + edit, same form pattern as DataStructureController's category edit.
 */
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

    @GetMapping({"/genericdatastoreedit", "/genericdatastoreedit/{id}"})
    public String genericDataStoreEdit(@PathVariable(value = "id", required = false) Long genericDataStoreId, Model model)
    {
        GenericDataStore genericDataStore = new GenericDataStore();

        if (genericDataStoreId != null) {
            genericDataStore = genericDataStoreDAO.getGenericDataStoreForId(genericDataStoreId);

            if (genericDataStore == null) {
                return "redirect:/admin/genericdatastore";
            }
        }

        model.addAttribute("genericDataStore", genericDataStore);

        return "admin/genericdatastore/generic_data_store_edit";
    }

    /**
     * Key is trimmed and must be non-blank and unique - the DAO's getGenericDataStoreForKey is a
     * single-row lookup, a duplicate key would make which value an Activity sees a matter of luck.
     * The value is stored as-is (no trim): it's a LONGTEXT that may legitimately be multi-line or
     * carry significant whitespace, and empty is allowed (a key can exist with no value yet).
     */
    @PostMapping({"/genericdatastoreedit", "/genericdatastoreedit/{id}"})
    public String genericDataStoreSubmit(@PathVariable(value = "id", required = false) Long genericDataStoreId,
                                         @ModelAttribute GenericDataStore genericDataStore, BindingResult bindingResult)
    {
        String key = genericDataStore.getDataKey() == null ? "" : genericDataStore.getDataKey().trim();
        genericDataStore.setDataKey(key);

        if (key.isEmpty()) {
            bindingResult.rejectValue("dataKey", "genericDataStore.dataKey", "Key must not be empty");
        } else {
            GenericDataStore existing = genericDataStoreDAO.getGenericDataStoreForKey(key);

            if (existing != null && (genericDataStore.getId() == null || !existing.getId().equals(genericDataStore.getId()))) {
                bindingResult.rejectValue("dataKey", "genericDataStore.dataKey", "An entry with this key already exists (id " + existing.getId() + ")");
            }
        }

        if (bindingResult.hasErrors()) {
            return "admin/genericdatastore/generic_data_store_edit";
        }

        genericDataStoreDAO.saveOrUpdateGenericDataStore(genericDataStore);

        return "redirect:/admin/genericdatastore";
    }

    @GetMapping("/genericdatastoredelete/{genericDataStoreId}")
    public String genericDataStoreDelete(@PathVariable(value = "genericDataStoreId", required = true) Long genericDataStoreId)
    {
        GenericDataStore genericDataStore = genericDataStoreDAO.getGenericDataStoreForId(genericDataStoreId);

        if (genericDataStore != null) {
            genericDataStoreDAO.deleteGenericDataStore(genericDataStore);
        }

        return "redirect:/admin/genericdatastore";
    }

    @GetMapping("/nonpersistentstoredelete/{nonpersistentstorekey}")
    public String nonPersistentStoreDelete(@PathVariable(value = "nonpersistentstorekey", required = true) String nonPersistentStoreKey)
    {
        GenericDataStore.nonPersistentStore.remove(nonPersistentStoreKey);

        return "redirect:/admin/genericdatastore";
    }
}
