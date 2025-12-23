package gov.healthit.chpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.core.models.IProgressCallback;
import com.microsoft.graph.core.models.UploadResult;
import com.microsoft.graph.core.tasks.LargeFileUploadTask;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.AttachmentItem;
import com.microsoft.graph.models.AttachmentType;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.messages.MessagesRequestBuilder;
import com.microsoft.graph.users.item.messages.item.attachments.createuploadsession.CreateUploadSessionPostRequestBody;

import gov.healthit.chpl.email.ChplEmailMessage;
import io.jsonwebtoken.lang.Arrays;

public class GraphTest {
    private static final String GRAPH_DEFAULT_SCOPE = "https://graph.microsoft.com/.default";
    private static final String HEADER_PREFER = "Prefer";
    private static final String HEADER_IMMUTABLE_ID = "IdType=\"ImmutableId\"";
    private static final int MAX_ATTACH_LARGE_FILE_ATTEMPTS = 5;
    private static final BigInteger THREE_MB_IN_BYTES = new BigInteger("3145728");

    //THE BELOW PROPERTIES MUST NOT BE COMMITTED
    //FILL THEM IN WITH VALID VALUES BEFORE RUNNING THE TESTS
    private static final String[] RECIPIENTS = new String[] {""};
    private static final String ABSOLUTE_FILE_PATH = "";
    private static final String AZURE_USER = "";
    private static final String AZURE_CLIENT_ID = "";
    private static final String AZURE_CLIENT_SECRET = "";
    private static final String AZURE_TENANT_ID = "";

    private GraphServiceClient graphServiceClient;


    //I would like to leave this test here in the event we need to diagnose any issues with Email.
    @Ignore
    @Test
    public void testEmailWithLargeAttachment() {
        ChplEmailMessage message = new ChplEmailMessage();
        message.setBody("Test");
        message.setRetryAttempts(2);
        message.setSubject("Test");
        message.setToRecipients(Arrays.asList(RECIPIENTS));
        message.setFileAttachments(Stream.of(new File(ABSOLUTE_FILE_PATH)).collect(Collectors.toList()));

        graphServiceClient = getGraphServiceClient();

        Message graphMessage = null;
        graphMessage = getDraftMessage(message);
        uploadAttachments(graphMessage, message.getFileAttachments());
        sendMessage(graphMessage);
        System.out.println("Email successfully sent to: "
                + Stream.concat(message.getToRecipients().stream(), message.getCcRecipients().stream()).
                        map(addr -> addr.toString())
                        .collect(Collectors.joining(", ")));
        System.out.println("With subject: " + message.getSubject());
        deleteMessage(graphMessage);
    }

    private Message getDraftMessage(ChplEmailMessage message) {
        System.out.println("Creating a draft message with subject '" + message.getSubject() + "'");
        final Message draftMessage = new Message();
        draftMessage.setSubject(message.getSubject());
        ItemBody body = new ItemBody();
        body.setContent(message.getBody());
        body.setContentType(BodyType.Html);
        draftMessage.setBody(body);

        draftMessage.setToRecipients(new ArrayList<Recipient>());
        List<String> recipientAddresses = message.getToRecipients();
        recipientAddresses.stream()
            .forEach(recipientAddress -> {
                final Recipient recipient = new Recipient();
                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setAddress(recipientAddress.trim());
                recipient.setEmailAddress(emailAddress);
                draftMessage.getToRecipients().add(recipient);
            });

        System.out.println("Saving the draft message");
        MessagesRequestBuilder messageBuilder = graphServiceClient
                .users().byUserId(AZURE_USER)
                .messages();
        Message savedDraft = messageBuilder.post(draftMessage, new ImmutableIdHeaderRequestConfiguration());
        System.out.println("Saved the draft message with ID " + savedDraft.getId());

        return savedDraft;
    }

