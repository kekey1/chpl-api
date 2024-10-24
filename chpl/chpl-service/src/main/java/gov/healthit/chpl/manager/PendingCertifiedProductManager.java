package gov.healthit.chpl.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.ff4j.FF4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.healthit.chpl.FeatureList;
import gov.healthit.chpl.dao.PendingCertifiedProductDAO;
import gov.healthit.chpl.dao.auth.UserDAO;
import gov.healthit.chpl.domain.CQMCriterion;
import gov.healthit.chpl.domain.CQMResultDetails;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.PendingCertifiedProductDetails;
import gov.healthit.chpl.domain.activity.ActivityConcept;
import gov.healthit.chpl.domain.auth.User;
import gov.healthit.chpl.dto.auth.UserDTO;
import gov.healthit.chpl.dto.listing.pending.PendingCertificationResultDTO;
import gov.healthit.chpl.dto.listing.pending.PendingCertifiedProductDTO;
import gov.healthit.chpl.dto.listing.pending.PendingCertifiedProductMetadataDTO;
import gov.healthit.chpl.entity.listing.pending.PendingCertifiedProductEntity;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.exception.ObjectMissingValidationException;
import gov.healthit.chpl.exception.UserRetrievalException;
import gov.healthit.chpl.manager.impl.SecuredManager;
import gov.healthit.chpl.optionalStandard.dao.OptionalStandardDAO;
import gov.healthit.chpl.optionalStandard.domain.OptionalStandard;
import gov.healthit.chpl.optionalStandard.domain.OptionalStandardCriteriaMap;
import gov.healthit.chpl.service.CuresUpdateService;
import gov.healthit.chpl.util.CertificationResultRules;
import gov.healthit.chpl.validation.listing.ListingValidatorFactory;
import gov.healthit.chpl.validation.listing.PendingValidator;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class PendingCertifiedProductManager extends SecuredManager {
    private CertificationResultRules certRules;
    private ListingValidatorFactory validatorFactory;
    private PendingCertifiedProductDAO pcpDao;
    private TestingFunctionalityManager testFunctionalityManager;
    private OptionalStandardDAO optionalStandardDAO;
    private UserDAO userDAO;
    private ActivityManager activityManager;
    private CuresUpdateService curesUpdateService;
    private DimensionalDataManager dimensionalDataManager;
    private FF4j ff4j;

    @Autowired
    public PendingCertifiedProductManager(CertificationResultRules certRules,
            ListingValidatorFactory validatorFactory,
            PendingCertifiedProductDAO pcpDao,
            TestingFunctionalityManager testFunctionalityManager,
            OptionalStandardDAO optionalStandardDAO,
            UserDAO userDAO,
            ActivityManager activityManager,
            CuresUpdateService curesUpdateService,
            DimensionalDataManager dimensionalDataManager,
            FF4j ff4j) {

        this.certRules = certRules;
        this.validatorFactory = validatorFactory;
        this.pcpDao = pcpDao;
        this.testFunctionalityManager = testFunctionalityManager;
        this.optionalStandardDAO = optionalStandardDAO;
        this.userDAO = userDAO;
        this.activityManager = activityManager;
        this.curesUpdateService = curesUpdateService;
        this.dimensionalDataManager = dimensionalDataManager;
        this.ff4j = ff4j;
    }

    @Transactional(readOnly = true)
    @PostAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_DETAILS_BY_ID,"
            + "returnObject)")
    public PendingCertifiedProductDetails getById(final Long id)
            throws EntityRetrievalException, AccessDeniedException {
        return getById(id, false);
    }

    /**
     * ROLE_ONC is allowed to see pending listings only for activity and no other times.
     */

    @Transactional(readOnly = true)
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_DETAILS_BY_ID_FOR_ACTIVITY)")
    public PendingCertifiedProductDetails getByIdForActivity(final Long id)
            throws EntityRetrievalException, AccessDeniedException {
        return getById(id, true);
    }

    @Transactional(readOnly = true)
    @PostAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_DETAILS_BY_ID,"
            + "returnObject)")
    public PendingCertifiedProductDetails getById(final Long id, final boolean includeRetired)
            throws EntityRetrievalException, AccessDeniedException {

        PendingCertifiedProductDTO pendingCp = pcpDao.findById(id, includeRetired);

        // the user has permission so continue getting the pending cp
        updateCertResults(pendingCp);
        validate(pendingCp);

        PendingCertifiedProductDetails pcpDetails = new PendingCertifiedProductDetails(pendingCp);
        pcpDetails.setCuresUpdate(curesUpdateService.isCuresUpdate(pcpDetails));
        addAllVersionsToCmsCriterion(pcpDetails);
        addAvailableTestFunctionalities(pcpDetails);
        addAvailableOptionalStandards(pcpDetails);
        return pcpDetails;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_ALL_METADATA)")
    @PostFilter("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_ALL_METADATA, filterObject)")
    public List<PendingCertifiedProductMetadataDTO> getAllPendingCertifiedProductMetadata() {
        List<PendingCertifiedProductMetadataDTO> products = pcpDao.getAllMetadata();
        return products;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_ALL)")
    public List<PendingCertifiedProductDTO> getAllPendingCertifiedProducts() {
        List<PendingCertifiedProductDTO> products = pcpDao.findAll();
        updateCertResults(products);
        validate(products);

        return products;
    }

    /**
     * This method is included so that the pending listings may be pre-loaded in a background cache without having to
     * duplicate manager logic. Prefer users of this class to call getPendingCertifiedProductsCached.
     *
     * @param acbId
     * @return
     */

    @Transactional(readOnly = true)
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).GET_BY_ACB, #acbId)")
    public List<PendingCertifiedProductDTO> getPendingCertifiedProducts(final Long acbId) {
        List<PendingCertifiedProductDTO> products = pcpDao.findByAcbId(acbId);
        updateCertResults(products);
        validate(products);

        return products;
    }

    @Transactional
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).UPDATEABLE, #acbId)")
    public boolean markAsProcessingIfAvailable(Long acbId, Long pendingListingId) throws EntityRetrievalException{
        if (!pcpDao.isProcessingOrDeleted(pendingListingId)) {
            pcpDao.updateProcessingFlag(pendingListingId, true);
            return true;
        }
        return false;
    }

    @Transactional
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).UPDATEABLE, #acbId)")
    public void markAsNotProcessing(Long acbId, Long pendingListingId) throws EntityRetrievalException{
        pcpDao.updateProcessingFlag(pendingListingId, false);
    }

    @Transactional(rollbackFor = {
            EntityRetrievalException.class, EntityCreationException.class, JsonProcessingException.class
    })
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).CREATE_OR_REPLACE, #toCreate)")
    public PendingCertifiedProductDTO createOrReplace(PendingCertifiedProductEntity toCreate)
            throws EntityRetrievalException, EntityCreationException, JsonProcessingException {
        Long existingId = pcpDao.findIdByOncId(toCreate.getUniqueId());
        if (existingId != null) {
            pcpDao.delete(existingId);
        }

        PendingCertifiedProductDTO pendingCpDto = null;
        try {
            // insert the record
            pendingCpDto = pcpDao.create(toCreate);
            updateCertResults(pendingCpDto);
            validate(pendingCpDto);
        } catch (Exception ex) {
            // something unexpected happened on upload make sure the user gets an appropriate error message
            String message = "An unexpected error occurred. "
                    + "Please review the information in your upload file. The CHPL team has been notified.";
            LOGGER.error(message);
            EntityCreationException toThrow = new EntityCreationException(message);
            toThrow.setStackTrace(ex.getStackTrace());
            throw toThrow;
        }

        String activityMsg = "Certified product " + pendingCpDto.getProductName() + " is pending.";
        activityManager.addActivity(ActivityConcept.PENDING_CERTIFIED_PRODUCT, pendingCpDto.getId(),
                activityMsg, null, pendingCpDto);

        return pendingCpDto;
    }

    @Transactional
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).DELETE, #pendingProductId)")
    public void deletePendingCertifiedProduct(final Long pendingProductId)
            throws EntityRetrievalException, EntityNotFoundException, EntityCreationException, AccessDeniedException,
            JsonProcessingException, ObjectMissingValidationException {

        PendingCertifiedProductDTO pendingCp = pcpDao.findById(pendingProductId, true);
        // dao throws entity not found exception if bad id
        if (pendingCp != null) {
            if (isPendingListingAvailableForUpdate(pendingCp)) {
                pcpDao.delete(pendingProductId);
                String activityMsg = "Pending certified product " + pendingCp.getProductName() + " has been rejected.";
                activityManager.addActivity(ActivityConcept.PENDING_CERTIFIED_PRODUCT, pendingCp.getId(),
                        activityMsg, pendingCp, null);
            }
        }
    }

    @Transactional
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).CONFIRM, #acbId)")
    public void confirm(final Long acbId, final Long pendingProductId)
            throws EntityRetrievalException, JsonProcessingException, EntityCreationException {
        PendingCertifiedProductDTO pendingCp = pcpDao.findById(pendingProductId, true);
        pcpDao.delete(pendingProductId);

        String activityMsg = "Pending certified product " + pendingCp.getProductName() + " has been confirmed.";
        activityManager.addActivity(ActivityConcept.PENDING_CERTIFIED_PRODUCT, pendingCp.getId(),
                activityMsg, pendingCp, pendingCp);

    }

    @Transactional
    @PreAuthorize("@permissions.hasAccess(T(gov.healthit.chpl.permissions.Permissions).PENDING_CERTIFIED_PRODUCT, "
            + "T(gov.healthit.chpl.permissions.domains.PendingCertifiedProductDomainPermissions).UPDATEABLE, #acbId)")
    public boolean isPendingListingAvailableForUpdate(Long acbId, Long pendingProductId)
            throws EntityRetrievalException, ObjectMissingValidationException {
        PendingCertifiedProductDTO pendingCp = pcpDao.findById(pendingProductId, true);
        return isPendingListingAvailableForUpdate(pendingCp);
    }

    private boolean isPendingListingAvailableForUpdate(final PendingCertifiedProductDTO pendingCp)
            throws EntityRetrievalException, ObjectMissingValidationException {
        if (pendingCp.getDeleted().booleanValue()) {
            ObjectMissingValidationException alreadyDeletedEx = new ObjectMissingValidationException();
            alreadyDeletedEx.getErrorMessages()
                    .add("This pending certified product has already been confirmed or rejected by another user.");
            alreadyDeletedEx.setObjectId(pendingCp.getUniqueId());

            try {
                UserDTO lastModifiedUserDto = userDAO.getById(pendingCp.getLastModifiedUser());
                if (lastModifiedUserDto != null) {
                    User lastModifiedUser = new User(lastModifiedUserDto);
                    alreadyDeletedEx.setUser(lastModifiedUser);
                } else {
                    alreadyDeletedEx.setUser(null);
                }
            } catch (final UserRetrievalException ex) {
                alreadyDeletedEx.setUser(null);
            }

            throw alreadyDeletedEx;
        } else {
            // If pendingCP were null, we would have gotten an NPE by this point
            return true;
        }
    }

    private void updateCertResults(final PendingCertifiedProductDTO dto) {
        List<PendingCertifiedProductDTO> products = new ArrayList<PendingCertifiedProductDTO>();
        products.add(dto);
        updateCertResults(products);
    }

    private void updateCertResults(final List<PendingCertifiedProductDTO> products) {
        for (PendingCertifiedProductDTO product : products) {
            for (PendingCertificationResultDTO certResult : product.getCertificationCriterion()) {
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.GAP)) {
                    certResult.setGap(null);
                } else if (certResult.getGap() == null) {
                    certResult.setGap(Boolean.FALSE);
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.G1_SUCCESS)) {
                    certResult.setG1Success(null);
                } else if (certResult.getG1Success() == null) {
                    certResult.setG1Success(Boolean.FALSE);
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.G2_SUCCESS)) {
                    certResult.setG2Success(null);
                } else if (certResult.getG2Success() == null) {
                    certResult.setG2Success(Boolean.FALSE);
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.API_DOCUMENTATION)) {
                    certResult.setApiDocumentation(null);
                } else if (certResult.getApiDocumentation() == null) {
                    certResult.setApiDocumentation("");
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(),
                        CertificationResultRules.EXPORT_DOCUMENTATION)) {
                    certResult.setExportDocumentation(null);
                } else if (certResult.getExportDocumentation() == null) {
                    certResult.setExportDocumentation("");
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.DOCUMENTATION_URL)) {
                    certResult.setDocumentationUrl(null);
                } else if (certResult.getDocumentationUrl() == null) {
                    certResult.setDocumentationUrl("");
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.USE_CASES)) {
                    certResult.setUseCases(null);
                } else if (certResult.getUseCases() == null) {
                    certResult.setUseCases("");
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.SERVICE_BASE_URL_LIST)) {
                    certResult.setServiceBaseUrlList(null);
                } else if (certResult.getServiceBaseUrlList() == null) {
                    certResult.setServiceBaseUrlList("");
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.PRIVACY_SECURITY)) {
                    certResult.setPrivacySecurityFramework(null);
                } else if (certResult.getPrivacySecurityFramework() == null) {
                    certResult.setPrivacySecurityFramework("");
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(),
                        CertificationResultRules.ATTESTATION_ANSWER)) {
                    certResult.setAttestationAnswer(null);
                } else if (certResult.getAttestationAnswer() == null) {
                    certResult.setAttestationAnswer(Boolean.FALSE);
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.SED)) {
                    certResult.setSed(null);
                } else if (certResult.getSed() == null) {
                    certResult.setSed(Boolean.FALSE);
                }

                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.UCD_FIELDS)) {
                    certResult.setUcdProcesses(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(),
                        CertificationResultRules.ADDITIONAL_SOFTWARE)) {
                    certResult.setAdditionalSoftware(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(),
                        CertificationResultRules.FUNCTIONALITY_TESTED)) {
                    certResult.setTestFunctionality(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.OPTIONAL_STANDARD)) {
                    certResult.setOptionalStandards(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.STANDARDS_TESTED)) {
                    certResult.setTestStandards(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.TEST_DATA)) {
                    certResult.setTestData(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.TEST_PROCEDURE)) {
                    certResult.setTestProcedures(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.TEST_TOOLS_USED)) {
                    certResult.setTestTools(null);
                }
                if (!certRules.hasCertOption(certResult.getCriterion().getNumber(), CertificationResultRules.TEST_TASK)) {
                    certResult.setTestTasks(null);
                }
            }
        }
    }

    private List<CQMCriterion> getAvailableCQMVersions() {
        List<CQMCriterion> criteria = new ArrayList<CQMCriterion>();

        for (CQMCriterion criterion : dimensionalDataManager.getCQMCriteria()) {
            if (!StringUtils.isEmpty(criterion.getCmsId()) && criterion.getCmsId().startsWith("CMS")) {
                criteria.add(criterion);
            }
        }
        return criteria;
    }

    private void validate(final List<PendingCertifiedProductDTO> products) {
        for (PendingCertifiedProductDTO dto : products) {
            PendingValidator validator = validatorFactory.getValidator(dto);
            if (validator != null) {
                validator.validate(dto);
            }
        }
    }

    private void validate(final PendingCertifiedProductDTO... products) {
        for (PendingCertifiedProductDTO dto : products) {
            PendingValidator validator = validatorFactory.getValidator(dto);
            if (validator != null) {
                validator.validate(dto);
            }
        }
    }

    public void addAllVersionsToCmsCriterion(final PendingCertifiedProductDetails pcpDetails) {
        // now add allVersions for CMSs
        String certificationEdition = pcpDetails.getCertificationEdition().get(CertifiedProductSearchDetails.EDITION_NAME_KEY)
                .toString();
        if (!certificationEdition.startsWith("2011")) {
            List<CQMCriterion> cqms = getAvailableCQMVersions();
            for (CQMCriterion cqm : cqms) {
                boolean cqmExists = false;
                for (CQMResultDetails details : pcpDetails.getCqmResults()) {
                    if (cqm.getCmsId().equals(details.getCmsId())) {
                        cqmExists = true;
                        details.getAllVersions().add(cqm.getCqmVersion());
                    }
                }
                if (!cqmExists) {
                    CQMResultDetails result = new CQMResultDetails();
                    result.setCmsId(cqm.getCmsId());
                    result.setNqfNumber(cqm.getNqfNumber());
                    result.setNumber(cqm.getNumber());
                    result.setTitle(cqm.getTitle());
                    result.setDescription(cqm.getDescription());
                    result.setSuccess(Boolean.FALSE);
                    result.getAllVersions().add(cqm.getCqmVersion());
                    result.setTypeId(cqm.getCqmCriterionTypeId());
                    pcpDetails.getCqmResults().add(result);
                }
            }
        }
    }

    public void addAvailableTestFunctionalities(final PendingCertifiedProductDetails pcpDetails) {
        // now add allMeasures for criteria
        for (CertificationResult cert : pcpDetails.getCertificationResults()) {
            String edition = pcpDetails.getCertificationEdition().get(CertifiedProductSearchDetails.EDITION_NAME_KEY).toString();
            Long practiceTypeId = null;
            if (pcpDetails.getPracticeType().containsKey("id")) {
                if (pcpDetails.getPracticeType().get("id") != null) {
                    practiceTypeId = Long.valueOf(pcpDetails.getPracticeType().get("id").toString());
                }
            }
            cert.setAllowedTestFunctionalities(
                    testFunctionalityManager.getTestFunctionalities(cert.getCriterion().getId(), edition, practiceTypeId));
        }
    }

    public void addAvailableOptionalStandards(PendingCertifiedProductDetails pcpDetails) {
        // now add allMeasures for criteria
        List<OptionalStandardCriteriaMap> optionalStandardCriteriaMap;
        try {
            optionalStandardCriteriaMap = optionalStandardDAO.getAllOptionalStandardCriteriaMap();
            for (CertificationResult cert : pcpDetails.getCertificationResults()) {
                cert.setAllowedOptionalStandards(getAvailableOptionalStandardsForCriteria(cert, optionalStandardCriteriaMap));
            }
        } catch (EntityRetrievalException e) {
            LOGGER.info("Error retrieving OS-criteria map", e.getMessage());
        }
    }

    private List<OptionalStandard> getAvailableOptionalStandardsForCriteria(CertificationResult result, List<OptionalStandardCriteriaMap> optionalStandardCriteriaMap) {
        return optionalStandardCriteriaMap.stream()
                .filter(osm -> ff4j.check(FeatureList.OPTIONAL_STANDARDS) && osm.getCriterion().getId().equals(result.getCriterion().getId()))
                .map(osm -> osm.getOptionalStandard())
                .collect(Collectors.toList());
    }
}
