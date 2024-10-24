package gov.healthit.chpl.permissions.domains.surveillance.report;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.permissions.domains.ActionPermissions;
import gov.healthit.chpl.surveillance.report.AnnualReportDAO;
import gov.healthit.chpl.surveillance.report.domain.AnnualReport;

@Component("surveillanceReportExportAnnualReportActionPermissions")
public class ExportAnnualReportActionPermissions extends ActionPermissions {

    @Autowired
    private AnnualReportDAO annualReportDao;

    @Override
    public boolean hasAccess() {
        return false;
    }

    @Override
    public boolean hasAccess(Object obj) {
        if (!(obj instanceof Long)) {
            return false;
        } else if (getResourcePermissions().isUserRoleAdmin()
                || getResourcePermissions().isUserRoleOnc()
                || getResourcePermissions().isUserRoleOncStaff()) {
            return true;
        } else if (getResourcePermissions().isUserRoleAcbAdmin()) {
            Long idToExport = (Long) obj;
            AnnualReport toExport = null;
            try {
                toExport = annualReportDao.getById(idToExport);
                return isAcbValidForCurrentUser(toExport.getAcb().getId());
            } catch (EntityRetrievalException ex) {
                return false;
            }
        } else {
            return false;
        }
    }

}
