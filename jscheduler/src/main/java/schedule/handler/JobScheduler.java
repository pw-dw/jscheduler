package schedule.handler;

import job.base.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ConcurrentCyclicFIFO;

import java.util.Comparator;
import java.util.concurrent.*;

public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    ////////////////////////////////////////////////////////////////////////////////

    private final String ownerName;
    private final ExecutorService priorityJobSelector;
    private final ExecutorService priorityJobPoolExecutor;
    private final PriorityBlockingQueue<Job> priorityQueue;

    ////////////////////////////////////////////////////////////////////////////////

    public JobScheduler(String ownerName, int poolSize, int queueSize) {
        this.ownerName = ownerName;
        priorityJobPoolExecutor = Executors.newFixedThreadPool(poolSize);
        priorityQueue = new PriorityBlockingQueue<>(
                queueSize,
                Comparator.comparing(Job::getPriority)
        );

        priorityJobSelector = Executors.newSingleThreadExecutor();
        priorityJobSelector.execute(this::run);
    }

    private void run() {
        while (true) {
            try {
                Job job = priorityQueue.poll();
                if (job == null) {
                    continue;
                }

                if (job.getIsFinished()) {
                    logger.debug("[{}]-[{}] is finished.", ownerName, job.getName());
                    continue;
                }

                if (!job.isLasted() && job.decCurRemainRunCount() <= 0) {
                    logger.trace("[{}]-[{}] is finished.", ownerName, job.getName());
                    continue;
                }

                priorityJobPoolExecutor.execute(job);
                logger.trace("[{}]-[{}]: is running. ({})", ownerName, job.getName(), job);

                int interval = job.getInterval();
                if (interval > 0) {
                    new Thread(new FutureScheduler(interval, job)).start();
                    logger.trace("[{}]-[{}] is scheduled.", ownerName, job.getName());
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public boolean schedule(Job job) {
        int initialDelay = job.getInitialDelay();
        if (initialDelay > 0) {
            new Thread(new FutureScheduler(initialDelay, job)).start();
            return true;
        }

        return priorityQueue.offer(job);
    }

    public void stop(Job job) {
        job.setIsFinished(true);
    }

    public void stopAll() {
        priorityQueue.clear();
        priorityJobSelector.shutdown();
        priorityJobPoolExecutor.shutdown();
    }

    ////////////////////////////////////////////////////////////////////////////////

    private class FutureScheduler implements Runnable {

        private final long sleepTime;
        private final TimeUnit timeUnit;

        private final Job job;

        ////////////////////////////////////////////////////////////////////////////////

        public FutureScheduler(long sleepTime, Job job) {
            this.sleepTime = sleepTime;
            this.timeUnit = job.getTimeUnit();
            this.job = job;
        }

        ////////////////////////////////////////////////////////////////////////////////

        @Override
        public void run() {
            try {
                timeUnit.sleep(sleepTime);

                priorityQueue.offer(job);
            } catch (Exception e) {
                job.setIsFinished(true);
            }
        }

    }

}
