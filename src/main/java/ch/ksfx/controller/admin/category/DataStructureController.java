package ch.ksfx.controller.admin.category;

import ch.ksfx.dao.CategoryDAO;
import ch.ksfx.dao.TimeSeriesDAO;
import ch.ksfx.model.Category;
import ch.ksfx.services.lucene.IndexService;
import ch.ksfx.services.seriesbrowser.SeriesBrowser;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequestMapping("/admin")
public class DataStructureController
{
    private CategoryDAO categoryDAO;
    private TimeSeriesDAO timeSeriesDAO;
    private SeriesBrowser seriesBrowser;
    private IndexService indexService;

    public DataStructureController(CategoryDAO categoryDAO)
    {
        this.categoryDAO = categoryDAO;
    }

    @GetMapping("/category")
    public String categoryIndex(Model model, Pageable pageable)
    {
        model.addAttribute("categoriesPage", categoryDAO.getCategoriesForPageable(pageable));

        return "admin/category/category";
    }

    @GetMapping({"/categoryedit", "/categoryedit/{id}"})
    public String categoryEdit(@PathVariable(value = "id", required = false) Long categoryId, Model model)
    {
        Category category = new Category();

        if (categoryId != null) {
            category = categoryDAO.getCategoryForId(categoryId);
        }

        model.addAttribute("category", category);

        return "admin/category/category_edit";
    }

    @PostMapping({"/categoryedit", "/categoryedit/{id}"})
    public String categorySubmit(@PathVariable(value = "id", required = false) Long categoryId, @Valid @ModelAttribute Category category, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            return "admin/category/category_edit";
        }

        categoryDAO.saveOrUpdateCategory(category);

        return "redirect:/admin/categoryedit/" + category.getId();
    }

    @GetMapping({"/categorydelete/{id}"})
    public String timeSeriesDelete(@PathVariable(value = "id", required = true) Long categoryId)
    {
        Category category = categoryDAO.getCategoryForId(categoryId);
        categoryDAO.deleteCategory(category);

        return "redirect:/admin/category";
    }
}
