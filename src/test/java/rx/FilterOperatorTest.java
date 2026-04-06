package rx;

import org.junit.jupiter.api.Test;
import rx.interfaces.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FilterOperatorTest {

    @Test
    void keepsOnlyMatchingItems() {
        List<Integer> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
                    for (int i = 1; i <= 5; i++) emitter.onNext(i);
                    emitter.onComplete();
                })
                .filter(n -> n % 2 == 0)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        result.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertEquals(List.of(2, 4), result);
    }

    @Test
    void noMatchProducesEmptyStreamWithOnComplete() {
        List<Integer> result = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(3);
                    emitter.onComplete();
                })
                .filter(n -> n > 100)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        result.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail(t);
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);
                    }
                });

        assertTrue(result.isEmpty());
        assertTrue(completed.get());
    }

    @Test
    void exceptionInPredicateRoutedToOnError() {
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable.<String>create(emitter -> {
                    emitter.onNext("valid");
                    emitter.onNext(null);
                    emitter.onComplete();
                })
                .filter(s -> s.length() > 3)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                    }

                    @Override
                    public void onError(Throwable t) {
                        error.set(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertInstanceOf(NullPointerException.class, error.get());
    }

    @Test
    void filterCombinedWithMap() {
        List<String> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
                    for (int i = 1; i <= 6; i++) emitter.onNext(i);
                    emitter.onComplete();
                })
                .filter(n -> n % 2 == 0)
                .map(n -> "val=" + n)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        result.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertEquals(List.of("val=2", "val=4", "val=6"), result);
    }

    @Test
    void nullPredicateThrowsNullPointerException() {
        Observable<Integer> obs = Observable.<Integer>create(emitter -> emitter.onNext(1));
        assertThrows(NullPointerException.class, () -> obs.filter(null));
    }
}
