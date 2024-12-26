package gov.healthit.chpl.scheduler.job.sed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;

import gov.healthit.chpl.certificationCriteria.CertificationCriterion;
import gov.healthit.chpl.dao.impl.BaseDAOImpl;
import gov.healthit.chpl.email.ChplEmailFactory;
import gov.healthit.chpl.exception.EmailNotSentException;
import gov.healthit.chpl.service.CertificationCriterionService;
import gov.healthit.chpl.service.CertificationCriterionService.Criteria2015;
import gov.healthit.chpl.util.CertificationStatusUtil;
import gov.healthit.chpl.util.Util;
import jakarta.persistence.Query;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@DisallowConcurrentExecution
@Log4j2(topic = "categorizeTestTasksJobLogger")
public class CategorizeTestTasksJob implements Job {

    @Autowired
    private SedDao sedDao;

    @Autowired
    private OpenAiConfigs openAiConfigs;

    @Autowired
    private CertificationCriterionService certificationCriterionService;

    @Autowired
    private ChplEmailFactory chplEmailFactory;

    private final CategoryStats stats = new CategoryStats();
    private int totalTokens = 0;
    private OpenAIClient openAiClient;
    private String reportFilename;

    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        LOGGER.info("********* Starting the Categorize Test Tasks Job. *********");

        openAiClient = setupOpenAIClient();
        reportFilename = "categorized-test-tasks";

        //TODO: it might be nice to allow the user to enter the criterion number when scheduling the report
        //TODO: it might also be nice to give the user a big text area to enter the AI prompt
        //with several autofill type options in a dropdown
        CertificationCriterion criterion = certificationCriterionService.get(Criteria2015.A_1);

        LOGGER.info("Getting task descriptions for " + Util.formatCriteriaNumber(criterion));
        List<String> taskDescriptions = sedDao.getUniqueTestTasksForCriterion(criterion.getId());
        LOGGER.info("Found {} descriptions to process", taskDescriptions.size());

        LOGGER.info("Asking OpenAI to categorize task descriptions for " + Util.formatCriteriaNumber(criterion));
        List<String> taskCategories = processDescriptions(taskDescriptions);
        LOGGER.info("Completed categorizing task descriptions");

