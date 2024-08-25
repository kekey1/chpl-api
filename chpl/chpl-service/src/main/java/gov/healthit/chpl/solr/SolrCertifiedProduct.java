package gov.healthit.chpl.solr;

import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.beans.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SolrCertifiedProduct {

    //this is a string because Solr doesn't allow id fields to be longs
    @Field
    private String id;

    @Field
    private String chplProductNumber;

    @Field
    private String certificationStatus;

    @Field
    private String reportFileLocation;

    @Field
    private String sedReportFileLocation;

    @Field
    private String sedIntendedUserDescription;

    //implemented as SolrDateRange
    @Field
    private String sedTestingEndDay;

    @Field
    private String acbCertificationId;

    @Field
    private String developer;

    @Field
    private String developerCode;

    //could search for listings by Banned developers
    @Field
    private String developerStatus;

    @Field
    private String developerWebsite;

    //line 1 plus line 2
    @Field
    private String developerStreetAddress;

    //we could search for "developers in California" or something
    @Field
    private String developerState;

    @Field
    private String product;

    @Field
    private String version;

    @Field
    private String acb;

    @Field
    private List<String> atls;

    //implemented as SolrDateRange
    @Field
    private String certificationDay;

    //implemented as SolrDateRange
    @Field
    private String decertificationDay;

    @Field
    private Boolean accessibilityCertified;

    @Field
    private String mandatoryDisclosures;

    @Field
    private List<String> previousChplProductNumbers;

    @Field
    private String rwtPlansUrl;

    //implemented as SolrDateRange
    @Field
    private String rwtPlansCheckDate;

    @Field
    private String rwtResultsUrl;

    //implemented as SolrDateRange
    @Field
    private String rwtResultsCheckDate;

    @Field
    private String svapNoticeUrl;

    @Field
    private List<String> listingsRelatedViaInheritance;

    @Field
    private List<String> accessibilityStandardNames;

    @Field
    private List<String> targetedUserNames;

    @Field
    private List<String> qmsStandardNames;

    @Field
    private List<String> qmsStandardModifications;
    //TODO also could have fields for qms standard applicable criteria

    @Field
    private List<String> measures;

    @Field
    private List<String> attestedCqms;

    @Field
    private List<String> attestedCriteria;

    @Field
    private Set<String> additionalSoftwareNames;

    @Field
    private Set<String> conformanceMethodNames;

    @Field
    private Set<String> documentationUrls;

    @Field
    private Set<String> exportDocumentation;

    @Field
    private Set<String> functionalitiesTestedNames;

    @Field
    private Set<String> optionalStandardNames;

    @Field
    private Set<String> privacySecurityFramework;

    @Field
    private Set<String> riskManagementSummaryInformation;

    @Field
    private Set<String> serviceBaseUrlList;

    @Field
    private Set<String> standardNames;

    @Field
    private Set<String> svaps;

    @Field
    private Set<String> testDataNames;

    @Field
    private Set<String> testToolNames;

    //SED
    @Field
    private Set<String> testTaskDescriptions;

    @Field
    private Set<String> testParticipantOccupations;

    @Field
    private Set<String> testParticipantEducations;

    //Surveillance
    @Field
    private Set<String> nonconformitySummary;

    @Field
    private Set<String> nonconformityFindings;

    @Field
    private Set<String> nonconformityResolution;

    @Field
    private Set<String> nonconformityDeveloperExplanations;
}
