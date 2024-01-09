package gov.healthit.chpl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CrashingServerApp {

    private static String[] endpoints = new String[]{
        "/rest/search/v3?certificationStatuses=Active",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=1",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=2",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=3",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=4",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=5",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=6",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=7",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=8",
        "/rest/search/v3?certificationStatuses=Active&pageNumber=9",
    };

    public static void main(String[] args) {
        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        int i = 0;
        final AtomicInteger counter = new AtomicInteger(0);
        while (true) {
            try {
                final int index = i;
                executor.submit(() -> {
                    int count = counter.getAndIncrement();
                    callEndpoint(count, "https://chpl-dev.healthit.gov" + endpoints[index]);
                    return null;
                  });
            } catch (Exception ex) {
                System.out.println("Could not call endpoint " + endpoints[i]);
            }

            i++;
            if (i == (endpoints.length)) {
                i = 0;
            }
        }
    }

    private static void callEndpoint(int requestCount, String endpoint) throws IOException, InterruptedException, URISyntaxException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(endpoint);
            System.out.println(requestCount + ": URI: " + request.toString());
            request.addHeader("API-Key", "12909a978483dfb8ecd0596c98ae9094");
            CloseableHttpResponse response = client.execute(request);

            String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(requestCount + ": Http Code: " + response.getStatusLine().getStatusCode());
            System.out.println(requestCount + ": Bytes Received: " + body.length());
            System.out.println(requestCount + ": Body: " + body);
        }

    }
}
