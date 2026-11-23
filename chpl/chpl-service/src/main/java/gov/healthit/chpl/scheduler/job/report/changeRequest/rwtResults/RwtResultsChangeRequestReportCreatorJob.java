package gov.healthit.chpl.scheduler.job.report.changeRequest.rwtResults;

import java.time.LocalDate;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.manager.CertificationBodyManager;
import gov.healthit.chpl.scheduler.SchedulerSecurityContextService;
import gov.healthit.chpl.scheduler.job.QuartzJob;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "rwtResultsChangeRequestReportCreatorJobLogger")
public class RwtResultsChangeRequestReportCreatorJob extends QuartzJob {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RwtResultsChangeRequestReportSummaryDataCollector dataCollector;

    @Autowired
    private CertificationBodyManager certificationBodyManager;

    @Autowired
    private RwtResultsChangeRequestReportDAO rwtResultsChangeRequestReportDao;

    @Autowired
    private SchedulerSecurityContextService securityContextService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

        LOGGER.info("********* Starting RWT Results Change Request Report Creator job. *********");
        securityContextService.setAdminSecurityContext();

        // We need to manually create a transaction in this case because of how
        // AOP works. When a method is annotated with @Transactional, the transaction
        // wrapper is only added if the object's proxy is called. The object's proxy is
        // not called when the method is called from within this class. The object's
        // proxy is called when the method is public and is called from a different
        // object.
        // https://stackoverflow.com/questions/3037006/starting-new-transaction-in-spring-bean
        TransactionOperations transactionOperations = new TransactionTemplate(transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        transactionOperations.executeWithoutResult(status -> {
                try {
                    LOGGER.info("Collecting report data");
                    List<Long> acbIds = certificationBodyManager.getAll().stream()
                            .map(CertificationBody::getId)
                            .toList();
                    List<RwtResultsChangeRequestSummary> reportRows = dataCollector.collect(acbIds);
                    rwtResultsChangeRequestReportDao.deleteByReportDate(LocalDate.now());
                    rwtResultsChangeRequestReportDao.save(reportRows);
                    LOGGER.info("Done Collecting report data");
                } catch (Exception e) {
                    LOGGER.error(e);
                }
        });
        LOGGER.info("********* Completed RWT Results Change Request Report Creator job. *********");
    }
}