    private void uploadAttachments(Message message, List<File> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            System.out.println("No attachments for message " + message.getId());
            return;
        }
        attachments.stream()
            .forEach(attachment -> uploadAttachment(message, attachment));
    }

    private void uploadAttachment(Message message, File attachment) {
        System.out.println("Uploading attachment " + attachment.getName() + " for message " + message.getId());
        long attachmentSize = FileUtils.sizeOfAsBigInteger(attachment).longValue();
        System.out.println("Attachment is " + attachmentSize + " bytes");
        if (attachmentSize < THREE_MB_IN_BYTES.longValue()) {
            uploadSmallAttachment(message, attachment);
        } else {
            System.out.println("Attaching file larger than 3MB: " + attachment.getName());
            uploadLargeAttachment(message, attachment);
        }
    }

    private void uploadSmallAttachment(Message message, File attachment) {
        try {
            FileAttachment fileAttachment = new FileAttachment();
            fileAttachment.setOdataType("#microsoft.graph.fileAttachment");
            fileAttachment.setName(attachment.getName());
            byte[] contentBytes = FileUtils.readFileToByteArray(attachment);
            fileAttachment.setContentBytes(contentBytes);
            Attachment attachedFile = graphServiceClient.users().byUserId(AZURE_USER)
                    .messages().byMessageId(message.getId())
                    .attachments()
                    .post(fileAttachment);
            if (attachedFile != null) {
                System.out.println("Completed uploading attachment " + attachment.getName() + " for message " + message.getId());
            }
        } catch (IOException ex) {
            System.out.println("Exception attaching file " + attachment.getName());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("Exception attaching file " + attachment.getName());
            ex.printStackTrace();
        }
    }

    private void uploadLargeAttachment(Message message, File attachment) {
        CreateUploadSessionPostRequestBody createUploadSessionPostRequestBody = new CreateUploadSessionPostRequestBody();
        AttachmentItem attachmentItem = new AttachmentItem();
        attachmentItem.setAttachmentType(AttachmentType.File);
        attachmentItem.setName(attachment.getName());
        attachmentItem.setSize(attachment.length());
        createUploadSessionPostRequestBody.setAttachmentItem(attachmentItem);
        UploadSession uploadSession = graphServiceClient
                .users().byUserId(AZURE_USER)
                .messages().byMessageId(message.getId())
                .attachments()
                .createUploadSession()
                .post(createUploadSessionPostRequestBody);

        //iteratively upload byte ranges of the file, in order
        IProgressCallback callback = new IProgressCallback() {
            @Override
            public void report(long current, long max) {
                System.out.println(
                        String.format("Uploaded %d bytes of %d total bytes", current, max)
                    );
            }
        };

        InputStream fileStream = null;
        LargeFileUploadTask<FileAttachment> uploadTask = null;
        try {
            fileStream = new FileInputStream(attachment);
            uploadTask = new LargeFileUploadTask<FileAttachment>(graphServiceClient.getRequestAdapter(),
                        uploadSession,
                        fileStream,
                        attachment.length(),
                        FileAttachment::createFromDiscriminatorValue);

            UploadResult<FileAttachment> uploadResult = uploadTask.upload(MAX_ATTACH_LARGE_FILE_ATTEMPTS, callback);
            if (uploadResult.isUploadSuccessful()) {
                System.out.println("Upload successful");
            } else {
                System.out.println("Upload failed");
            }
            System.out.println("Completed uploading attachment " + attachment.getName() + " for message " + message.getId());
        } catch (FileNotFoundException ex) {
            //the FileInputStream could not be created
            System.out.println("The file " + attachment.getAbsolutePath() + " could not be found and will not be sent as an attachment.");
            ex.printStackTrace();
        } catch (IOException ex) {
            //the uploadTask.upload method had an error
            System.out.println("The upload of file attachment " + attachment.getAbsoluteFile() + " to message with ID " + message.getId() + " failed.");
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            System.out.println("The upload of file attachment " + attachment.getAbsoluteFile() + " to message with ID " + message.getId() + " failed.");
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("The upload of file attachment " + attachment.getAbsoluteFile() + " to message with ID " + message.getId() + " failed.");
            ex.printStackTrace();
        } finally {
            try {
                fileStream.close();
            } catch (IOException io) {
                System.out.println("Could not close the filestream for the attachment: " + io.getMessage());
            }
        }
    }

    private void sendMessage(Message message) {
        System.out.println("Sending message with ID " + message.getId());
        graphServiceClient
        .users()
            .byUserId(AZURE_USER)
                .messages().byMessageId(message.getId())
        .send()
        .post();
    }

    private void deleteMessage(Message message) {
        //The message object gets moved from Drafts to Sent Items, but it might not be available right away...
        //It can take time to appear there.
        //If this turns out to be a problem, like if Sent Items fills up or something,
        //we can adjust the code here to to wait, to retry a configurable number of times, or schedule a separate job to retry.
        try {
            System.out.println("Deleting message with ID " + message.getId());
            graphServiceClient
            .users()
                .byUserId(AZURE_USER)
                    .messages().byMessageId(message.getId())
                    .delete();
        } catch (Exception ex) {
            if (message != null) {
                System.out.println("Error deleting" + (message.getIsDraft() ? " draft " : " ")
                    + "message with ID '" + message.getId() + "'. Message had subject '"
                    + message.getSubject() + "' and is addressed to "
                    + message.getToRecipients().stream()
                        .map(recip -> recip.getEmailAddress().getAddress())
                        .collect(Collectors.joining(",")));
            }
            ex.printStackTrace();
        }
    }

    private GraphServiceClient getGraphServiceClient() {
        ClientSecretCredential clientSecretCredential = null;
        GraphServiceClient graphServiceClient = null;

        System.out.println("Creating a new ClientSecretCredentialBuilder");

        clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(AZURE_CLIENT_ID)
                .tenantId(AZURE_TENANT_ID)
                .clientSecret(AZURE_CLIENT_SECRET)
                .build();

        graphServiceClient = new GraphServiceClient(clientSecretCredential, GRAPH_DEFAULT_SCOPE);
        return graphServiceClient;
    }

    private static final class ImmutableIdHeaderRequestConfiguration implements Consumer<MessagesRequestBuilder.PostRequestConfiguration> {
        @Override
        public void accept(com.microsoft.graph.users.item.messages.MessagesRequestBuilder.PostRequestConfiguration t) {
            t.headers.add(HEADER_PREFER, HEADER_IMMUTABLE_ID);
        }
    }
}
