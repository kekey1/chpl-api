package gov.healthit.chpl.web.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ff4j.FF4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.healthit.chpl.FeatureList;
import gov.healthit.chpl.changerequest.manager.ChangeRequestManager;
import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.domain.CertificationCriterion;
import gov.healthit.chpl.domain.CriteriaSpecificDescriptiveModel;
import gov.healthit.chpl.domain.DecertifiedDeveloperResult;
import gov.healthit.chpl.domain.DimensionalData;
import gov.healthit.chpl.domain.FuzzyChoices;
import gov.healthit.chpl.domain.KeyValueModel;
import gov.healthit.chpl.domain.KeyValueModelStatuses;
import gov.healthit.chpl.domain.Measure;
import gov.healthit.chpl.domain.MeasureType;
import gov.healthit.chpl.domain.SearchOption;
import gov.healthit.chpl.domain.SearchableDimensionalData;
import gov.healthit.chpl.domain.TestFunctionality;
import gov.healthit.chpl.domain.TestStandard;
import gov.healthit.chpl.domain.UploadTemplateVersion;
import gov.healthit.chpl.domain.surveillance.SurveillanceRequirementOptions;
import gov.healthit.chpl.domain.surveillance.SurveillanceRequirementOptionsDeprecated;
import gov.healthit.chpl.dto.FuzzyChoicesDTO;
import gov.healthit.chpl.entity.FuzzyType;
import gov.healthit.chpl.exception.EntityCreationException;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.exception.InvalidArgumentsException;
import gov.healthit.chpl.logging.Loggable;
import gov.healthit.chpl.manager.ComplaintManager;
import gov.healthit.chpl.manager.DeveloperManager;
import gov.healthit.chpl.manager.DimensionalDataManager;
import gov.healthit.chpl.manager.FilterManager;
import gov.healthit.chpl.manager.FuzzyChoicesManager;
import gov.healthit.chpl.optionalStandard.domain.OptionalStandard;
import gov.healthit.chpl.surveillance.report.SurveillanceReportManager;
import gov.healthit.chpl.svap.manager.SvapManager;
import gov.healthit.chpl.web.controller.annotation.CacheControl;
import gov.healthit.chpl.web.controller.annotation.CacheMaxAge;
import gov.healthit.chpl.web.controller.annotation.CachePolicy;
import gov.healthit.chpl.web.controller.results.CertificationCriterionResults;
import gov.healthit.chpl.web.controller.results.DecertifiedDeveloperResults;
import gov.healthit.chpl.web.controller.results.SvapResults;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "data")
@RestController
@RequestMapping("/data")
@Loggable
public class DimensionalDataController {
    private DimensionalDataManager dimensionalDataManager;
    private FuzzyChoicesManager fuzzyChoicesManager;
    private DeveloperManager developerManager;
    private FilterManager filterManager;
    private ComplaintManager complaintManager;
    private SurveillanceReportManager survReportManager;
    private ChangeRequestManager changeRequestManager;
    private SvapManager svapManager;
    private FF4j ff4j;

    @Autowired
    @SuppressWarnings("checkstyle:parameternumber")
    public DimensionalDataController(DimensionalDataManager dimensionalDataManager,
            FuzzyChoicesManager fuzzyChoicesManager,
            DeveloperManager developerManager,
            FilterManager filterManager,
            ComplaintManager complaintManager,
            SurveillanceReportManager survReportManager,
            ChangeRequestManager changeRequestManager,
            SvapManager svapManager, FF4j ff4j) {
        this.dimensionalDataManager = dimensionalDataManager;
        this.fuzzyChoicesManager = fuzzyChoicesManager;
        this.developerManager = developerManager;
        this.filterManager = filterManager;
        this.complaintManager = complaintManager;
        this.survReportManager = survReportManager;
        this.changeRequestManager = changeRequestManager;
        this.svapManager = svapManager;
        this.ff4j = ff4j;
    }

