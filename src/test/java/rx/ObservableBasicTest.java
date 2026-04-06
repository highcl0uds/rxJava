package rx;

import org.junit.jupiter.api.Test;
import rx.interfaces.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ObservableBasicTest {

    @Test
    void emitsItemsInOrder() {
        List<Integer> received = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        }).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected error");
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(List.of(1, 2, 3), received);
    }

    @Test
    void emptyStreamCallsOnComplete() {
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.<Object>create(emitter -> emitter.onComplete())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onNext(Object item) {
                        fail("No items expected");
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("No error expected");
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);
                    }
                });

        assertTrue(completed.get());
    }

    @Test
    void onCompleteCalledOnlyOnce() {
        List<String> events = new ArrayList<>();

        Observable.<Object>create(emitter -> {
            emitter.onComplete();
            emitter.onComplete();
        }).subscribe(new Observer<Object>() {
            @Override
            public void onNext(Object item) {
            }

            @Override
            public void onError(Throwable t) {
                events.add("error");
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        });

        assertEquals(List.of("complete"), events);
    }

    @Test
    void onErrorCalledOnlyOnce() {
        List<String> events = new ArrayList<>();

        Observable.<Object>create(emitter -> {
            emitter.onError(new RuntimeException("first"));
            emitter.onError(new RuntimeException("second"));
        }).subscribe(new Observer<Object>() {
            @Override
            public void onNext(Object item) {
            }

            @Override
            public void onError(Throwable t) {
                events.add(t.getMessage());
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        });

        assertEquals(List.of("first"), events);
    }

    @Test
    void onErrorAfterOnCompleteIsIgnored() {
        List<String> events = new ArrayList<>();

        Observable.<Object>create(emitter -> {
            emitter.onComplete();
            emitter.onError(new RuntimeException("late error"));
        }).subscribe(new Observer<Object>() {
            @Override
            public void onNext(Object item) {
            }

            @Override
            public void onError(Throwable t) {
                events.add("error");
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        });

        assertEquals(List.of("complete"), events);
    }

    @Test
    void exceptionInSourceRoutedToOnError() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Object>create(emitter -> {
            throw new IllegalStateException("source exploded");
        }).subscribe(new Observer<Object>() {
            @Override
            public void onNext(Object item) {
                fail("No items expected");
            }

            @Override
            public void onError(Throwable t) {
                caught.set(t);
            }

            @Override
            public void onComplete() {
                fail("No complete expected");
            }
        });

        assertInstanceOf(IllegalStateException.class, caught.get());
        assertEquals("source exploded", caught.get().getMessage());
    }

    @Test
    void nullSourceThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Observable.create(null));
    }

    @Test
    void nullObserverThrowsNullPointerException() {
        Observable<Integer> obs = Observable.<Integer>create(emitter -> emitter.onNext(1));
        assertThrows(NullPointerException.class, () -> obs.subscribe(null));
    }
}
