package rx;

import rx.interfaces.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class Emitter<T> implements ObservableEmitter<T>, Disposable {

    private final Observer<T> observer;
    private final AtomicBoolean isTerminated = new AtomicBoolean(false);

    Emitter(Observer<T> oberver) {
        this.observer = oberver;
    }

    @Override
    public void onNext(T item) {
        if (isTerminated.get()) return;
        observer.onNext(item);
    }

    @Override
    public void onError(Throwable t) {
        if (isTerminated.compareAndSet(false, true)) observer.onError(t);
    }

    @Override
    public void onComplete() {
        if (isTerminated.compareAndSet(false, true)) observer.onComplete();
    }

    @Override
    public void dispose() {
        isTerminated.set(true);
    }

    @Override
    public boolean isDisposed() {
        return isTerminated.get();
    }

}
