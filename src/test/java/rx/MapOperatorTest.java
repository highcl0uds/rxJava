package rx;

import org.junit.jupiter.api.Test;
import rx.interfaces.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MapOperatorTest {

    @Test
    void transformsAllItems() {
        List<String> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onNext(3);
                    emitter.onComplete();
                })
                .map(n -> "item" + n)
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

        assertEquals(List.of("item1", "item2", "item3"), result);
    }

    @Test
    void chainedMapsAppliedInOrder() {
        List<Integer> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(2);
                    emitter.onComplete();
                })
                .map(n -> n * 3)
                .map(n -> n + 4)
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

        assertEquals(List.of(10), result);
    }

    @Test
    void exceptionInMapperRoutedToOnError() {
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(0);
                    emitter.onComplete();
                })
                .map(n -> 10 / n)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        fail("No items expected");
                    }

                    @Override
                    public void onError(Throwable t) {
                        error.set(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertInstanceOf(ArithmeticException.class, error.get());
    }

    @Test
    void nullMapperThrowsNullPointerException() {
        Observable<Integer> obs = Observable.<Integer>create(emitter -> emitter.onNext(1));
        assertThrows(NullPointerException.class, () -> obs.map(null));
    }
}
