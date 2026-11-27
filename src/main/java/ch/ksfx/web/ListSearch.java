package ch.ksfx.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The search of a list page (activities, publishing configurations): free text, optionally also in
 * the code, and the category as one part of it. Kept in the session per list and project, so the
 * existing pagination and sort links - which carry no search parameters - keep the search, and a
 * category of another project never filters this one. The search form always sends "search=1";
 * without it the stored search applies.
 */
public class ListSearch implements Serializable
{
    private String query;
    private Long categoryId;
    private boolean inCode;

    /** Reads the submitted search (and stores it) or, for paging/sorting links, the stored one. */
    @SuppressWarnings("unchecked")
    public static ListSearch resolve(HttpServletRequest request, String listKey, Long projectId)
    {
        HttpSession session = request.getSession();
        String attribute = "ksfxListSearch." + listKey;
        Map<Long, ListSearch> byProject = (Map<Long, ListSearch>) session.getAttribute(attribute);

        if (byProject == null) {
            byProject = new HashMap<>();
        }

        ListSearch search;

        if (request.getParameter("search") != null) {
            search = new ListSearch();
            String query = request.getParameter("q");
            search.query = query != null && !query.trim().isEmpty() ? query.trim() : null;
            search.inCode = request.getParameter("code") != null;

            try {
                String category = request.getParameter("category");
                search.categoryId = category != null && !category.isEmpty() && !"0".equals(category) ? Long.valueOf(category) : null;
            } catch (NumberFormatException e) {
                search.categoryId = null;
            }

            byProject.put(projectId, search);
            session.setAttribute(attribute, byProject);
        } else {
            search = byProject.getOrDefault(projectId, new ListSearch());
        }

        return search;
    }

    public String getQuery() { return query; }
    public Long getCategoryId() { return categoryId; }
    public boolean isInCode() { return inCode; }

    public boolean isActive()
    {
        return query != null || categoryId != null;
    }
}
