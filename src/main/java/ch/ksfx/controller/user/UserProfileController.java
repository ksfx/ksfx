package ch.ksfx.controller.user;

import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.user.User;
import ch.ksfx.services.project.UserScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Self-service profile (first name, last name, e-mail) behind the header's user dropdown - always
 * the logged-in user, never an id from the request, so it needs no authority beyond being logged
 * in. Lives under /user/** - its own path outside /admin/**, so the ADMIN-only URL rule needs no
 * exception and the NoProjectInterceptor lets it through for users without a project. The login
 * name stays read-only here: it is the identity everywhere (attribution, assignee keys,
 * remoteUser in the header) and remains an administrator's change in User Management.
 */
@Controller
@RequestMapping("/user")
public class UserProfileController
{
    private final UserDAO userDAO;
    private final UserScope userScope;

    public UserProfileController(UserDAO userDAO, UserScope userScope)
    {
        this.userDAO = userDAO;
        this.userScope = userScope;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices rememberMeServices;

    /**
     * "Auf allen Geräten abmelden": removes every remember-me token of this user (all devices lose
     * "Angemeldet bleiben") and ends the current session. Other devices with a session that is still
     * open keep it until it ends (idle timeout or the next app restart) - sessions live in memory and
     * are not tracked per user; after that they need the password again.
     */
    @PostMapping("/logout-everywhere")
    public String logoutEverywhere(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
    {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        rememberMeServices.logout(request, response, authentication); // removes ALL tokens of the user + clears this device's cookie
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request, response, authentication);

        return "redirect:/login?logout";
    }

    @GetMapping("/profile")
    public String profile(Model model)
    {
        model.addAttribute("user", userScope.currentUser());

        return "user/profile";
    }

    @PostMapping("/profile")
    public String save(@RequestParam(required = false) String firstName,
                       @RequestParam(required = false) String lastName,
                       @RequestParam(required = false) String email,
                       Model model)
    {
        User user = userScope.currentUser();

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            model.addAttribute("user", user);
            model.addAttribute("error", "Please enter a valid e-mail address.");

            return "user/profile";
        }

        user.setFirstName(firstName != null ? firstName.trim() : "");
        user.setLastName(lastName != null ? lastName.trim() : "");
        user.setEmail(email.trim());
        userDAO.save(user);

        model.addAttribute("user", user);
        model.addAttribute("success", "Profile saved.");

        return "user/profile";
    }
}
