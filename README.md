# camunda-scale-workers
This project demonstrates different ways to scale a worker in the same Java Machine.

# Scope
A process (worktodo.bpmn) has multi-instance service tasks, and each process instance generates 200 tasks.
The service task to perform (org.camunda.scale.workexecution.WorkerExecution) sleeps 14 seconds in order to simulate something to perform.
With only one worker executing in sequential the treatment, it needs than 200*14 s = 2800 s, so 46 mn.


Note: in all options after, increasing the number of threads after 40 does not improve performances. 
WorkerExecution has a synchronized() method to track the number of execution in progress and to determine when the work is finished


# Operate

Start a Camunda Engine, started on localhost:8080 
Deploy on this engine the process worktodo.bpm

Play with the topic-name on each prototype. Use "work-to-do" to setup the correct topic.

Start [org.camunda.scale.BeanThreadApplication](https://github.com/camunda-consulting/camunda-scale-workers/blob/main/src/main/java/org/camunda/scale/BeanThreadApplication.java) 


On the Camunda Engine, create a process instance in worktodo.

# option 1 Multiple Workers objects:

Visit org.camunda.scale.externalclient.External
This solution is a Java program that creates multiple Client Workers.
Each client run in a different thread.
Starting the software with numberOfClients = 20, the total time to execute the treatment is 3 mn 39.


20 clients => 218704 ( 3mn 38)
40 clients => 134,384 ms for 236 (2 mn 14 )
80 clients => 128372

# Option 2 BeanThreadApplication
Visit org.camunda.scale.beanthread.BeanThread
In this implementation, each call immediately starts a new thread, which handles the execution.
To control the maximum number of threads running on the machine, a ThreadPoolExecutor is used, with a limit of 40 threads.

Attention: This method has a limitation. Because any task is immediately taken in charge by the client, it locks the 200 tasks immediately. Because it takes time to process it, then the lock expires on some tasks. Camunda resubmits them.
Using this method implies having a big lock duration or using the next option

# Option 3 BeanThreadApplication Limited
3.1 Visit org.camunda.scale.beanthreadlimitation.BeanThreadLimitedByExecutor
Same as before, except when the ThreadPool is full, the external client is stopped. The client does not accept any new task.
To do the stop() and start, a new thread has to be used. External task does not accept that the handle thread does this kind of operation.
Note: this way to handle the start/stop is very dynamique, and can be used to resize the number of workers differently in the day. 

3.2 Visit org.camunda.scale.beanthreadlimitation.BeanThreadLimitedByObject
In this implementation, the bean created a limited number of ExternalTask Client.

Note: you have to change the topic to get the work between beanthread/BeanThread and beanthreadlimitation/BeanThreadLimited

# Option 4 : Spring Boot Dynamic beans
Visit org.camunda.scale.beanthreadlimitation.BeanInstanciation 

Create dynamically multiple beans, to have multiple threads.

Note: the ExternalTaskClient is created by the bean, and it's not possible to duplicate a org.camunda.scale.beanthread.BeanThread class for example.
The BeanTread pattern is more convenient than the BeanModel.

# Option 5 Spring Boot multiple workers
In the spring boot config, describe multiple environment. Each environment starts its own workers.
Recommendation:
* Each worker should have a different worker ID
* Set the max task to 1
  ==> To verify
