package ch.ksfx.services.project;

import ch.ksfx.model.user.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A logged-in user without any project membership (and without the ADMIN role) is sent to the
 * "no project assigned" page before any project-scoped controller runs - those rely on a
 * non-null current project (concept, Q3: the account is valid, it just has nothing to see yet).
 */
@Component
public class NoProjectInterceptor implements HandlerInterceptor
{
    private final UserScope userScope;

    public NoProjectInterceptor(UserScope userScope)
    {
        this.userScope = userScope;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        User user = userScope.currentUser();

        if (user == null || userScope.isAdmin(user) || !userScope.accessibleProjects(user).isEmpty()) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/noproject");

        return false;
    }
}
