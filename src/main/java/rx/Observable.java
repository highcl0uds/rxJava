package rx;

import rx.interfaces.*;

import java.util.function.Function;
import java.util.function.Predicate;

public class Observable<T> {

    private final ObservableOnSubscribe<T> source;

    private Observable(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        if (source == null) throw new NullPointerException("NULL source detected");
        return new Observable<>(source);
    }

    public Disposable subscribe(Observer<T> observer) {
        if (observer == null) throw new NullPointerException("NULL observer detected");

        Emitter<T> emitter = new Emitter<>(observer);
        try {
            source.subscribe(emitter);
        } catch (Throwable t) {
            emitter.onError(t);
        }
        return emitter;
    }

    public <R> Observable<R> map(Function<T, R> mapper) {
        if (mapper == null) throw new NullPointerException("NULL mapper detected");
        return Observable.create(emitter ->
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (emitter.isDisposed()) return;
                    try {
                        emitter.onNext(mapper.apply(item));
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onComplete() {
                    emitter.onComplete();
                }
            })
        );
    }

    public Observable<T> filter(Predicate<T> predicate) {
        if (predicate == null) throw new NullPointerException("NULL predicate detected");
        return Observable.create(emitter ->
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (emitter.isDisposed()) return;
                    try {
                        if (predicate.test(item)) {
                            emitter.onNext(item);
                        }
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onComplete() {
                    emitter.onComplete();
                }
            })
        );
    }

    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        if (mapper == null) throw new NullPointerException("NULL mapper detected");
        return Observable.create(emitter ->
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    if (emitter.isDisposed()) return;
                    try {
                        Observable<R> inner = mapper.apply(item);
                        inner.subscribe(new Observer<R>() {
                            @Override
                            public void onNext(R i) {
                                emitter.onNext(i);
                            }

                            @Override
                            public void onError(Throwable t) {
                                emitter.onError(t);
                            }

                            @Override
                            public void onComplete() {}
                        });
                    } catch (Throwable t) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.onError(t);
                }

                @Override
                public void onComplete() {
                    emitter.onComplete();
                }
            })
        );
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        if (scheduler == null) throw new NullPointerException("NULL scheduler detected");
        return create(emitter ->
            scheduler.execute(() -> {
                try {
                    source.subscribe(emitter);
                } catch (Throwable t) {
                    emitter.onError(t);
                }
            })
        );
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        if (scheduler == null) throw new NullPointerException("NULL scheduler detected");
        return Observable.create(emitter ->
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    scheduler.execute(() -> emitter.onNext(item));
                }

                @Override
                public void onError(Throwable t) {
                    scheduler.execute(() -> emitter.onError(t));
                }

                @Override
                public void onComplete() {
                    scheduler.execute(emitter::onComplete);
                }
            })
        );
    }
}
