package example;

import job.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RecvRtpJob extends Job {

    private static final Logger logger = LoggerFactory.getLogger(RecvRtpJob.class);

    public RecvRtpJob(String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);
    }

    @Override
    public void run() {
        logger.debug("RECV RTP @@@");
    }

}
