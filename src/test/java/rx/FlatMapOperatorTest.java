package rx;

import org.junit.jupiter.api.Test;
import rx.interfaces.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FlatMapOperatorTest {

    @Test
    void expandsEachItemIntoInnerStream() {
        List<String> result = new ArrayList<>();

        Observable.<String>create(emitter -> {
                    emitter.onNext("A");
                    emitter.onNext("B");
                    emitter.onComplete();
                })
                .flatMap(letter -> Observable.<String>create(inner -> {
                    inner.onNext(letter + "1");
                    inner.onNext(letter + "2");
                    inner.onComplete();
                }))
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

        assertEquals(List.of("A1", "A2", "B1", "B2"), result);
    }

    @Test
    void emptyInnerStreamDoesNotBreakOuterStream() {
        List<String> result = new ArrayList<>();

        Observable.<String>create(emitter -> {
                    emitter.onNext("X");
                    emitter.onNext("Y");
                    emitter.onComplete();
                })
                .flatMap(x -> Observable.<String>create(inner -> inner.onComplete()))
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

        assertTrue(result.isEmpty());
    }

    @Test
    void errorInMapperRoutedToOnError() {
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable.<String>create(emitter -> {
                    emitter.onNext("x");
                    emitter.onComplete();
                })
                .<String>flatMap(s -> {
                    throw new RuntimeException("mapper failed");
                })
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
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

        assertEquals("mapper failed", error.get().getMessage());
    }

    @Test
    void errorInInnerObservableRoutedToOnError() {
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable.<String>create(emitter -> {
                    emitter.onNext("x");
                    emitter.onComplete();
                })
                .<String>flatMap(x -> Observable.<String>create(inner -> {
                    throw new IllegalArgumentException("inner error");
                }))
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
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

        assertInstanceOf(IllegalArgumentException.class, error.get());
    }

    @Test
    void nullMapperThrowsNullPointerException() {
        Observable<String> obs = Observable.<String>create(emitter -> emitter.onNext("x"));
        assertThrows(NullPointerException.class, () -> obs.flatMap(null));
    }
}
