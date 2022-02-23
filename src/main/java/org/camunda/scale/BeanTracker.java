package org.camunda.scale;

import org.camunda.scale.workexecution.WorkTracker;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@EnableScheduling
public class BeanTracker {
    @Scheduled(fixedDelay = 60000)
    public void ping() {
        WorkTracker.getInstance().checkTracker();
    }
}
