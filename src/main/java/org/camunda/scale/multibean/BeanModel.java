package org.camunda.scale.multibean;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.scale.beanthread.BeanThread;
import org.camunda.scale.workexecution.WorkExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

// This bean is the model to instantiate dynamically multiple beans

@Component("beanExecutorTaskHandlerWorkToDo")
public class BeanModel {
    private static final Logger logger = Logger.getLogger(BeanThread.class.getName());

    private String workerId;
    private final Set<String> inProgressActivity = new HashSet<>();

    // Attention, the topic "work-to-do" must be handled at a time by only one bean.
    // So, when you replace the value here, check that all other Bean does not get it.
    public void subscribeTaskClient() {
        String baseUrl = "http://localhost:8080/engine-rest";
        ExternalTaskClient client =
                ExternalTaskClient.create()
                        .baseUrl(baseUrl)
                        .workerId(workerId)
                        .maxTasks(1)
                        .build();

        client.subscribe("work-to-do")
                .lockDuration(60000)
                .handler((externalTask, externalTaskService) -> {
            synchronized (inProgressActivity) {
                inProgressActivity.add(externalTask.getId());
            }

            logger.info("BeanThreadModel.execute : >>>>>>>>>>> start ["+externalTask.getId()
                    +"] in parallel "+inProgressActivity.size() );

            WorkExecution workExecution = new WorkExecution();
            workExecution.execute("BatchThread", externalTask.getId());
            externalTaskService.complete(externalTask);

            synchronized (inProgressActivity) {
                inProgressActivity.remove(externalTask.getId());
            }

            logger.info("BeanThreadModel.execute : >>>>>>>>>>>> end ["+externalTask.getId()+"]");
        })
                .open();
    }

    /**
     * Set the workerId
     * @param workerId worker Id
     */
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}
