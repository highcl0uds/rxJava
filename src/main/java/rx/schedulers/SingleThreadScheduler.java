package rx.schedulers;

import rx.interfaces.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadScheduler implements Scheduler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "single-thread");
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
}
