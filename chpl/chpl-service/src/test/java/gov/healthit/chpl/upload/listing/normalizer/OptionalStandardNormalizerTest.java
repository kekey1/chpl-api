package gov.healthit.chpl.upload.listing.normalizer;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import gov.healthit.chpl.domain.CertificationCriterion;
import gov.healthit.chpl.domain.CertificationResult;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.exception.EntityRetrievalException;
import gov.healthit.chpl.optionalStandard.dao.OptionalStandardDAO;
import gov.healthit.chpl.optionalStandard.domain.CertificationResultOptionalStandard;
import gov.healthit.chpl.optionalStandard.domain.OptionalStandard;
import gov.healthit.chpl.optionalStandard.domain.OptionalStandardCriteriaMap;

public class OptionalStandardNormalizerTest {
    private OptionalStandardDAO optionalStandardDao;
    private OptionalStandardNormalizer normalizer;

    @Before
    public void before() {
        optionalStandardDao = Mockito.mock(OptionalStandardDAO.class);
        List<OptionalStandardCriteriaMap> allowedOptionalStandards = new ArrayList<OptionalStandardCriteriaMap>();
        allowedOptionalStandards.add(OptionalStandardCriteriaMap.builder()
                .criterion(CertificationCriterion.builder()
                        .id(1L)
                        .number("170.315 (a)(1)")
                        .build())
                .optionalStandard(OptionalStandard.builder()
                        .id(1L)
                        .citation("OPT1")
                        .build())
                .build());
        allowedOptionalStandards.add(OptionalStandardCriteriaMap.builder()
                .criterion(CertificationCriterion.builder()
                        .id(1L)
                        .number("170.315 (a)(1)")
                        .build())
                .optionalStandard(OptionalStandard.builder()
                        .id(2L)
                        .citation("OPT2")
                        .build())
                .build());

        try {
            Mockito.when(optionalStandardDao.getAllOptionalStandardCriteriaMap()).thenReturn(allowedOptionalStandards);
        } catch (EntityRetrievalException e) {
        }

        normalizer = new OptionalStandardNormalizer(optionalStandardDao);
    }

    @Test
    public void normalize_nullOptionalStandard_noChanges() {
        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationResult(CertificationResult.builder()
                        .success(true)
                        .criterion(CertificationCriterion.builder()
                                .id(1L)
                                .number("170.315 (a)(1)")
                                .build())
                        .build())
                .build();
        listing.getCertificationResults().get(0).setOptionalStandards(null);
        normalizer.normalize(listing);
        assertNull(listing.getCertificationResults().get(0).getOptionalStandards());
    }

    @Test
    public void normalize_emptyOptionalStandard_noChanges() {
        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationResult(CertificationResult.builder()
                        .success(true)
                        .criterion(CertificationCriterion.builder()
                                .id(1L)
                                .number("170.315 (a)(1)")
                                .build())
                        .optionalStandards(new ArrayList<CertificationResultOptionalStandard>())
                        .build())
                .build();
        normalizer.normalize(listing);
        assertEquals(0, listing.getCertificationResults().get(0).getOptionalStandards().size());
    }

    @Test
    public void normalize_optionalStandardNotInDatabase_idIsNull() {
        List<CertificationResultOptionalStandard> optionalStandards = new ArrayList<CertificationResultOptionalStandard>();
        optionalStandards.add(CertificationResultOptionalStandard.builder()
                .optionalStandardId(null)
                .citation("notindb")
                .build());

        Mockito.when(optionalStandardDao.getByCitation(ArgumentMatchers.eq("notindb")))
            .thenReturn(null);
        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationEdition(create2015EditionMap())
                .certificationResult(CertificationResult.builder()
                        .success(true)
                        .criterion(CertificationCriterion.builder()
                                .id(1L)
                                .number("170.315 (a)(1)")
                                .build())
                        .optionalStandards(optionalStandards)
                        .build())
                .build();
        normalizer.normalize(listing);
        assertEquals(1, listing.getCertificationResults().get(0).getOptionalStandards().size());
        assertNull(listing.getCertificationResults().get(0).getOptionalStandards().get(0).getOptionalStandardId());
    }

    @Test
    public void normalize_optionalStandardInDatabase_setsId() {
        List<CertificationResultOptionalStandard> optionalStandards = new ArrayList<CertificationResultOptionalStandard>();
        optionalStandards.add(CertificationResultOptionalStandard.builder()
                .optionalStandardId(null)
                .citation("valid")
                .build());

        Mockito.when(optionalStandardDao.getByCitation(ArgumentMatchers.eq("valid")))
            .thenReturn(OptionalStandard.builder()
                    .id(1L)
                    .citation("valid")
                    .build());

        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationEdition(create2015EditionMap())
                .certificationResult(CertificationResult.builder()
                        .success(true)
                        .criterion(CertificationCriterion.builder()
                                .id(1L)
                                .number("170.315 (a)(1)")
                                .build())
                        .optionalStandards(optionalStandards)
                        .build())
                .build();
        normalizer.normalize(listing);
        assertEquals(1, listing.getCertificationResults().get(0).getOptionalStandards().size());
        assertEquals(1L, listing.getCertificationResults().get(0).getOptionalStandards().get(0).getOptionalStandardId());
    }

    @Test
    public void normalize_criterionHasAllowedOptionalStandards_addsAllowedOptionalStandardsToCertificationResult() {
        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationResult(CertificationResult.builder()
                        .criterion(CertificationCriterion.builder()
                                    .id(1L)
                                    .number("170.315 (a)(1)")
                                    .build())
                        .optionalStandards(new ArrayList<CertificationResultOptionalStandard>())
                        .build())
                .build();
        normalizer.normalize(listing);
        assertEquals(2, listing.getCertificationResults().get(0).getAllowedOptionalStandards().size());
    }

    @Test
    public void normalize_criterionHasNoAllowedOptionalStandards_noAllowedOptionalStandardsAdded() {
        CertifiedProductSearchDetails listing = CertifiedProductSearchDetails.builder()
                .certificationResult(CertificationResult.builder()
                        .criterion(CertificationCriterion.builder()
                                    .id(2L)
                                    .number("170.315 (a)(2)")
                                    .build())
                        .optionalStandards(new ArrayList<CertificationResultOptionalStandard>())
                        .build())
                .build();
        normalizer.normalize(listing);
        assertEquals(0, listing.getCertificationResults().get(0).getAllowedOptionalStandards().size());
    }

    private Map<String, Object> create2015EditionMap() {
        Map<String, Object> editionMap = new HashMap<String, Object>();
        editionMap.put(CertifiedProductSearchDetails.EDITION_ID_KEY, 3L);
        editionMap.put(CertifiedProductSearchDetails.EDITION_NAME_KEY, "2015");
        return editionMap;
    }
}