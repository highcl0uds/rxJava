package rx;

import org.junit.jupiter.api.Test;
import rx.interfaces.Disposable;
import rx.interfaces.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DisposableTest {

    @Test
    void isDisposedAfterOnComplete() {
        Disposable[] ref = new Disposable[1];

        ref[0] = Observable.<Object>create(emitter -> emitter.onComplete())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onNext(Object item) {
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertTrue(ref[0].isDisposed());
    }

    @Test
    void isDisposedAfterOnError() {
        Disposable[] ref = new Disposable[1];

        ref[0] = Observable.<Object>create(emitter -> emitter.onError(new RuntimeException()))
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onNext(Object item) {
                    }

                    @Override
                    public void onError(Throwable t) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertTrue(ref[0].isDisposed());
    }

    @Test
    void disposeStopsItemDelivery() {
        List<Integer> received = new ArrayList<>();
        AtomicReference<Disposable> ref = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
                    ref.set(emitter);
                    for (int i = 1; i <= 10; i++) {
                        if (emitter.isDisposed()) break;
                        emitter.onNext(i);
                    }
                    emitter.onComplete();
                })
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        received.add(item);
                        if (item == 3) ref.get().dispose();
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertTrue(received.size() <= 4);
        assertTrue(received.contains(3));
        assertTrue(ref.get().isDisposed());
    }

    @Test
    void onNextIgnoredAfterDispose() {
        List<Integer> received = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.dispose();
                    emitter.onNext(2);
                    emitter.onNext(3);
                })
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        received.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        assertEquals(List.of(1), received);
    }
}
