package gov.healthit.chpl.scheduler.job.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import gov.healthit.chpl.dao.ActivityDAO;
import gov.healthit.chpl.dao.CertificationCriterionDAO;
import gov.healthit.chpl.domain.CertificationCriterion;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.activity.ActivityConcept;
import gov.healthit.chpl.domain.search.CertifiedProductFlatSearchResult;
import gov.healthit.chpl.dto.ActivityDTO;
import gov.healthit.chpl.dto.CertificationCriterionDTO;
import gov.healthit.chpl.manager.CertifiedProductSearchManager;
import gov.healthit.chpl.scheduler.job.QuartzJob;
import gov.healthit.chpl.util.JSONUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "mongoActivityWriterJobLogger")
public class MongoActivityWriter extends QuartzJob {

    @Autowired
    private CertifiedProductSearchManager searchManager;

    @Autowired
    private CertificationCriterionDAO criteriaDao;

    @Autowired
    private ActivityDAO activityDao;

    @Value("${mongoDb.url}")
    private String mongoConnectionString;

    private ObjectMapper jsonMapper;

    public MongoActivityWriter() {
        jsonMapper = new ObjectMapper();
    }

    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        LOGGER.info("********* Starting the Mongo Activity Writer job. *********");
        try (MongoClient mongoClient = MongoClients.create(mongoConnectionString)) {
            MongoDatabase openchplDb = mongoClient.getDatabase("openchpl");
            //tests connection
            List<Document> databases = mongoClient.listDatabases().into(new ArrayList<>());
            databases.forEach(db -> LOGGER.info(db.toJson()));

            MongoCollection<Document> listingActivityCollection = openchplDb.getCollection("listing_activity");

            List<CertifiedProductFlatSearchResult> allListings = searchManager.search();
            allListings.stream()
                .filter(listing -> listing.getEdition().equals("2015"))
                .forEach(listing -> getListingActivityAndInsertIntoMongo(listing.getId(), listingActivityCollection));
        }
        LOGGER.info("********* Completed the Mongo Activity Writer job. *********");
    }

    private void getListingActivityAndInsertIntoMongo(Long listingId, MongoCollection<Document> listingActivityCollection) {
        List<ActivityDTO> allListingActivities
            = activityDao.findByObjectId(listingId, ActivityConcept.CERTIFIED_PRODUCT, new Date(0), new Date());
        LOGGER.info("Found " + allListingActivities.size() + " activities for listing " + listingId + ".");

        for (ActivityDTO activity : allListingActivities) {
            LOGGER.info("Processing activity " + activity.getId());
            MongoListingActivity newListing = null;
            if (!StringUtils.isEmpty(activity.getNewData())) {
                try {
                    newListing =
                        jsonMapper.readValue(activity.getNewData(), MongoListingActivity.class);
                } catch (Exception ex) {
                    LOGGER.error("Could not parse activity ID " + activity.getId() + " new data. "
                            + "JSON was: " + activity.getNewData(), ex);
                }
            }
            if (newListing != null) {
                newListing.setHistoricalListingDate(activity.getActivityDate());
                augmentCriteriaIds(newListing);
                Document jsonDoc = null;
                try {
                    jsonDoc = Document.parse(JSONUtils.toJSON(newListing));
                } catch (Exception ex) {
                    LOGGER.error("Could not convert object to json", ex);
                }
                if (jsonDoc != null) {
                    listingActivityCollection.insertOne(jsonDoc);
                } else {
                    LOGGER.warn("Not inserting json for activity.");
                }
                LOGGER.info("Inserted newData for activity " + activity.getId());
            }
        }
    }

    private void augmentCriteriaIds(CertifiedProductSearchDetails listing) {
        if (listing.getCertificationResults() != null && listing.getCertificationResults().size() > 0) {
            listing.getCertificationResults().stream()
                .filter(certResult -> hasCriterionNumberButNotCriterionObject(certResult))
                .forEach(certResult -> fillInCriteriaId(certResult));
        }
    }

    private boolean hasCriterionNumberButNotCriterionObject(CertificationResult certResult) {
        return !StringUtils.isEmpty(certResult.getNumber())
                && (certResult.getCriterion() == null || certResult.getCriterion().getId() == null);
    }

    private void fillInCriteriaId(CertificationResult certResult) {
        List<CertificationCriterionDTO> criteriaByNumber = criteriaDao.getAllByNumber(certResult.getNumber());
        CertificationCriterionDTO criterion = criteriaByNumber.stream()
            .filter(crit -> !crit.getTitle().contains("Cures"))
            .findFirst().get();
        if (criterion != null) {
            certResult.setCriterion(new CertificationCriterion(criterion));
        }
    }
}
