package ch.ksfx.controller.admin.user;

import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.user.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/user")
public class ChangePasswordController
{
    private static final Long ADMIN_USER = 1l;

    UserDAO userDAO;

    public ChangePasswordController(UserDAO userDAO)
    {
        this.userDAO = userDAO;
    }

    @GetMapping("/changepassword")
    public String changePassword()
    {
        return "admin/user/change_password";
    }

    @PostMapping("/changepassword")
    public String changePasswordSubmit(@RequestParam(value = "password", required = true) String password,
                                       @RequestParam(value = "reTypePassword", required = true) String reTypePassword,
                                       Model model)
    {

        if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated() && !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) && SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null) {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            System.out.println("Current User: " + currentUser.toString());
            System.out.println("Current User ID: " + currentUser.getId());

            if (password != null && password.length() > 4 && reTypePassword != null && password.equals(reTypePassword)) {
                System.out.println("CHANGE PASSWORD");

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                User user = userDAO.getUser(currentUser.getId());
                user.setPassword(encoder.encode(password));
                userDAO.save(user);

                model.addAttribute("success", "Password changed");
            } else {
                model.addAttribute("error", "Password could not be changed, make sure it has more than 4 chars and that both passwords match");
            }
        } else {
            model.addAttribute("error", "Password could not be changed, make sure it has more than 4 chars");
        }

        return "admin/user/change_password";
    }
}
