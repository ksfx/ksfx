package ch.ksfx.dao.user;

import ch.ksfx.model.user.Role;

import java.util.List;

public interface RoleDAO
{
    public Role getRoleForId(Long roleId);

    /** All defined roles (seeded: USER, ADMIN) - backs the role checkboxes in the user management UI. */
    public List<Role> getAllRoles();
}
