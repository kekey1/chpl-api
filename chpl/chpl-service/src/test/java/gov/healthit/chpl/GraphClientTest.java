package gov.healthit.chpl;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.MailFolder;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MailFolderCollectionPage;
import com.microsoft.graph.requests.MailFolderCollectionRequestBuilder;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.requests.MessageCollectionRequestBuilder;

import okhttp3.Request;

public class GraphClientTest {
    private static final String GRAPH_DEFAULT_SCOPE = "https://graph.microsoft.com/.default";
    private static final String HEADER_PREFER = "Prefer";
    private static final String HEADER_IMMUTABLE_ID = "IdType=\"ImmutableId\"";

    private GraphServiceClient<Request> graphServiceClient;
    private String azureUser = "";
    private String azureClientId = "";
    private String azureClientSecret = "";
    private String azureTenantId = "";

    @Before
    public void setup() {
        ClientSecretCredential clientSecretCredential = null;

        System.out.println("Creating a new ClientSecretCredentialBuilder");
        clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(azureClientId)
            .tenantId(azureTenantId)
            .clientSecret(azureClientSecret)
            .build();

        System.out.println("Creating a new GraphServiceClient");
        final TokenCredentialAuthProvider authProvider =
            new TokenCredentialAuthProvider(
                List.of(GRAPH_DEFAULT_SCOPE), clientSecretCredential);

        graphServiceClient = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient();
    }

    @Ignore
    @Test
    public void listFolders() {
        MailFolderCollectionPage foldersPage = graphServiceClient.users(azureUser).mailFolders()
          .buildRequest()
          .get();

        int i = 1;
        while (foldersPage != null) {
            final List<MailFolder> folders = foldersPage.getCurrentPage();
            for (MailFolder folder : folders) {
                System.out.println((i++) + ": " + folder.displayName + ": " + folder.id);
            }
            final MailFolderCollectionRequestBuilder nextPage = foldersPage.getNextPage();
            if (nextPage == null) {
                break;
            } else {
                foldersPage = nextPage.buildRequest().get();
            }
        }
    }

    @Ignore
    @Test
    public void readItemsInFolder() {
        MessageCollectionPage messagesPage = graphServiceClient.users(azureUser)
                .mailFolders("mailfolderId goes here")
                .messages()
                .buildRequest()
                .select("id,sender,subject,sentDateTime")
                .get();

        int i = 1;
        while (messagesPage != null) {
            final List<Message> messages = messagesPage.getCurrentPage();
            for (Message msg : messages) {
                System.out.println((i++) + ": " + msg.id);
                System.out.println("\t" + msg.subject);
                System.out.println("\t" + msg.sentDateTime);
            }
            final MessageCollectionRequestBuilder nextPage = messagesPage.getNextPage();
            if (nextPage == null) {
                break;
            } else {
                messagesPage = nextPage.buildRequest().get();
            }
        }
    }
}
