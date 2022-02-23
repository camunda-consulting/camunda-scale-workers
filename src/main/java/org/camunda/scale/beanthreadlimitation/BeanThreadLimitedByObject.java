package org.camunda.scale.beanthreadlimitation;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.scale.workexecution.WorkExecution;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * This bean register N external client. Each client is independent
 *
 */

@Component
public class BeanThreadLimitedByObject {

        private static final Logger logger = Logger.getLogger(BeanThreadLimitedByObject.class.getName());


        /**
         * How many thread do I want to execute at a time?
         * Nb: we instantiate a ThreadPoolExecutor, then we can access the size
         * Nota: you can set the maxTask() to this number
         */
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        ExternalTaskClient externalTaskClient;

        public BeanThreadLimitedByObject() {
            String baseUrl = "http://localhost:8080/engine-rest";

            List<ExternalTaskClient> listClients = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String workerId="client-"+i;
                ExternalTaskClient client =
                        ExternalTaskClient.create()
                                .baseUrl(baseUrl)
                                .workerId(workerId)
                                .maxTasks(1)
                                .build();

                client.subscribe("work-to-do-LimitedByObject-to-replace")
                        .lockDuration(60000)
                        .handler(
                                (externalTask, externalTaskService) -> {
                                    WorkExecution workExecution = new WorkExecution();
                                    workExecution.execute(workerId, externalTask.getId());
                                    try {
                                        externalTaskService.complete(externalTask);
                                    } catch(Exception e) {
                                        logger.severe(">>>>>>>>>>>>>>>>>>>> Can't complete taskId["+externalTask.getId()+"] by worker["+workerId+"]");
                                    }

                                })
                        .open();
                listClients.add(client);
            }

    }


}
