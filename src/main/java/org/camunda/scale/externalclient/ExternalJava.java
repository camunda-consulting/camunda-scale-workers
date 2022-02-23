package org.camunda.scale.externalclient;


import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
import org.camunda.scale.workexecution.WorkExecution;
import org.camunda.scale.workexecution.WorkTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class ExternalJava {
    private static final Logger logger = Logger.getLogger(ExternalJava.class.getName());

    public static void main(String[] args) {

        // prepare back-off strategy for external task
        final BackoffStrategy backoffStrategy = new ExponentialBackoffStrategy(1000, 2, 16000);
        // bootstrap the client
        String baseUrl = "http://localhost:8080/engine-rest";

        List<ExternalTaskClient> listClients = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String workerId = "client-"+i;
            ExternalTaskClient client =
                    ExternalTaskClient.create()
                            .baseUrl(baseUrl)
                            .workerId(workerId)
                            .backoffStrategy(backoffStrategy)
                            .maxTasks(1)
                            .build();

            client.subscribe("work-to-do")
                    .lockDuration(60000)
                    .handler(
                            (externalTask, externalTaskService) -> {

                                WorkExecution workExecution = new WorkExecution();
                                workExecution.execute(workerId, externalTask.getId());

                                externalTaskService.complete(externalTask);


                            })
                    .open();
            listClients.add(client);
        }
        WorkTracker tracker = WorkTracker.getInstance();
        tracker.monitorTracker();
        logger.info("ExternalJava: the end");
    }


}
