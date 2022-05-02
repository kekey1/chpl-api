package gov.healthit.chpl.permissions.domains.attestation;

import org.springframework.stereotype.Component;

import gov.healthit.chpl.attestation.domain.DeveloperAttestationSubmission;
import gov.healthit.chpl.permissions.domains.ActionPermissions;

@Component("attestationCreateActionPermissions")
public class CreateActionPermissions extends ActionPermissions {

    @Override
    public boolean hasAccess() {
        return false;
    }

    @Override
    public boolean hasAccess(Object obj) {
        if (!(obj instanceof DeveloperAttestationSubmission)) {
            return false;
        } else if (getResourcePermissions().isUserRoleAdmin() || getResourcePermissions().isUserRoleOnc()) {
            return true;
        } else if (getResourcePermissions().isUserRoleAcbAdmin()) {
            DeveloperAttestationSubmission das = (DeveloperAttestationSubmission) obj;
            return isCurrentAcbUserAssociatedWithDeveloper(das.getDeveloper().getDeveloperId());
        } else {
            return false;
        }
    }

}