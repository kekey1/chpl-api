package gov.healthit.chpl;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.healthit.chpl.certificationCriteria.CertificationCriterion;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.standard.Standard;

public class ValidateStandardApp {

    private static String activeCertificateFile = "C://CHPL//chpl-active-20240109_065450.json";
    private static String inactiveCertificateFile = "C://CHPL//chpl-inactive-20240109_072757.json";

    private static Map<CertificationCriterion, List<Standard>> standardRequirements;

    public static void main(String[] args) throws JsonParseException, IOException {
        buildStandardRequirements();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
        jsonFactory.setCodec(objectMapper);

        CertificationCriterion criterion = CertificationCriterion.builder().id(172L).build();
        Standard standard = Standard.builder()
                .regulatoryTextCitation("170.205(k)(3)")
                .endDay(null)
                .build();

        System.out.println("Loading listings from " + activeCertificateFile);
        JsonParser jParser = jsonFactory.createParser(new File(activeCertificateFile)); // or URL, Stream, Reader, String, byte[]
        List<CertifiedProductSearchDetails> listings = objectMapper.readValue(
                jParser, new TypeReference<List<CertifiedProductSearchDetails>>() { });
        reviewListingsHaveStandardOnExpectedCriterion(listings, criterion, standard);
        //TODO: review standard isn't on any other criteria

        System.out.println("Loading listings from " + inactiveCertificateFile);
        jParser = jsonFactory.createParser(new File(inactiveCertificateFile)); // or URL, Stream, Reader, String, byte[]
        listings = objectMapper.readValue(jParser, new TypeReference<List<CertifiedProductSearchDetails>>() { });
        reviewListingsHaveStandardOnExpectedCriterion(listings, criterion, standard);
        //TODO: review standard isn't on any other criteria
    }

    private static void reviewListingsHaveStandardOnExpectedCriterion(
            List<CertifiedProductSearchDetails> listings, CertificationCriterion criterion, Standard standard) {
        int listingCount = 0;
        int listingsWithCriterionCount = 0;
        System.out.println("Iterating through each listing...");
        for (CertifiedProductSearchDetails listing : listings) {
            CertificationResult attestedCriterion = getCertResult(listing, criterion);
            if (attestedCriterion != null) {
                listingsWithCriterionCount++;
                if (!certResultContainsStandard(attestedCriterion, standard)
                        && listingInactiveDateIsBeforeStandardEndDate(listing.getDecertificationDay(), standard)
                        && listingCertDateIsBeforeStandardEndDate(listing.getCertificationDay(), standard)) {
                    System.out.println("Standard " + standard.getRegulatoryTextCitation() + " not found for criterion ID " + criterion.getId() + " in listing "
                            + listing.getChplProductNumber());
                }
            }
            listingCount++;
        }
        System.out.println("# listings with criterion " + criterion.getId() + ": " + listingsWithCriterionCount);
        System.out.println("# listings: " + listingCount);
    }

    private static void buildStandardRequirements() {
        standardRequirements = new LinkedHashMap<CertificationCriterion, List<Standard>>();

        standardRequirements.put(CertificationCriterion.builder().id(16L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(d))").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(p)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(17L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(19L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(20L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(21L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(22L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(o)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(23L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(o)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(24L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(25L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(h)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(26L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(h)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(27L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(h)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(28L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(h)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(2)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(40L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.204(a)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.204(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(43L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(e)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(44L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(d)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(45L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(g)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(46L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(i)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(48L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(r)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(49L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(s)(1)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(55L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(58L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(59L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(e)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(60L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(b))").build(),
                        Standard.builder().regulatoryTextCitation("170.202(d))").build(),
                        Standard.builder().regulatoryTextCitation("170.202(e)(1)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(165L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(d))").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(p)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(166L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(167L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(b)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(168L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(o)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(169L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(o)(1)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(170L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(172L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(h)(2)-Cures").endDay(LocalDate.parse("2022-12-31")).build(),
                        Standard.builder().regulatoryTextCitation("170.205(h)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(1)-Cures").endDay(LocalDate.parse("2022-12-31")).build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(2)-Cures").endDay(LocalDate.parse("2022-12-31")).build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(3)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(178L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.204(a)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.204(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(180L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(181L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
    }

    private static CertificationResult getCertResult(CertifiedProductSearchDetails listing, CertificationCriterion criterion) {
        if (listing.getCertificationResults() != null && listing.getCertificationResults().size() > 0) {
            Optional<CertificationResult> attestedCertResult = listing.getCertificationResults().stream()
                .filter(certResult -> certResult.getCriterion().getId().equals(criterion.getId()))
                .filter(certResult -> certResult.getSuccess().equals(Boolean.TRUE))
                .findAny();
            if (attestedCertResult.isEmpty()) {
                return null;
            }
            return attestedCertResult.get();
        }
        return null;
    }

    private static boolean certResultContainsStandard(CertificationResult certResult, Standard standard) {
        return certResult.getStandards().stream()
            .filter(std -> std.getStandard().getRegulatoryTextCitation().equals(standard.getRegulatoryTextCitation()))
            .findAny().isPresent();
    }

    private static boolean listingCertDateIsBeforeStandardEndDate(LocalDate certDay, Standard standard) {
        if (standard.getEndDay() == null) {
            return true;
        }
        return standard.getEndDay().isEqual(certDay) || standard.getEndDay().isAfter(certDay);
    }

    private static boolean listingInactiveDateIsBeforeStandardEndDate(LocalDate decertDay, Standard standard) {
        if (standard.getEndDay() == null) {
            return true;
        }
        if (decertDay == null) {
            return true;
        }
        return standard.getEndDay().isEqual(decertDay) || standard.getEndDay().isAfter(decertDay);
    }
}
