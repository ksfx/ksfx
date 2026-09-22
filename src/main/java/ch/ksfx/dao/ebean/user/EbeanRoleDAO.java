package ch.ksfx.dao.ebean.user;

import ch.ksfx.dao.user.RoleDAO;
import ch.ksfx.model.user.Role;
import io.ebean.Ebean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EbeanRoleDAO implements RoleDAO
{
    @Override
    public Role getRoleForId(Long roleId)
    {
        return Ebean.find(Role.class, roleId);
    }

    @Override
    public List<Role> getAllRoles()
    {
        return Ebean.find(Role.class).order().asc("name").findList();
    }
}
