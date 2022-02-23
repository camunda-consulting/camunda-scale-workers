package org.camunda.scale.workexecution;

import java.util.HashMap;
import java.util.logging.Logger;

public class WorkExecution {
    private static final HashMap<String, Long> registerSignature = new HashMap<>();
    Logger logger = Logger.getLogger(WorkExecution.class.getName());

    public void execute(String workerId, String taskId) {
        try {
            if (registerSignature.containsKey(taskId)) {
                logger.info("WorkExecution - this [" + taskId + "] show up multiple time ! " + Long.valueOf(registerSignature.get(taskId) + 1));
            }
            synchronized (registerSignature) {
                long execution = registerSignature.getOrDefault(taskId, Long.valueOf(0));
                registerSignature.put(taskId, Long.valueOf(execution + 1));
            }

            // is this taskId was already registered?
            logger.info("WorkExecution - start [" + taskId + "] workerId["+workerId+"]" );
            WorkTracker.getInstance().startActivity(taskId);
            Thread.sleep(14 * 1000);
            WorkTracker.getInstance().endActivity(taskId);
            logger.info("WorkExecution - end [" + taskId + "] workerId["+workerId+"] ");
        } catch (Exception e) {
            logger.info("WorkExecution - error [" + taskId + "] workerId["+workerId+"] " + e);
        }
    }
}
