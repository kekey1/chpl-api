package gov.healthit.chpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Test;

import gov.healthit.chpl.solr.SolrCertifiedProduct;

public class SolrTest {

    private static final String INDEX_NAME = "active-certificates";

    @Test
    public void insertDocumentsAndQuery() throws SolrServerException, IOException {
        SolrClient client = getSolrClient();

        LocalDate yesterday = LocalDate.now().minusDays(1);
        SolrCertifiedProduct listing = SolrCertifiedProduct.builder()
                .id("1")
                .chplProductNumber("15.05.05.3121.CHRP.01.01.1.220912")
                .certificationDay("[" + yesterday + " TO " + yesterday + "]")
                .acb("Drummond Group, Inc")
                .atls(Stream.of("ICSA", "Leidos").toList())
                .developer("Epic")
                .product("EpicCare")
                .version("1.1")
                .accessibilityCertified(false)
                .attestedCqms(Stream.of("CMS123", "CMS456").toList())
                .build();
        try {
            UpdateResponse response = client.addBean(INDEX_NAME, listing);
            System.out.println(response.getStatus());
        } catch (Exception ex) {
            System.out.println(ex);
        }

        client.commit(INDEX_NAME);

        final SolrQuery query = new SolrQuery("*:*");

        final QueryResponse queryResponse = client.query(INDEX_NAME, query);
        final List<SolrCertifiedProduct> queryResults = queryResponse.getBeans(SolrCertifiedProduct.class);
        assertNotNull(queryResults);
        assertEquals(1, queryResults.size());
        assertEquals("1", queryResults.get(0).getId());
        assertNotNull(queryResults.get(0).getAttestedCqms());
        assertEquals(2, queryResults.get(0).getAttestedCqms().size());
    }

    private SolrClient getSolrClient() {
        final String solrUrl = "http://localhost:8983/solr";
        return new HttpJdkSolrClient.Builder(solrUrl)
            .withConnectionTimeout(10000, TimeUnit.MILLISECONDS)
            .withRequestTimeout(60000, TimeUnit.MILLISECONDS)
            .build();
    }
}
