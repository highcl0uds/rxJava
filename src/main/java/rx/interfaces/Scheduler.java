package rx.interfaces;

public interface Scheduler {

    void execute(Runnable r);

    void shutdown();
}
