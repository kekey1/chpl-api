package gov.healthit.chpl.scheduler.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Matcher;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.KeyMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.auth.user.JWTAuthenticatedUser;
import gov.healthit.chpl.dao.CertificationCriterionDAO;
import gov.healthit.chpl.dao.CertifiedProductDAO;
import gov.healthit.chpl.dao.impl.BaseDAOImpl;
import gov.healthit.chpl.domain.concept.CertificationEditionConcept;
import gov.healthit.chpl.dto.CertificationCriterionDTO;
import gov.healthit.chpl.dto.CertifiedProductDetailsDTO;
import gov.healthit.chpl.dto.MacraMeasureDTO;
import gov.healthit.chpl.dto.TestDataDTO;
import gov.healthit.chpl.dto.TestProcedureDTO;
import gov.healthit.chpl.entity.CertificationCriterionEntity;
import gov.healthit.chpl.entity.MacraMeasureEntity;
import gov.healthit.chpl.entity.TestDataCriteriaMapEntity;
import gov.healthit.chpl.entity.TestDataEntity;
import gov.healthit.chpl.entity.TestProcedureCriteriaMapEntity;
import gov.healthit.chpl.entity.TestProcedureEntity;
import gov.healthit.chpl.entity.listing.pending.PendingCertifiedProductEntity;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.manager.CertifiedProductDetailsManager;
import gov.healthit.chpl.manager.PendingCertifiedProductManager;
import gov.healthit.chpl.scheduler.ChplSchedulerReference;
import gov.healthit.chpl.scheduler.job.extra.JobResponseTriggerListener;
import gov.healthit.chpl.scheduler.job.extra.JobResponseTriggerWrapper;
import gov.healthit.chpl.util.AuthUtil;
import net.sf.ehcache.CacheManager;

public class AddCriteriaTo2015ListingsJob extends QuartzJob {
    private static final Logger LOGGER = LogManager.getLogger("addCriteriaToListingsJobLogger");
    private static final String JOB_NAME_FOR_EXISTING_LISTINGS = "addCriteriaToSingleListingJob";
    private static final String JOB_NAME_FOR_PENDING_LISTINGS = "addCriteriaToSinglePendingListingJob";
    private static final String JOB_GROUP = "subordinateJobs";
    private static final long ADMIN_ID = -2L;

    @Autowired
    private CertificationCriterionDAO criterionDAO;

    @Autowired
    private ExtendedCertificationCriterionDao extendedCriterionDAO;

    @Autowired
    private InsertableMacraMeasureDao insertableMmDao;

    @Autowired
    private InsertableTestDataDao insertableTestDataDao;

    @Autowired
    private InsertableTestProcedureDao insertableTestProcDao;

    @Autowired
    private CertifiedProductDAO certifiedProductDAO;

    @Autowired
    private PendingCertifiedProductDaoIdsOnly pendingCertifiedProductDAO;

    @Autowired
    private ChplSchedulerReference chplScheduler;

    @Autowired
    private CertifiedProductDetailsManager certifiedProductDetailsManager;

    @Autowired
    private PendingCertifiedProductManager pcpManager;

    @Autowired
    private Environment env;

    private static final String CRITERIA_TO_ADD = "170.315 (b)(10):Clinical Information Export;"
            + "170.315 (d)(12):Encrypt Authentication Credentials;"
            + "170.315 (d)(13):Multi-Factor Authentication;"
            + "170.315 (g)(10):Standardized API for Patient and Population Services";

    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        LOGGER.info("********* Starting the Add Criteria to 2015 Listings job. *********");

        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        setSecurityContext();
        LOGGER.info("statusInterval = " + jobContext.getMergedJobDataMap().getInt("statusInterval"));
        addCriteria();
        addTestDataMaps();
        addTestProcedureMaps();
        addMacraMeasureMaps();

        List<JobResponseTriggerWrapper> wrappers = new ArrayList<JobResponseTriggerWrapper>();
        wrappers.addAll(getExistingListingWrappers(jobContext));
        wrappers.addAll(getPendingListingWrappers(jobContext));

        LOGGER.info("Total number of listings to update: " + wrappers.size());

