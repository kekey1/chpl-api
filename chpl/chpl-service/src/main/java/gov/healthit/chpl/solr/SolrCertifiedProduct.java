package gov.healthit.chpl.solr;

import java.util.List;

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
    private List<String> cqms;
}