    @ApiOperation(value = "Get all fuzzy matching choices for the items that be fuzzy matched.",
            notes = "Security Restrictions: ROLE_ADMIN, ROLE_ONC or ROLE_ONC_STAFF")
    @RequestMapping(value = "/fuzzy_choices", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<FuzzyChoices> getFuzzyChoices()
            throws EntityRetrievalException, JsonParseException, JsonMappingException, IOException {
        return fuzzyChoicesManager.getFuzzyChoices();
    }

    @ApiOperation(value = "Change existing fuzzy matching choices.",
            notes = "Security Restrictions: ROLE_ADMIN or ROLE_ONC")
    @RequestMapping(value = "/fuzzy_choices/{fuzzyChoiceId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/json; charset=utf-8")
    public FuzzyChoices updateFuzzyChoicesForSearching(@RequestBody FuzzyChoices fuzzyChoices)
            throws InvalidArgumentsException, EntityRetrievalException, JsonProcessingException,
            EntityCreationException, IOException {

        return updateFuzzyChoices(fuzzyChoices);
    }

    private FuzzyChoices updateFuzzyChoices(FuzzyChoices fuzzyChoices)
            throws InvalidArgumentsException, EntityRetrievalException, JsonProcessingException,
            EntityCreationException, IOException {

        FuzzyChoicesDTO toUpdate = new FuzzyChoicesDTO();
        toUpdate.setId(fuzzyChoices.getId());
        toUpdate.setFuzzyType(FuzzyType.getValue(fuzzyChoices.getFuzzyType()));
        toUpdate.setChoices(fuzzyChoices.getChoices());

        FuzzyChoices result = fuzzyChoicesManager.updateFuzzyChoices(toUpdate);
        return result;
        // return new FuzzyChoices(result);
    }

    @ApiOperation(value = "Get a list of quarters for which a surveillance report can be created.")
    @RequestMapping(value = "/quarters", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getQuarters() {
        return dimensionalDataManager.getQuarters();
    }

    @ApiOperation(value = "Get a list of surveillance process types.",
            notes = "Security Restrictions: ROLE_ADMIN, ROLE_ONC, ROLE_ONC_STAFF, or ROLE_ACB.")
    @RequestMapping(value = "/surveillance-process-types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getSurveillanceProcessTypes() {
        return survReportManager.getSurveillanceProcessTypes();
    }

    @ApiOperation(value = "Get a list of surveillance outcomes.",
            notes = "Security Restrictions: ROLE_ADMIN, ROLE_ONC, ROLE_ONC_STAFF, or ROLE_ACB.")
    @RequestMapping(value = "/surveillance-outcomes", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getSurveillanceOutcomes() {
        return survReportManager.getSurveillanceOutcomes();
    }

    @ApiOperation(value = "Get all possible classifications in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/classification_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getClassificationNames() {
        return dimensionalDataManager.getClassificationNames();
    }

    @ApiOperation(value = "Get all possible certificaiton editions in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/certification_editions", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getEditionNames() {
        return dimensionalDataManager.getEditionNames(false);
    }

    @ApiOperation(value = "Get all possible certification statuses in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/certification_statuses", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getCertificationStatuses() {
        return dimensionalDataManager.getCertificationStatuses();
    }

    @ApiOperation(value = "Get all possible practice types in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/practice_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModel> getPracticeTypeNames() {
        return dimensionalDataManager.getPracticeTypeNames();
    }

    @ApiOperation(value = "Get all possible product names in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/products", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModelStatuses> getProductNames() {
        return dimensionalDataManager.getProducts();
    }

    @ApiOperation(value = "Get all possible developer names in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/developers", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<KeyValueModelStatuses> getDeveloperNames() {
        return dimensionalDataManager.getDevelopers();
    }

    @ApiOperation(value = "Get all possible ACBs in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/certification_bodies", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody Set<CertificationBody> getCertBodyNames() {
        return dimensionalDataManager.getCertBodyNames();
    }

    @ApiOperation(value = "Get all possible education types in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/education_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getEducationTypes() {
        Set<KeyValueModel> data = dimensionalDataManager.getEducationTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible test participant age ranges in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/age_ranges", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getAgeRanges() {
        Set<KeyValueModel> data = dimensionalDataManager.getAgeRanges();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible optional standard options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/data/optional-standards", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getOptionalStandards() {
        Set<OptionalStandard> data = dimensionalDataManager.getOptionalStandards();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        if (ff4j.check(FeatureList.OPTIONAL_STANDARDS)) {
            result.setData(data);
        } else {
            result.setData(new HashSet<OptionalStandard>());
        }
        return result;
    }

    @ApiOperation(value = "Get all possible test functionality options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/test_functionality", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getTestFunctionality() {
        Set<TestFunctionality> data = dimensionalDataManager.getTestFunctionality();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible test tool options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/test_tools", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getTestTools() {
        Set<KeyValueModel> data = dimensionalDataManager.getTestTools();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible test procedure options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/test_procedures", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getTestProcedures() {
        Set<CriteriaSpecificDescriptiveModel> data = dimensionalDataManager.getTestProcedures();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible test data options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/test_data", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getTestData() {
        Set<CriteriaSpecificDescriptiveModel> data = dimensionalDataManager.getTestData();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible test standard options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/test_standards", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getTestStandards() {
        Set<TestStandard> data = dimensionalDataManager.getTestStandards();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible qms standard options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/qms_standards", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getQmsStandards() {
        Set<KeyValueModel> data = dimensionalDataManager.getQmsStandards();
        SearchOption result = new SearchOption();
        result.setExpandable(true);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible targeted user options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/targeted_users", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getTargetedUsers() {
        Set<KeyValueModel> data = dimensionalDataManager.getTargetedUesrs();
        SearchOption result = new SearchOption();
        result.setExpandable(true);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible UCD process options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/ucd_processes", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getUcdProcesses() {
        Set<KeyValueModel> data = dimensionalDataManager.getUcdProcesses();
        SearchOption result = new SearchOption();
        result.setExpandable(true);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible accessibility standard options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/accessibility_standards", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getAccessibilityStandards() {
        Set<KeyValueModel> data = dimensionalDataManager.getAccessibilityStandards();
        SearchOption result = new SearchOption();
        result.setExpandable(true);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible measure options in the CHPL",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/measures", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getMeasures() {
        Set<Measure> data = dimensionalDataManager.getMeasures();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible types of measures in the CHPL, currently this is G1 and G2.",
            notes = "This is useful for knowing what values one might possibly search for.")
    @RequestMapping(value = "/measure-types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getMeasureTypes() {
        Set<MeasureType> data = dimensionalDataManager.getMeasureTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible developer status options in the CHPL")
    @RequestMapping(value = "/developer_statuses", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getDeveloperStatuses() {
        Set<KeyValueModel> data = dimensionalDataManager.getDeveloperStatuses();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible surveillance type options in the CHPL")
    @RequestMapping(value = "/surveillance_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getSurveillanceTypes() {
        Set<KeyValueModel> data = dimensionalDataManager.getSurveillanceTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible surveillance result type options in the CHPL")
    @RequestMapping(value = "/surveillance_result_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getSurveillanceResultTypes() {
        Set<KeyValueModel> data = dimensionalDataManager.getSurveillanceResultTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible surveillance requirement type options in the CHPL")
    @RequestMapping(value = "/surveillance_requirement_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getSurveillanceRequirementTypes() {
        Set<KeyValueModel> data = dimensionalDataManager.getSurveillanceRequirementTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @Deprecated
    @ApiOperation(value = "DEPRECATED. Get all possible surveillance requirement options in the CHPL")
    @RequestMapping(value = "/surveillance_requirements", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SurveillanceRequirementOptionsDeprecated getSurveillanceRequirementOptionsDeprecated() {
        SurveillanceRequirementOptionsDeprecated data = dimensionalDataManager.getSurveillanceRequirementOptionsDeprecated();
        return data;
    }

    @ApiOperation(value = "Get all possible surveillance requirement options in the CHPL")
    @RequestMapping(value = "/surveillance-requirements", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SurveillanceRequirementOptions getSurveillanceRequirementOptions() {
        SurveillanceRequirementOptions data = dimensionalDataManager.getSurveillanceRequirementOptions();
        return data;
    }

    @Deprecated
    @ApiOperation(value = "DEPRECATED. Get all possible nonconformity status type options in the CHPL")
    @RequestMapping(value = "/nonconformity_status_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getNonconformityStatusTypes() {
        Set<KeyValueModel> data = new HashSet<KeyValueModel>();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @Deprecated
    @ApiOperation(value = "DEPRECATED. Get all possible nonconformity type options in the CHPL")
    @RequestMapping(value = "/nonconformity_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getNonconformityTypeOptionsDeprecated() {
        Set<KeyValueModel> data = dimensionalDataManager.getNonconformityTypeOptionsDeprecated();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible nonconformity type options in the CHPL")
    @RequestMapping(value = "/nonconformity-types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getNonconformityTypeOptions() {
        Set<CertificationCriterion> data = dimensionalDataManager.getNonconformityTypeOptions();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all available pending listing upload template versions.")
    @RequestMapping(value = "/upload_template_versions", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getUploadTemplateVersions() {
        Set<UploadTemplateVersion> data = dimensionalDataManager.getUploadTemplateVersions();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    /**
     * Returns all of the fields that have a finite set of values and may be
     * used as filers when searching for listings.
     * 
     * @param simple
     *            whether to include data relevant to 2011 listings (2011
     *            edition and NQF numbers)
     * @return a map of all filterable values
     * @throws EntityRetrievalException
     *             if an item cannot be retrieved from the db
     */
    @Deprecated
    @ApiOperation(value = "DEPRECATED. Use /data/search-options instead. Get all search options in the CHPL",
            notes = "This returns all of the other /data/{something} results in one single response.")
    @RequestMapping(value = "/search_options", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchableDimensionalData getSearchOptionsDeprecated(
            @RequestParam(value = "simple", required = false, defaultValue = "false") Boolean simple)
            throws EntityRetrievalException {

        return dimensionalDataManager.getSearchableDimensionalData(simple);
    }

    /**
     * Returns all of the fields that have a finite set of values and may be
     * used as filers when searching for listings.
     * 
     * @param simple
     *            whether to include data relevant to 2011 listings (2011
     *            edition and NQF numbers)
     * @return a map of all filterable values
     * @throws EntityRetrievalException
     *             if an item cannot be retrieved from the db
     */
    @ApiOperation(value = "Get all search options in the CHPL",
            notes = "This returns all of the other /data/{something} results in one single response.")
    @RequestMapping(value = "/search-options", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody DimensionalData getSearchOptions(
            @RequestParam(value = "simple", required = false, defaultValue = "false") Boolean simple)
            throws EntityRetrievalException {
        return dimensionalDataManager.getDimensionalData(simple);
    }

    @Deprecated
    @ApiOperation(value = "DEPRECATED. Use /collections/decertified-developers instead. "
            + "Get all developer decertifications in the CHPL",
            notes = "This returns all decertified developers.")
    @RequestMapping(value = "/decertifications/developers", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody DecertifiedDeveloperResults getDecertifiedDevelopers() throws EntityRetrievalException {
        DecertifiedDeveloperResults ddr = new DecertifiedDeveloperResults();
        List<DecertifiedDeveloperResult> results = developerManager.getDecertifiedDevelopers();
        ddr.setDecertifiedDeveloperResults(results);
        return ddr;
    }

    @ApiOperation(value = "Get all available filter type.")
    @RequestMapping(value = "/filter_types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getFilterTypes() {
        Set<KeyValueModel> data = filterManager.getFilterTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible complainant types in the CHPL")
    @RequestMapping(value = "/complainant-types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getComplainantTypes() {
        Set<KeyValueModel> data = complaintManager.getComplainantTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible certification criteria in the CHPL")
    @RequestMapping(value = "/certification-criteria", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody CertificationCriterionResults getCertificationCriteria() {
        Set<CertificationCriterion> criteria = dimensionalDataManager.getCertificationCriterion();
        CertificationCriterionResults result = new CertificationCriterionResults();
        for (CertificationCriterion criterion : criteria) {
            result.getCriteria().add(criterion);
        }
        return result;
    }

    @ApiOperation(value = "Get all possible change request types in the CHPL")
    @RequestMapping(value = "/change-request-types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getChangeRequestTypes() {
        Set<KeyValueModel> data = changeRequestManager.getChangeRequestTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible change request status types in the CHPL")
    @RequestMapping(value = "/change-request-status-types", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SearchOption getChangeRequestStatusTypes() {
        Set<KeyValueModel> data = changeRequestManager.getChangeRequestStatusTypes();
        SearchOption result = new SearchOption();
        result.setExpandable(false);
        result.setData(data);
        return result;
    }

    @ApiOperation(value = "Get all possible SVAP and associated criteria in the CHPL")
    @RequestMapping(value = "/svap", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    @CacheControl(policy = CachePolicy.PUBLIC, maxAge = CacheMaxAge.TWELVE_HOURS)
    public @ResponseBody SvapResults getSvapCriteriaMaps() throws EntityRetrievalException {
        return new SvapResults(svapManager.getAllSvapCriteriaMaps());
    }
}
