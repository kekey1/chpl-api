package gov.healthit.chpl.scheduler.job.sed;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import lombok.extern.log4j.Log4j2;

@DisallowConcurrentExecution
@Log4j2(topic = "categorizeTestTasksJobLogger")
public class CategorizeTestTasksJob implements Job {

    @Override
    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        LOGGER.info("********* Starting the Categorize Test Tasks Job. *********");

        //TODO
        LOGGER.info("********* Completed the Categorize Test Tasks Job. *********");
    }

}