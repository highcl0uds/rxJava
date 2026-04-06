package rx.schedulers;

import rx.interfaces.Scheduler;

public final class Schedulers {

    private static final Scheduler ioThreadScheduler = new IOThreadScheduler();
    private static final Scheduler computationScheduler = new ComputationScheduler();
    private static final Scheduler singleThreadScheduler = new SingleThreadScheduler();

    private Schedulers() {}

    public static Scheduler io() {
        return ioThreadScheduler;
    }

    public static Scheduler computation() {
        return computationScheduler;
    }

    public static Scheduler single() {
        return singleThreadScheduler;
    }
}
