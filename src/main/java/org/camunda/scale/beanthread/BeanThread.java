package org.camunda.scale.beanthread;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.scale.workexecution.WorkExecution;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * This worker use a simple thread, one execution at a time.
 * The execution immediately register the work in a executorPool, and return. Execution is then delay.
 * To protect the machine, the executorPool creates maximum 20 threads at a time. So, maximum 20 executions is processed
 * Attention:
 * - Because the handling is very fast (just register the work to do), if there is 2000 tasks on the Camunda Engine, this worker will get all, and put in the queue.
 * If there is an another worker in the system, it will have nothing to do, and this worker will have a large list to process
 * - after X ms, task is unlocked. Then, Camunda Engine will propose the same task, and this worker will get it twice.
 *  In the executorPool, task is register twice and will be executed twice.
 *
 *  Use this pattern only if the job is small and can the queue is not very big. Else see beanthreadlimitation package
  */


@Configuration
public class BeanThread {

    private static final Logger logger = Logger.getLogger(BeanThread.class.getName());

    private String workerId;
    /**
     * This object is used only to track the activity - not use it
     */
    private final Set<String> inProgressActivity = new HashSet<>();

    /**
     * How many threads do I want to execute at a time?
     */
    ThreadPoolExecutor executorPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);


    // Attention, the topic "work-to-do" must be handled at a time by only one bean.
    // So, when you replace the value here, check that all other Bean does not get it.
    @Bean("beanThreadWorkToDo")
    @ExternalTaskSubscription(
            topicName = "work-to-do-replaceit",
            lockDuration = 60000)
    public ExternalTaskHandler workToDoHandler() {
        return (externalTask, externalTaskService) -> {
            synchronized (inProgressActivity) {
                inProgressActivity.add(externalTask.getId());
            }

            logger.info("BeanThread.execute : >>>>>>>>>>> start ["+externalTask.getId()
                    +"] in parallel "+ inProgressActivity.size() + "In pool "+ executorPool.getQueue().size());

            executorPool.submit(() -> {
                        WorkExecution workExecution = new WorkExecution();
                        workExecution.execute("BatchThread", externalTask.getId());
                        externalTaskService.complete(externalTask);
                    }
            );
            synchronized (inProgressActivity) {
                inProgressActivity.remove(externalTask.getId());
            }

            logger.info("BeanThread.execute : >>>>>>>>>>>> end ["+externalTask.getId()+"]");
        };
    }

    /**
     * Set the workerId
     * @param workerId worker Id
     */
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getWorkerId() {
        return this.workerId;
    }
}
