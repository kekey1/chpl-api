package gov.healthit.chpl.permissions.domains.secureduser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.dto.auth.UserDTO;
import gov.healthit.chpl.dto.auth.UserPermissionDTO;
import gov.healthit.chpl.exception.UserRetrievalException;
import gov.healthit.chpl.permissions.domains.ActionPermissions;

/**
 * Permission for impersonating another user.
 * @author alarned
 *
 */
@Component(value = "userPermissionsImpersonateUserActionPermissions")
public class ImpersonateUserActionPermissions extends ActionPermissions {
    private static final Logger LOGGER = LogManager.getLogger(ImpersonateUserActionPermissions.class);

    @Override
    public boolean hasAccess() {
        return false;
    }

    /**
     * Compare acting user's role with targeted user's role.
     * ROLE_ADMIN > all other roles
     * ROLE_ADMIN is not > ROLE_ADMIN
     * ROLE_ONC > all roles except ROLE_ADMIN and ROLE_ONC
     */
    @Override
    public boolean hasAccess(final Object obj) {
        if (!getResourcePermissions().isUserRoleAdmin() && !getResourcePermissions().isUserRoleOnc()) {
            return false;
        }
        UserDTO target;
        try {
            target = getResourcePermissions().getUserByName((String) obj);
        } catch (UserRetrievalException e) {
            LOGGER.error("Unable to get user by name %s", (String) obj);
            return false;
        }
        if (getResourcePermissions().isUserRoleAdmin() && !getResourcePermissions().isUserRoleOnc()) {
            for (UserPermissionDTO t : getResourcePermissions().getPermissionsByUserId(target.getId())) {
                if (t.getName().equalsIgnoreCase("ADMIN")) {
                    return false;
                }
            }
            return true;
        }
        for (UserPermissionDTO t : getResourcePermissions().getPermissionsByUserId(target.getId())) {
            if (t.getName().equalsIgnoreCase("ADMIN") || t.getName().equalsIgnoreCase("ONC")) {
                return false;
            }
        }
        return true;
    }
}
