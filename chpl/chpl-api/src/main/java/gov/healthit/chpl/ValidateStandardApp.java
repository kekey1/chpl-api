package gov.healthit.chpl;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.healthit.chpl.certificationCriteria.CertificationCriterion;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.standard.CertificationResultStandard;
import gov.healthit.chpl.standard.Standard;

public class ValidateStandardApp {

    private static String activeCertificateFile = "C://CHPL//chpl-active-20240111_065450.json";
    private static String inactiveCertificateFile = "C://CHPL//chpl-inactive-20240111_072758.json";

    private static Map<CertificationCriterion, List<Standard>> standardRequirements;

    public static void main(String[] args) throws JsonParseException, IOException {
        buildStandardRequirements();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory
        jsonFactory.setCodec(objectMapper);

        System.out.println("Loading listings from " + activeCertificateFile);
        JsonParser jParser = jsonFactory.createParser(new File(activeCertificateFile)); // or URL, Stream, Reader, String, byte[]
        List<CertifiedProductSearchDetails> listings = objectMapper.readValue(
                jParser, new TypeReference<List<CertifiedProductSearchDetails>>() { });

        for (CertifiedProductSearchDetails listing : listings) {
            for (CertificationResult certResult : listing.getCertificationResults()) {
                reviewCertificationResultHasCorrectStandards(listing, certResult);
            }
        }

        System.out.println("Loading listings from " + inactiveCertificateFile);
        jParser = jsonFactory.createParser(new File(inactiveCertificateFile)); // or URL, Stream, Reader, String, byte[]
        listings = objectMapper.readValue(jParser, new TypeReference<List<CertifiedProductSearchDetails>>() { });

        for (CertifiedProductSearchDetails listing : listings) {
            for (CertificationResult certResult : listing.getCertificationResults()) {
                reviewCertificationResultHasCorrectStandards(listing, certResult);
            }
        }
    }

    private static void buildStandardRequirements() {
        standardRequirements = new LinkedHashMap<CertificationCriterion, List<Standard>>();

        standardRequirements.put(CertificationCriterion.builder().id(16L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(d)").build(),
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

        standardRequirements.put(CertificationCriterion.builder().id(30L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(g)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(31L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(g)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(35L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(a)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(36L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(c)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(37L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(c)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(38L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(g)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(39L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(d)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(40L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.204(a)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.204(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(g)").build()).toList());
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

        standardRequirements.put(CertificationCriterion.builder().id(52L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.315(g)(3)(iv)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(55L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(58L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(59L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(e)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(60L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(b)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(d)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(e)(1)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(165L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.202(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(5)").build(),
                        Standard.builder().regulatoryTextCitation("170.202(d)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(p)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(166L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(5)").build(),
                        Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(167L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(b)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(168L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(o)(1)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(169L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(o)(1)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(170L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(5)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(172L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(h)(2)-Cures").endDay(LocalDate.parse("2022-12-31")).build(),
                        Standard.builder().regulatoryTextCitation("170.205(h)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(1)-Cures").endDay(LocalDate.parse("2022-12-31")).build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(2)-Cures").endDay(LocalDate.parse("2022-12-31")).build(),
                        Standard.builder().regulatoryTextCitation("170.205(k)(3)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(173L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(e)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(e)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(e)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(h)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(174L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(e)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(e)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(e)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(h)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(175L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(e)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.210(h)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(176L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.210(a)(2)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(178L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.204(a)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.204(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(5)").build(),
                        Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(179L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(180L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(5)").build(),
                        Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(181L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.205(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.205(a)(5)").build(),
                        Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());
        standardRequirements.put(CertificationCriterion.builder().id(182L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.213(a)").build(),
                        Standard.builder().regulatoryTextCitation("170.215(a)(1)").build(),
                        Standard.builder().regulatoryTextCitation("170.215(a)(2)").build(),
                        Standard.builder().regulatoryTextCitation("170.215(a)(3)").build(),
                        Standard.builder().regulatoryTextCitation("170.215(a)(4)").build(),
                        Standard.builder().regulatoryTextCitation("170.215(b)").build()).toList());

        standardRequirements.put(CertificationCriterion.builder().id(210L).build(),
                Stream.of(Standard.builder().regulatoryTextCitation("170.213(a)").build()).toList());
    }

    private static void reviewCertificationResultHasCorrectStandards(CertifiedProductSearchDetails listing, CertificationResult certResult) {
        CertificationCriterion criterion = certResult.getCriterion();
        Optional<CertificationCriterion> standardRequirementsKey = standardRequirements.keySet().stream()
                .filter(crit -> crit.getId().equals(criterion.getId()))
                .findAny();
        List<Standard> expectedStandards = new ArrayList<Standard>();
        if (standardRequirementsKey.isPresent()) {
            expectedStandards = standardRequirements.get(standardRequirementsKey.get());
        }
        List<String> expectedRegTexts = expectedStandards.stream()
                .map(std -> std.getRegulatoryTextCitation())
                .collect(Collectors.toList());

        //check that standards on the attested criterion are all contained in the list of expected standards
        for (CertificationResultStandard certResultStandard : certResult.getStandards()) {
            if (!expectedRegTexts.contains(certResultStandard.getStandard().getRegulatoryTextCitation())) {
                System.out.println("Standard " + certResultStandard.getStandard().getRegulatoryTextCitation() + " was found on criterion "
                        + certResult.getCriterion().getId() + " in listing " + listing.getChplProductNumber()
                        + ". The standard was not expected.");
            }
        }

        //check that every expected standard is contained on the attested criterion
        for (Standard expectedStandard : expectedStandards) {
            if (!certResultContainsStandard(certResult, expectedStandard)
                    && listingInactiveDateIsBeforeStandardEndDate(listing.getDecertificationDay(), expectedStandard)
                    && listingCertDateIsBeforeStandardEndDate(listing.getCertificationDay(), expectedStandard)) {
                System.out.println("Standard " + expectedStandard.getRegulatoryTextCitation()
                        + " was not found on criterion  " + criterion.getId() + " in listing "
                        + listing.getChplProductNumber() + ". The standard was expected.");
            }
        }
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
