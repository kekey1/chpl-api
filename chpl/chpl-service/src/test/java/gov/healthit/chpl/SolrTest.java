package gov.healthit.chpl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Test;

import gov.healthit.chpl.domain.CertifiedProduct;
import gov.healthit.chpl.entity.CertificationStatusType;

public class SolrTest {

    private static final String INDEX_NAME = "active-certificates";

    @Test
    public void insertDocumentsAndQuery() throws SolrServerException, IOException {
        SolrClient client = getSolrClient();

        CertifiedProduct listing = CertifiedProduct.builder()
                .id(1L)
                .certificationDate(System.currentTimeMillis())
                .certificationStatus(CertificationStatusType.Active.getName())
                .chplProductNumber("15.05.05.3121.CHRP.01.01.1.220912")
                .edition(null)
                .build();
        UpdateResponse response = client.addBean(INDEX_NAME, listing);
        System.out.println(response.getStatus());

        listing = CertifiedProduct.builder()
                .id(2L)
                .certificationDate(System.currentTimeMillis())
                .certificationStatus(CertificationStatusType.Active.getName())
                .chplProductNumber("15.04.04.1029.Rize.01.00.0.221214")
                .edition(null)
                .build();
        response = client.addBean(INDEX_NAME, listing);
        System.out.println(response.getStatus());

        client.commit(INDEX_NAME);

        final SolrQuery query = new SolrQuery("Rize");
        query.addField("id");
        query.addField("chplProductNumber");

        final QueryResponse queryResponse = client.query(INDEX_NAME, query);
        final List<CertifiedProduct> queryResults = queryResponse.getBeans(CertifiedProduct.class);
        assertNotNull(queryResults);
        assertEquals(1, queryResults.size());
        assertEquals(2L, queryResults.get(0).getId());
    }

    private SolrClient getSolrClient() {
        final String solrUrl = "http://localhost:8983/solr";
        return new HttpJdkSolrClient.Builder(solrUrl)
            .withConnectionTimeout(10000, TimeUnit.MILLISECONDS)
            .withRequestTimeout(60000, TimeUnit.MILLISECONDS)
            .build();
    }
}
