package rx.interfaces;

@FunctionalInterface
public interface ObservableOnSubscribe<T> {

    void subscribe(ObservableEmitter<T> emitter) throws Throwable;
}
