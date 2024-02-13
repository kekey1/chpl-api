package gov.healthit.chpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Configuration
public class ChplElasticsearchConfiguration extends ElasticsearchConfiguration  {

    @Autowired
    private Environment env;

    @Override
    public ClientConfiguration clientConfiguration() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "ApiKey " + env.getProperty("elastic.apikey"));

        LOGGER.info("Configuring Elasticsearch!");
        return ClientConfiguration.builder()
            .connectedTo("localhost:9200")
            .usingSsl("7F1DAB49639C3F646C0B4BB51B4F619D2556C26715E33262666EFBF397F7FE47") //add the generated sha-256 fingerprint
            .withBasicAuth(env.getProperty("elastic.username"), env.getProperty("elastic.password"))
            .withDefaultHeaders(httpHeaders)
            .build();
    }
}