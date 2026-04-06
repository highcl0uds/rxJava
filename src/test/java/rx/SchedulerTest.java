package rx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import rx.interfaces.Observer;
import rx.schedulers.ComputationScheduler;
import rx.schedulers.IOThreadScheduler;
import rx.schedulers.Schedulers;
import rx.schedulers.SingleThreadScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    private IOThreadScheduler ioScheduler;
    private ComputationScheduler computationScheduler;
    private SingleThreadScheduler singleScheduler;

    @AfterEach
    void tearDown() {
        if (ioScheduler != null) ioScheduler.shutdown();
        if (computationScheduler != null) computationScheduler.shutdown();
        if (singleScheduler != null) singleScheduler.shutdown();
    }

    @Test
    void ioSchedulerRunsTaskInIoThread() throws InterruptedException {
        ioScheduler = new IOThreadScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        ioScheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(threadName.get().contains("io-thread"));
    }

    @Test
    void ioSchedulerRunsConcurrentTasks() throws InterruptedException {
        ioScheduler = new IOThreadScheduler();
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < taskCount; i++) {
            final int val = i;
            ioScheduler.execute(() -> {
                results.add(val);
                latch.countDown();
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(taskCount, results.size());
    }

    @Test
    void computationSchedulerRunsTaskInComputationThread() throws InterruptedException {
        computationScheduler = new ComputationScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        computationScheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(threadName.get().contains("computation-thread"));
    }

    @Test
    void computationSchedulerThreadCountMatchesCpuCores() {
        computationScheduler = new ComputationScheduler();
        assertEquals(Runtime.getRuntime().availableProcessors(), computationScheduler.getThreadCount());
    }

    @Test
    void singleSchedulerRunsAllTasksInOneThread() throws InterruptedException {
        singleScheduler = new SingleThreadScheduler();
        int taskCount = 5;
        CountDownLatch latch = new CountDownLatch(taskCount);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < taskCount; i++) {
            singleScheduler.execute(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(1, threadNames.size());
        assertTrue(threadNames.iterator().next().contains("single-thread"));
    }

    @Test
    void singleSchedulerPreservesTaskOrder() throws InterruptedException {
        singleScheduler = new SingleThreadScheduler();
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        singleScheduler.execute(() -> {
            order.add(1);
            latch.countDown();
        });
        singleScheduler.execute(() -> {
            order.add(2);
            latch.countDown();
        });
        singleScheduler.execute(() -> {
            order.add(3);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(List.of(1, 2, 3), order);
    }

    @Test
    void schedulersFactoryReturnsSingletons() {
        assertSame(Schedulers.io(), Schedulers.io());
        assertSame(Schedulers.computation(), Schedulers.computation());
        assertSame(Schedulers.single(), Schedulers.single());
    }

    @Test
    void subscribeOnRunsSourceInGivenThread() throws InterruptedException {
        ioScheduler = new IOThreadScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sourceThread = new AtomicReference<>();

        Observable.<String>create(emitter -> {
                    sourceThread.set(Thread.currentThread().getName());
                    emitter.onNext("data");
                    emitter.onComplete();
                })
                .subscribeOn(ioScheduler)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(sourceThread.get().contains("io-thread"));
    }

    @Test
    void subscribeOnNullSchedulerThrowsNullPointerException() {
        Observable<Integer> obs = Observable.<Integer>create(emitter -> emitter.onNext(1));
        assertThrows(NullPointerException.class, () -> obs.subscribeOn(null));
    }

    @Test
    void observeOnDeliversEventsInGivenThread() throws InterruptedException {
        singleScheduler = new SingleThreadScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> observerThread = new AtomicReference<>();

        Observable.<String>create(emitter -> {
                    emitter.onNext("hello");
                    emitter.onComplete();
                })
                .observeOn(singleScheduler)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        observerThread.set(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(observerThread.get().contains("single-thread"));
    }

    @Test
    void observeOnNullSchedulerThrowsNullPointerException() {
        Observable<Integer> obs = Observable.<Integer>create(emitter -> emitter.onNext(1));
        assertThrows(NullPointerException.class, () -> obs.observeOn(null));
    }

    @Test
    void subscribeOnAndObserveOnUseDistinctThreads() throws InterruptedException {
        ioScheduler = new IOThreadScheduler();
        singleScheduler = new SingleThreadScheduler();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sourceThread = new AtomicReference<>();
        AtomicReference<String> observerThread = new AtomicReference<>();

        Observable.<String>create(emitter -> {
                    sourceThread.set(Thread.currentThread().getName());
                    emitter.onNext("value");
                    emitter.onComplete();
                })
                .subscribeOn(ioScheduler)
                .observeOn(singleScheduler)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        observerThread.set(Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(sourceThread.get().contains("io-thread"));
        assertTrue(observerThread.get().contains("single-thread"));
        assertNotEquals(sourceThread.get(), observerThread.get());
    }
}
