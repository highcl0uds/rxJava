package rx.schedulers;

import rx.interfaces.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComputationScheduler implements Scheduler {

    private final int threadCount = Runtime.getRuntime().availableProcessors();

    private final ExecutorService executor = Executors.newFixedThreadPool(threadCount, r -> {
        Thread thread = new Thread(r, "computation-thread");
        thread.setDaemon(true);

        return thread;
    });

    @Override
    public void execute(Runnable r) {
        executor.execute(r);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    public int getThreadCount() {
        return threadCount;
    }
}
