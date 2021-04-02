package ch.ksfx.controller.admin.user;

import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.user.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
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
                                       @RequestParam(value = "reTypePassword", required = true) String reTypePassword)
    {
        if (password != null && reTypePassword != null && password.equals(reTypePassword)) {
            System.out.println("CHANGE PASSWORD");

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            User user = userDAO.getUser(ADMIN_USER);
            user.setPassword(encoder.encode(password));
            userDAO.save(user);
        }

        return "admin/user/change_password";
    }
}