        try {
            JobResponseTriggerListener listener = new JobResponseTriggerListener(
                    wrappers,
                    jobContext.getMergedJobDataMap().getString("email"),
                    jobContext.getMergedJobDataMap().getString("emailCsvFileName"),
                    jobContext.getMergedJobDataMap().getString("emailSubject"),
                    jobContext.getMergedJobDataMap().getInt("statusInterval"),
                    env,
                    LOGGER);

            // Add the triggers and listener to the scheduler
            chplScheduler.getScheduler().getListenerManager()
                    .addTriggerListener(listener, getTriggerKeyMatchers(wrappers));

            // Fire the triggers
            wrappers.stream()
                    .forEach(wrapper -> fireTrigger(wrapper.getTrigger()));

        } catch (SchedulerException e) {
            LOGGER.error("Scheduler Error: " + e.getMessage(), e);
        } finally {
            //search options-related calls may have changed data now
            CacheManager.getInstance().clearAll();
            // Need to "refresh" the data in CertifiedProductDetailsManager since it is stored within the bean.
            certifiedProductDetailsManager.refreshData();
            pcpManager.refreshData();
        }
        LOGGER.info("********* Completed the Add Criteria To 2015 Listings job. *********");
    }

    private void addCriteria() {
        add2015Criterion("170.315 (b)(10)", "Clinical Information Export");
        add2015Criterion("170.315 (d)(12)", "Encrypt Authentication Credentials");
        add2015Criterion("170.315 (d)(13)", "Multi-Factor Authentication");
        add2015Criterion("170.315 (g)(10)", "Standardized API for Patient and Population Services");
    }

    private void add2015Criterion(String number, String title) {
        CertificationCriterionDTO criterion = new CertificationCriterionDTO();
        criterion.setCertificationEdition(CertificationEditionConcept.CERTIFICATION_EDITION_2015.getYear());
        criterion.setCertificationEditionId(CertificationEditionConcept.CERTIFICATION_EDITION_2015.getId());
        criterion.setNumber(number);
        criterion.setTitle(title);
        criterion.setRemoved(false);
        if (!criterionExists(criterion.getNumber(), criterion.getTitle())) {
            try {
                criterionDAO.create(criterion);
                LOGGER.info("Inserted criterion " + criterion.getNumber() + ":" + criterion.getTitle());
            } catch (EntityRetrievalException ex) {
                LOGGER.error("Error creating new 2015 criterion " + number + ":" + title, ex);
            } catch (EntityCreationException ex) {
                LOGGER.error("Error creating new 2015 criterion " + number + ":" + title, ex);
            }
        } else {
            LOGGER.info("Criterion " + number + ":" + title + " already exists.");
        }
    }

    private boolean criterionExists(String number, String title) {
        CertificationCriterionDTO criterion =
                extendedCriterionDAO.getByNumberAndTitle(number, title);
        return criterion != null;
    }

    @SuppressWarnings({"checkstyle:linelength"})
    private void addMacraMeasureMaps() {
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT2a EP Stage 3", "Patient Electronic Access: Eligible Professional", "Required Test 2: Stage 3 Objective 5 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT2a EH/CAH Stage 3", "Provide Patients Electronic Access to Their Health Information (formerly Patient Electronic Access): Eligible Hospital/Critical Access Hospital", "Required Test 2: Stage 3 Objective 5 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT2a EC PI", "Provide Patients Electronic Access to Their Health Information (formerly Patient Electronic Access): Eligible Clinician", "Required Test 2: Promoting Interoperability Objective 3 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT2c EP Stage 3", "Patient Electronic Access: Eligible Professional", "Required Test 2: Stage 3 Objective 5 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT2c EH/CAH Stage 3", "Provide Patients Electronic Access to Their Health Information (formerly Patient Electronic Access): Eligible Hospital/Critical Access Hospital", "Required Test 2: Stage 3 Objective 5 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT2c EC PI", "Provide Patients Electronic Access to Their Health Information (formerly Patient Electronic Access): Eligible Clinician", "Required Test 2: Promoting Interoperability Objective 3 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT4a EP Stage 3", "View, Download, or Transmit (VDT): Eligible Professional", "Required Test 4: Stage 3 Objective 6 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT4a EH/CAH Stage 3", "View, Download, or Transmit (VDT): Eligible Hospital/Critical Access Hospital", "Required Test 4: Stage 3 Objective 6 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT4a EC PI", "View, Download, or Transmit (VDT):  Eligible Clinician", "Required Test 4: Promoting Interoperability Objective 4 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT4c EP Stage 3", "View, Download, or Transmit (VDT): Eligible Professional", "Required Test 4: Stage 3 Objective 6 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT4c EH/CAH Stage 3", "View, Download, or Transmit (VDT): Eligible Hospital/Critical Access Hospital", "Required Test 4: Stage 3 Objective 6 Measure 1");
        addMacraMeasureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "RT4c EC PI", "View, Download, or Transmit (VDT):  Eligible Clinician", "Required Test 4: Promoting Interoperability Objective 4 Measure 1");
    }

    private void addMacraMeasureMap(String criterionNumber, String criterionTitle, String value, String name, String description) {
        if (!macraMeasureCriteriaMapExists(criterionNumber, criterionTitle, value)) {
            MacraMeasureDTO mm = new MacraMeasureDTO();
            CertificationCriterionDTO criterion = extendedCriterionDAO.getByNumberAndTitle(criterionNumber, criterionTitle);
            if (criterion == null) {
                LOGGER.error("Cannot insert macra measure for criteria that is not found: " + criterionNumber);
            } else {
                mm.setCriteriaId(criterion.getId());
                mm.setDescription(description);
                mm.setName(name);
                mm.setValue(value);
                insertableMmDao.create(mm);
                LOGGER.info("Inserted macra measure " + value + " for criterion " + criterion.getNumber());
            }
        } else {
            LOGGER.info("Mapping from " + criterionNumber + " to macra measure " + value + " already exists.");
        }
    }

    private boolean macraMeasureCriteriaMapExists(String criterionNumber, String criterionTitle, String value) {
        MacraMeasureDTO mm = insertableMmDao.getByCriteriaNumberTitleAndValue(criterionNumber, criterionTitle, value);
        return mm != null;
    }

    private void addTestDataMaps() {
        addTestDataMap("170.315 (b)(10)", "Clinical Information Export", "ONC Test Method");
        addTestDataMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "ONC Test Method");
    }

    private void addTestDataMap(String criterionNumber, String criterionTitle, String testDataName) {
        if (!testDataMapExists(criterionNumber, criterionTitle, testDataName)) {
            TestDataDTO testData = insertableTestDataDao.getTestDataByName(testDataName);
            CertificationCriterionDTO criterion = extendedCriterionDAO.getByNumberAndTitle(criterionNumber, criterionTitle);
            if (testData == null) {
                LOGGER.error("Could not find test data " + testDataName);
            }
            if (criterion == null) {
                LOGGER.error("Could not find criterion " + criterionNumber);
            }
            if (testData != null && criterion != null) {
                insertableTestDataDao.create(testData, criterion);
                LOGGER.info("Added test data mapping from " + criterionNumber + " to " + testDataName);
            }
        } else {
            LOGGER.info("Test data mapping from " + criterionNumber + " to " + testDataName + " already exists.");
        }
    }

    private boolean testDataMapExists(String criterionNumber, String criterionTitle, String testDataName) {
        TestDataDTO td = insertableTestDataDao.getByCriteriaNumberTitleAndValue(criterionNumber, criterionTitle, testDataName);
        return td != null;
    }

    private void addTestProcedureMaps() {
        addTestProcedureMap("170.315 (b)(10)", "Clinical Information Export", "ONC Test Method");
        addTestProcedureMap("170.315 (d)(12)", "Encrypt Authentication Credentials", "ONC Test Method");
        addTestProcedureMap("170.315 (d)(13)", "Multi-Factor Authentication", "ONC Test Method");
        addTestProcedureMap("170.315 (g)(10)", "Standardized API for Patient and Population Services", "ONC Test Method");
    }

    private void addTestProcedureMap(String criterionNumber, String criterionTitle, String testProcedureName) {
        if (!testProcedureMapExists(criterionNumber, criterionTitle, testProcedureName)) {
            TestProcedureDTO testProc = insertableTestProcDao.getTestProcedureByName(testProcedureName);
            CertificationCriterionDTO criterion = extendedCriterionDAO.getByNumberAndTitle(criterionNumber, criterionTitle);
            if (testProc == null) {
                LOGGER.error("Could not find test procedure " + testProcedureName);
            }
            if (criterion == null) {
                LOGGER.error("Could not find criterion " + criterionNumber);
            }
            if (testProcedureName != null && criterion != null) {
                insertableTestProcDao.create(testProc, criterion);
                LOGGER.info("Added test procedure mapping from " + criterionNumber + " to " + testProcedureName);
            }
        } else {
            LOGGER.info("Test procedure mapping from " + criterionNumber + " to " + testProcedureName + " already exists.");
        }
    }

    private boolean testProcedureMapExists(String criterionNumber, String criterionTitle, String testProcedureName) {
        TestProcedureDTO tp = insertableTestProcDao.getByCriteriaNumberTitleAndValue(
                criterionNumber, criterionTitle, testProcedureName);
        return tp != null;
    }

    private List<JobResponseTriggerWrapper> getExistingListingWrappers(JobExecutionContext jobContext) {
        List<Long> listings = getListingIds();
        List<JobResponseTriggerWrapper> wrappers = new ArrayList<JobResponseTriggerWrapper>();

        for (Long cpId : listings) {
            wrappers.add(buildTriggerWrapper(cpId, jobContext, JOB_NAME_FOR_EXISTING_LISTINGS));
        }
        LOGGER.info("Total number of existing listings to update: " + listings.size());
        return wrappers;
    }

    private List<JobResponseTriggerWrapper> getPendingListingWrappers(JobExecutionContext jobContext) {
        List<Long> listings = getPendingListingIds();
        List<JobResponseTriggerWrapper> wrappers = new ArrayList<JobResponseTriggerWrapper>();

        for (Long cpId : listings) {
            wrappers.add(buildTriggerWrapper(cpId, jobContext, JOB_NAME_FOR_PENDING_LISTINGS));
        }
        LOGGER.info("Total number of pending listings to update: " + listings.size());
        return wrappers;
    }

    private JobResponseTriggerWrapper buildTriggerWrapper(final Long cpId, final JobExecutionContext jobContext, String jobName) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("listing", cpId);
        dataMap.put("criteria", CRITERIA_TO_ADD);
        dataMap.put("logger", LOGGER);

        return new JobResponseTriggerWrapper(
                TriggerBuilder.newTrigger()
                        .forJob(jobName, JOB_GROUP)
                        .usingJobData(dataMap)
                        .build());
    }

    private List<Matcher<TriggerKey>> getTriggerKeyMatchers(final List<JobResponseTriggerWrapper> wrappers) {
        return wrappers.stream()
                .map(wrapper -> KeyMatcher.keyEquals(wrapper.getTrigger().getKey()))
                .collect(Collectors.toList());
    }

    private void fireTrigger(final Trigger trigger) {
        try {
            chplScheduler.getScheduler().scheduleJob(trigger);
        } catch (SchedulerException e) {
            LOGGER.error("Scheduler Error: " + e.getMessage(), e);
        }
    }

    private List<Long> getListingIds() {
        List<CertifiedProductDetailsDTO> cps = certifiedProductDAO.findByEdition("2015");

        return cps.stream()
                .map(cp -> cp.getId())
                .filter(cp -> cp > 1200) // testing code
                .collect(Collectors.toList());
    }

    @Transactional
    private List<Long> getPendingListingIds() {
        return pendingCertifiedProductDAO.getAllIds();
    }

    private void setSecurityContext() {

        JWTAuthenticatedUser adminUser = new JWTAuthenticatedUser();
        adminUser.setFullName("Administrator");
        adminUser.setId(ADMIN_ID);
        adminUser.setFriendlyName("Admin");
        adminUser.setSubjectName("admin");
        adminUser.getPermissions().add(new GrantedPermission("ROLE_ADMIN"));

        SecurityContextHolder.getContext().setAuthentication(adminUser);
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Component("extendedCertificationCriterionDao")
    private static class ExtendedCertificationCriterionDao extends BaseDAOImpl {

        @SuppressWarnings("unused")
        ExtendedCertificationCriterionDao() {
            super();
        }

        @Transactional
        public CertificationCriterionDTO getByNumberAndTitle(final String criterionNumber, final String criterionTitle) {
            Query query = entityManager
                    .createQuery(
                            "SELECT cce " + "FROM CertificationCriterionEntity cce "
                                    + "WHERE (NOT cce.deleted = true) "
                                    + "AND (cce.number = :number) "
                                    + "AND (cce.title = :title) ",
                                    CertificationCriterionEntity.class);
            query.setParameter("number", criterionNumber);
            query.setParameter("title", criterionTitle);
            @SuppressWarnings("unchecked") List<CertificationCriterionEntity> results = query.getResultList();

            CertificationCriterionEntity entity = null;
            if (results.size() > 0) {
                entity = results.get(0);
            }
            CertificationCriterionDTO result = null;
            if (entity != null) {
                result = new CertificationCriterionDTO(entity);
            }
            return result;
        }
    }
    @Component("insertableMacraMeasureDao")
    private static class InsertableMacraMeasureDao extends BaseDAOImpl {

        @SuppressWarnings("unused")
        InsertableMacraMeasureDao() {
            super();
        }

        @Transactional
        public MacraMeasureDTO getByCriteriaNumberTitleAndValue(String criteriaNumber, String criteriaTitle, String value) {
            Query query = entityManager.createQuery(
                    "FROM MacraMeasureEntity mme "
                            + "LEFT OUTER JOIN FETCH mme.certificationCriterion cce "
                            + "LEFT OUTER JOIN FETCH cce.certificationEdition "
                            + "WHERE (NOT mme.deleted = true) "
                            + "AND (UPPER(cce.number) = :criteriaNumber) "
                            + "AND cce.title = :criteriaTitle "
                            + "AND (UPPER(mme.value) = :value)",
                    MacraMeasureEntity.class);
            query.setParameter("criteriaNumber", criteriaNumber.trim().toUpperCase());
            query.setParameter("criteriaTitle", criteriaTitle);
            query.setParameter("value", value.trim().toUpperCase());
            @SuppressWarnings("unchecked") List<MacraMeasureEntity> result = query.getResultList();
            if (result == null || result.size() == 0) {
                return null;
            }
            return new MacraMeasureDTO(result.get(0));
        }

        @Transactional
        public void create(MacraMeasureDTO dto) {
            MacraMeasureEntity toInsert = new MacraMeasureEntity();
            toInsert.setCertificationCriterionId(dto.getCriteriaId());
            toInsert.setDeleted(false);
            toInsert.setDescription(dto.getDescription());
            toInsert.setLastModifiedUser(AuthUtil.getAuditId());
            toInsert.setLastModifiedDate(new Date());
            toInsert.setName(dto.getName());
            toInsert.setRemoved(false);
            toInsert.setValue(dto.getValue());
            super.create(toInsert);
        }
    }

    @Component("insertableTestDataDao")
    private static class InsertableTestDataDao extends BaseDAOImpl {

        @SuppressWarnings("unused")
        InsertableTestDataDao() {
            super();
        }

        @Transactional
        public TestDataDTO getByCriteriaNumberTitleAndValue(String criteriaNumber, String criteriaTitle, String value) {
            Query query = entityManager.createQuery("SELECT tdMap "
                    + "FROM TestDataCriteriaMapEntity tdMap "
                    + "JOIN FETCH tdMap.testData td "
                    + "JOIN FETCH tdMap.certificationCriterion cce "
                    + "JOIN FETCH cce.certificationEdition "
                    + "WHERE tdMap.deleted <> true "
                    + "AND td.deleted <> true "
                    + "AND (UPPER(cce.number) = :criteriaNumber) "
                    + "AND cce.title = :criteriaTitle "
                    + "AND (UPPER(td.name) = :value)",
                    TestDataCriteriaMapEntity.class);
            query.setParameter("criteriaNumber", criteriaNumber.trim().toUpperCase());
            query.setParameter("criteriaTitle", criteriaTitle);
            query.setParameter("value", value.trim().toUpperCase());

            @SuppressWarnings("unchecked") List<TestDataCriteriaMapEntity> results = query.getResultList();
            if (results == null || results.size() == 0) {
                return null;
            }
            List<TestDataEntity> tds = new ArrayList<TestDataEntity>();
            for (TestDataCriteriaMapEntity result : results) {
                tds.add(result.getTestData());
            }
            return new TestDataDTO(tds.get(0));
        }

        @Transactional
        public TestDataDTO getTestDataByName(String name) {
            String hql = "SELECT td "
                    + "FROM TestDataEntity td "
                    + "WHERE td.name = :name "
                    + "AND deleted = false";
            Query query = entityManager.createQuery(hql);
            query.setParameter("name", name);
            @SuppressWarnings("unchecked") List<TestDataEntity> tdEntities = query.getResultList();
            TestDataDTO result = null;
            if (tdEntities != null && tdEntities.size() > 0) {
                result = new TestDataDTO(tdEntities.get(0));
            }
            return result;
        }

        @Transactional
        public void create(TestDataDTO testDataDto, CertificationCriterionDTO criterion) {
            TestDataCriteriaMapEntity toInsert = new TestDataCriteriaMapEntity();
            toInsert.setCertificationCriterionId(criterion.getId());
            toInsert.setCreationDate(new Date());
            toInsert.setDeleted(false);
            toInsert.setLastModifiedDate(new Date());
            toInsert.setLastModifiedUser(AuthUtil.getAuditId());
            toInsert.setTestDataId(testDataDto.getId());
            super.create(toInsert);
        }
    }

    @Component("insertableTestProcedureDao")
    private static class InsertableTestProcedureDao extends BaseDAOImpl {

        @SuppressWarnings("unused")
        InsertableTestProcedureDao() {
            super();
        }

        @Transactional
        public TestProcedureDTO getByCriteriaNumberTitleAndValue(String criteriaNumber, String criteriaTitle, String value) {
            Query query = entityManager.createQuery("SELECT tpMap "
                    + "FROM TestProcedureCriteriaMapEntity tpMap "
                    + "JOIN FETCH tpMap.testProcedure tp "
                    + "JOIN FETCH tpMap.certificationCriterion cce "
                    + "JOIN FETCH cce.certificationEdition "
                    + "WHERE tpMap.deleted <> true "
                    + "AND tp.deleted <> true "
                    + "AND (UPPER(cce.number) = :criteriaNumber) "
                    + "AND cce.title = :criteriaTitle "
                    + "AND (UPPER(tp.name) = :value)",
                    TestProcedureCriteriaMapEntity.class);
            query.setParameter("criteriaNumber", criteriaNumber.trim().toUpperCase());
            query.setParameter("criteriaTitle", criteriaTitle);
            query.setParameter("value", value.trim().toUpperCase());

            @SuppressWarnings("unchecked") List<TestProcedureCriteriaMapEntity> results = query.getResultList();
            if (results == null || results.size() == 0) {
                return null;
            }
            List<TestProcedureEntity> tps = new ArrayList<TestProcedureEntity>();
            for (TestProcedureCriteriaMapEntity result : results) {
                tps.add(result.getTestProcedure());
            }
            return new TestProcedureDTO(tps.get(0));
        }

        @Transactional
        public TestProcedureDTO getTestProcedureByName(String name) {
            String hql = "SELECT tp "
                    + "FROM TestProcedureEntity tp "
                    + "WHERE tp.name = :name "
                    + "AND deleted = false";
            Query query = entityManager.createQuery(hql);
            query.setParameter("name", name);
            @SuppressWarnings("unchecked") List<TestProcedureEntity> tpEntities = query.getResultList();
            TestProcedureDTO result = null;
            if (tpEntities != null && tpEntities.size() > 0) {
                result = new TestProcedureDTO(tpEntities.get(0));
            }
            return result;
        }

        @Transactional
        public void create(TestProcedureDTO testProcDto, CertificationCriterionDTO criterion) {
            TestProcedureCriteriaMapEntity toInsert = new TestProcedureCriteriaMapEntity();
            toInsert.setCertificationCriterionId(criterion.getId());
            toInsert.setCreationDate(new Date());
            toInsert.setDeleted(false);
            toInsert.setLastModifiedDate(new Date());
            toInsert.setLastModifiedUser(AuthUtil.getAuditId());
            toInsert.setTestProcedureId(testProcDto.getId());
            super.create(toInsert);
        }
    }

    @Component("pendingCertifiedProductDaoIdsOnly")
    private static class PendingCertifiedProductDaoIdsOnly extends BaseDAOImpl {

        @SuppressWarnings("unused")
        PendingCertifiedProductDaoIdsOnly() {
            super();
        }

        @Transactional
        public List<Long> getAllIds() {
            List<PendingCertifiedProductEntity> entities = entityManager.createQuery(
                    "SELECT pcp from PendingCertifiedProductEntity pcp "
                            + "WHERE pcp.certificationEdition = '2015' "
                            + "AND pcp.deleted = false",
                            PendingCertifiedProductEntity.class)
                    .getResultList();
            List<Long> allIds = new ArrayList<Long>();
            for (PendingCertifiedProductEntity entity : entities) {
                allIds.add(entity.getId());
            }
            return allIds;
        }
    }
}
