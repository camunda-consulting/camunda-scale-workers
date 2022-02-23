package org.camunda.scale.beanthreadlimitation;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.scale.workexecution.WorkExecution;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
* This worker use a simple thread, one execution at a time.
* The execution immediately register the work in a executorPool, and return. Execution is then delay.
* To protect the machine, the executorPool creates maximum 20 threads at a time. So, maximum 20 executions is processed
 * If the executorPool maximum limit is reach, then the worker is stops. It will not accept any new task.
 * When the queue is under the limit, it will be restarted, to accept new task.
 * This avoid the starvation and the "execute multiple time" describe in the beanthread class

 * This implementation use a ThreadPoolExecutor
        */
@Component
public class BeanThreadLimitedByExecutor {

    private static final Logger logger = Logger.getLogger(BeanThreadLimitedByExecutor.class.getName());

    public final static int MAX_THREADS = 20;
    /**
     * This object is used only to track the activity - not use it
     */
    private final Set<String> inProgressActivity = new HashSet<>();

    /**
     * How many thread do I want to execute at a time?
     * Nb: we instantiate a ThreadPoolExecutor, then we can access the size
     * Nota: you can set the maxTask() to this number
     */
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);

    ThreadPoolExecutor externalTaskController = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public BeanThreadLimitedByExecutor() {
    }


    @PostConstruct
    public void postConstruct() {
        logger.info("BeanThreadLimitedByExecutor: postContruct");
        String baseUrl = "http://localhost:8080/engine-rest";

            ExternalTaskClient client =
                    ExternalTaskClient.create()
                            .baseUrl(baseUrl)
                            .workerId("WorkBeanThreadExecutorLimited")
                            .maxTasks(1)
                            .build();

        // Attention, the topic "work-to-do" must be handled at a time by only one bean.
        // So, when you replace the value here, check that all other Bean does not get it.
        client.subscribe("work-to-do-limitedbyexecutor")
                .lockDuration(60000)
                .handler((externalTask, externalTaskService) -> {

                    synchronized (inProgressActivity) {
                        inProgressActivity.add(externalTask.getId());
                    }

                    logger.info("BeanThread.execute : >>>>>>>>>>> start [" + externalTask.getId()
                            + "] in parallel " + inProgressActivity.size() + ", queueSize=" + executor.getQueue().size());
                    if (executor.getQueue().size() >= MAX_THREADS && client.isActive()) {
                        // sleep, we don't want to accept any new job now
                        // If we continue, we will lock all the jobs in Camunda, and we can't work on it now.
                        // Keep them in the Camunda sid, then another worker can work on it.
                        // Just accept all works we can process at this moment
                        logger.info("BeanThreadLimitedByExecutor.execute: >>>>>>>>>>>>>>>>>>>>> Too much item in the queue, stop the subscription <<<<<<<<<<<<<<<<");

                        // stop now. Stop wait that all threads are ended, so to commit a suicide, we have to start a new thread.
                        externalTaskController.submit(() -> client.stop());


                    }
                    executor.submit(() -> {
                                WorkExecution workExecution = new WorkExecution();
                                workExecution.execute("BatchThreadLimitedByExecutor", externalTask.getId());
                                externalTaskService.complete(externalTask);
                                logger.info("BeanThreadLimitedByExecutor.execute: how is the client? active="+client.isActive());
                                if (!client.isActive() && executor.getQueue().size() < MAX_THREADS) {
                                    logger.info("BeanThreadLimitedByExecutor.execute: >>>>>>>>>>>>>>>>>>>>>>> Restart the client <<<<<<<<<<<<<<<<<<");
                                    // must be an external thread to operate
                                    externalTaskController.submit(()-> client.start());
                                }
                                logger.info("BeanThreadLimitedByExecutor.execute: end of executor");

                            }
                    );
                    synchronized (inProgressActivity) {
                        inProgressActivity.remove(externalTask.getId());
                    }

                    logger.info("BeanThreadLimitedByExecutor.execute : >>>>>>>>>>>> end [" + externalTask.getId() + "] queueSize="+executor.getQueue().size());
                })
                .open();
    }


}