        try {
            File results = writeToFile(taskDescriptions, taskCategories);
            sendFileToUser(jobContext.getMergedJobDataMap().getString("email"), results);
        } catch (Exception ex) {
            LOGGER.error("Unable to create file and/or send file to user", ex);
        }
        LOGGER.info("********* Completed the Categorize Test Tasks Job. *********");
    }

    private OpenAIClient setupOpenAIClient() {
        return new OpenAIClientBuilder()
            .endpoint(openAiConfigs.getAzureOpenAiUrl())
            .credential(new AzureKeyCredential(openAiConfigs.getAzureOpenAiKey()))
            .buildClient();
    }

    public List<String> processDescriptions(List<String> taskDescriptions) {
        List<String> allCategories = new ArrayList<>();
        try {
            // Process in batches
            for (int i = 0; i < taskDescriptions.size(); i += openAiConfigs.getBatchSize()) {
                int end = Math.min(i + openAiConfigs.getBatchSize(), taskDescriptions.size());
                List<String> batch = taskDescriptions.subList(i, end);

                List<String> categories = categorizeBatch(batch, 0);
                allCategories.addAll(categories);

                // Log progress
                int processed = Math.min(i + openAiConfigs.getBatchSize(), taskDescriptions.size());
                double percentage = (processed * 100.0) / taskDescriptions.size();
                LOGGER.info(String.format("%.1f%% complete (%d/%d records processed)",
                    percentage, processed, taskDescriptions.size()));

                if (end < taskDescriptions.size()) {
                    TimeUnit.SECONDS.sleep(openAiConfigs.getBatchDelay());
                }
            }

            LOGGER.info("Processing completed successfully");
            LOGGER.info(stats.getSummary());
            LOGGER.info("Total tokens used: {}", totalTokens);

        } catch (Exception e) {
            LOGGER.error("Processing failed", e);
            throw new RuntimeException("Processing failed", e);
        }
        return allCategories;
    }

    private List<String> categorizeBatch(List<String> descriptions, int retryCount) {
        try {
            LOGGER.info("Categorizing batch of descriptions: " + descriptions);
            String prompt = buildPrompt(descriptions);

            ChatCompletions response = openAiClient.getChatCompletions(
                openAiConfigs.getAzureOpenAiDeploymentName(),
                new ChatCompletionsOptions(List.of(
                    new ChatMessage(ChatRole.SYSTEM, "You are a helpful assistant that categorizes SED task descriptions."),
                    new ChatMessage(ChatRole.USER, prompt)
                ))
                .setTemperature(openAiConfigs.getTemperature())
                .setMaxTokens(openAiConfigs.getMaxTokens())
            );

            totalTokens += response.getUsage().getTotalTokens();
            String content = response.getChoices().get(0).getMessage().getContent();
            List<String> categories = List.of(content.trim().split("\n"));
            LOGGER.info("Got categories: " + categories);

            if (categories.size() != descriptions.size()) {
                throw new RuntimeException("Received incorrect number of categories. Was: " + categories.size() + ", expected: " + descriptions.size());
            }

            // Update statistics
            categories.forEach(stats::increment);

            return categories;

        } catch (Exception e) {
            if (retryCount < openAiConfigs.getMaxRetries()) {
                LOGGER.warn("Retry attempt {} after error: {}: {}",
                    retryCount + 1, e.getClass().getSimpleName(), e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(openAiConfigs.getBatchDelay());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return categorizeBatch(descriptions, retryCount + 1);
            } else {
                String errorMsg = String.format("Failed after %d attempts: %s: %s",
                        openAiConfigs.getMaxRetries(), e.getClass().getSimpleName(), e.getMessage());
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
        }
    }

    private String buildPrompt(List<String> descriptions) {
        return """
            For each of the following SED task descriptions, categorize it into one of these categories:
            - 'record': for creating new medication orders
            - 'change': for modifying existing medication orders
            - 'access': for viewing or accessing medication orders (including 'review' actions)
            - 'multiple': when more than one category applies to the description
            - 'unknown': when the description is unclear, unrelated, or too general

            Rules for categorization:
            1. Each description must be assigned exactly one category. If multiple categories could apply, use 'multiple'
            2. If a description mentions multiple distinct actions (record/change/access), use 'multiple'
            3. Words like "review", "view", "display" should be treated as 'access'
            4. "Attempt to order" or similar phrases should be treated as 'record'
            5. Generic descriptions like "summary of tasks" or "Clinical Decision Support" should be 'unknown'

            Descriptions to categorize:
            %s

            For each description, reply with only the category name in lowercase."""
            .formatted(String.join("\n", descriptions));
    }

    private File writeToFile(List<String> taskDescriptions, List<String> categories)
        throws IOException {
        File temp = File.createTempFile(reportFilename, ".csv");
        temp.deleteOnExit();

        // Write results
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(temp),
                Charset.forName("UTF-8").newEncoder());
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL)) {
            writer.write('\ufeff');
            csvPrinter.printRecord("Task Description", "Category");
            for (int i = 0; i < taskDescriptions.size(); i++) {
                csvPrinter.printRecord(Stream.of(taskDescriptions.get(i), categories.get(i)).toList());
            }
            String finalStats = stats.getSummary() + "\n\nTotal tokens used: " + totalTokens;
            csvPrinter.println();
            csvPrinter.printRecord(Stream.of(finalStats).toList());
        } catch (IOException e) {
            LOGGER.error(e);
        }
        LOGGER.info("Results written to {}", temp);
        return temp;
    }

    private void sendFileToUser(String recipient, File attachment) {
        LOGGER.info("Sending email to {} ", recipient);

        try {
            List<String> recipients = new ArrayList<String>();
            recipients.add(recipient);

            chplEmailFactory.emailBuilder()
                .recipients(recipients)
                .subject("Test Task Categorization Results")
                .htmlMessage("Please see attachment")
                .fileAttachments(Stream.of(attachment).toList())
                .sendEmail();
        } catch (EmailNotSentException e) {
            LOGGER.error(e);
        }
    }

    @Component
    @Getter
    private static final class OpenAiConfigs {
        private final String azureOpenAiKey;
        private final String azureOpenAiUrl;
        private final String azureOpenAiApiVersion;
        private final String azureOpenAiDeploymentName;
        private final int maxTokens;
        private final double temperature;
        private final int batchSize;
        private final int maxRetries;
        private final int batchDelay;

        @Autowired
        public OpenAiConfigs(Environment env) {
            // Azure OpenAI Configuration
            this.azureOpenAiKey = env.getProperty("azure.openai.apiKey");
            this.azureOpenAiUrl = env.getProperty("azure.openai.url");
            this.azureOpenAiApiVersion = env.getProperty("azure.openai.version");
            this.azureOpenAiDeploymentName = env.getProperty("azure.deployment.name");

            // Other Configuration
            this.maxTokens = Integer.parseInt(env.getProperty("azure.model.maxTokens", "1000"));
            this.temperature = Double.parseDouble(env.getProperty("azure.model.temperature", "0.7"));
            this.batchSize = Integer.parseInt(env.getProperty("azure.processing.batchSize", "10"));
            this.maxRetries = Integer.parseInt(env.getProperty("azure.processing.maxRetries", "3"));
            this.batchDelay = Integer.parseInt(env.getProperty("azure.processing.batchDelay", "1"));
        }
    }

    @Component
    private static final class SedDao extends BaseDAOImpl {

        public List<String> getUniqueTestTasksForCriterion(Long criterionId) {
            String hql = "SELECT DISTINCT tt.description "
                    + "FROM CertifiedProductDetailsEntity cpd, "
                    + "CertificationResultEntity cr, "
                    + "CertificationResultTestTaskEntity crtt, "
                    + "TestTaskEntity tt "
                    + "WHERE cpd.id = cr.certifiedProductId "
                    + "AND cpd.certificationStatusName IN (:activeStatuses) "
                    + "AND cr.id = crtt.certificationResultId "
                    + "AND tt.id = crtt.testTaskId "
                    + "AND cr.success = true "
                    + "AND cr.deleted = false "
                    + "AND crtt.deleted = false "
                    + "AND cr.certificationCriterionId = :criterionId";

            Query query = entityManager.createQuery(hql);
            query.setParameter("activeStatuses", CertificationStatusUtil.getActiveStatusNames());
            query.setParameter("criterionId", criterionId);

            List<String> results = query.getResultList();
            //the input to the AI is expecting one test task description per line
            //but it turns out some of our descriptions have newlines in them, so this ends up
            //not working because the AI thinks it's two items and returns two categories
            //when it's really all part of the same description - so here we remove the newlines
            return results.stream()
                .map(desc -> desc.replaceAll(System.lineSeparator(), " "))
                //We probably only need the line above, but because i'm looking at linux data
                //on a windows machine, when running locally the lines below are needed.
                //I don't think it hurts to leave them there for all environments.
                .map(desc -> desc.replaceAll("\r\n", " "))
                .map(desc -> desc.replaceAll("\n", " "))
                .collect(Collectors.toList());
        }
    }
}