package rx.interfaces;

public interface ObservableEmitter<T> extends Disposable {

    void onNext(T item);

    void onError(Throwable t);

    void onComplete();

    boolean isDisposed();
}
