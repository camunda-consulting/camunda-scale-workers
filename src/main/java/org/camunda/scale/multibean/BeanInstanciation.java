package org.camunda.scale.multibean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * This implementation generate multiple bean. Each bean is a worker
 *
 */
@Component
public class BeanInstanciation {
    private static final Logger logger = Logger.getLogger(BeanInstanciation.class.getName());

    @Autowired
    ConfigurableListableBeanFactory configurableListableBeanFactory;

    BeanInstanciation() {
    }

    @PostConstruct
    void postConstruct() {
        createInstances(10);
    }

    /**
     * Create multiple bean instance
     * @param numberOfInstances number of instance to creates
     */
    private void createInstances(int numberOfInstances) {
        logger.info("BeanInstanciation- start");
       for (int i=0;i<numberOfInstances;i++) {

           DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
           BeanDefinitionBuilder b = BeanDefinitionBuilder.rootBeanDefinition(BeanModel.class)
                   .addPropertyValue("workerId", "workToDo-" + i);
           beanFactory.registerBeanDefinition("beanModel", b.getBeanDefinition());
           BeanModel bean = beanFactory.getBean(BeanModel.class);
           bean.subscribeTaskClient();
       }
        logger.info("BeanInstanciation- end");
    }
}
