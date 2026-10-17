package ch.ksfx.controller.admin.user;

import ch.ksfx.dao.user.RoleDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.user.Role;
import ch.ksfx.model.user.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Full user CRUD + per-user role assignment under Admin -> User Management (see KSFX issue #3) -
 * until now the only self-service was ChangePasswordController (own password only) and everything
 * else meant SQL by hand. Same house style as ApiClientController.
 *
 * Deliberately NOT @ModelAttribute-bound to the User entity: User doubles as the Spring Security
 * UserDetails principal, and binding a form straight onto it would invite both the known
 * field-wiping trap (see ApiClientController's comment) and mass-assignment of fields no form
 * should ever set (password hash, id of someone else, ...). Explicit @RequestParams only.
 *
 * Password handling: bcrypt via the same BCryptPasswordEncoder the login path uses; on edit an
 * empty password field means "keep the current one". Guard rails: you cannot delete or disable
 * your own account (locking yourself out of the only admin UI that could undo it).
 */
@Controller
@RequestMapping("/admin/usermanagement")
public class UserManagementController
{
    private final UserDAO userDAO;
    private final RoleDAO roleDAO;

    public UserManagementController(UserDAO userDAO, RoleDAO roleDAO)
    {
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
    }

    @GetMapping("/")
    public String index(Model model)
    {
        model.addAttribute("users", userDAO.getAllUsers());
        model.addAttribute("currentUserId", currentUserId());

        return "admin/user/user_list";
    }

    @GetMapping({"/edit", "/edit/{id}"})
    public String edit(@PathVariable(value = "id", required = false) Long id, Model model)
    {
        User user = id != null ? userDAO.getUser(id) : null;

        if (id != null && user == null) {
            return "redirect:/admin/usermanagement/";
        }

        Set<Long> assignedRoleIds = new HashSet<>();

        if (user != null) {
            for (Role role : user.getRoles()) {
                assignedRoleIds.add(role.getId());
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("isNew", user == null);
        model.addAttribute("allRoles", roleDAO.getAllRoles());
        model.addAttribute("assignedRoleIds", assignedRoleIds);

        return "admin/user/user_edit";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String username,
                       @RequestParam(defaultValue = "") String firstName,
                       @RequestParam(defaultValue = "") String lastName,
                       @RequestParam(defaultValue = "") String email,
                       @RequestParam(defaultValue = "") String password,
                       @RequestParam(defaultValue = "") String reTypePassword,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       @RequestParam(required = false, name = "roleIds") List<Long> roleIds,
                       RedirectAttributes redirectAttributes)
    {
        boolean isNew = id == null;
        User user = isNew ? new User() : userDAO.getUser(id);

        if (user == null) {
            return "redirect:/admin/usermanagement/";
        }

        String error = validate(user, isNew, username, password, reTypePassword, enabled);

        if (error != null) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", error);
            return isNew ? "redirect:/admin/usermanagement/edit" : "redirect:/admin/usermanagement/edit/" + id;
        }

        user.setUsername(username.trim());
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email.trim());
        user.setEnabled(enabled);

        if (!password.isEmpty()) {
            user.setPassword(new BCryptPasswordEncoder().encode(password));
        }

        Set<Role> roles = new HashSet<>();

        if (roleIds != null) {
            for (Long roleId : roleIds) {
                Role role = roleDAO.getRoleForId(roleId);

                if (role != null) {
                    roles.add(role);
                }
            }
        }

        user.setRoles(roles);
        userDAO.save(user);

        redirectAttributes.addFlashAttribute("resultMessage", isNew ? "User created." : "User saved.");
        return "redirect:/admin/usermanagement/";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes)
    {
        if (id.equals(currentUserId())) {
            redirectAttributes.addFlashAttribute("resultError", true);
            redirectAttributes.addFlashAttribute("resultMessage", "You cannot delete your own account.");
            return "redirect:/admin/usermanagement/";
        }

        User user = userDAO.getUser(id);

        if (user != null) {
            userDAO.delete(user);
            redirectAttributes.addFlashAttribute("resultMessage", "User deleted.");
        }

        return "redirect:/admin/usermanagement/";
    }

    private String validate(User user, boolean isNew, String username, String password, String reTypePassword, boolean enabled)
    {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required.";
        }

        User existing = userDAO.getUserByName(username.trim());

        if (existing != null && (isNew || !existing.getId().equals(user.getId()))) {
            return "A user with that username already exists.";
        }

        if (isNew && password.isEmpty()) {
            return "A new user needs a password.";
        }

        // Same 5+ chars rule as ChangePasswordController; empty on edit = keep current password.
        if (!password.isEmpty() && (password.length() <= 4 || !password.equals(reTypePassword))) {
            return "Password must be longer than 4 characters and both fields must match.";
        }

        if (!isNew && user.getId().equals(currentUserId()) && !enabled) {
            return "You cannot disable your own account.";
        }

        return null;
    }

    private Long currentUserId()
    {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        return principal instanceof User ? ((User) principal).getId() : null;
    }
}
