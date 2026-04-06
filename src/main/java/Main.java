import rx.interfaces.Disposable;
import rx.Observable;
import rx.interfaces.Observer;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===DEMO START===\n");

        basicObservable();
        mapAndFilter();
        flatMap();
        errorHandling();
        disposable();
        subscribeOnIO();
        observeOnSingle();
        subscribeOnAndObserveOn();

        System.out.println("===DEMO END===\n");
    }

    static void basicObservable() {
        System.out.println("Basic Observable scenario start");

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("a");
            emitter.onNext("b");
            emitter.onComplete();
        });

        observable.subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                System.out.println("onNext: " + item);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("onError: " + t.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }
        });
        System.out.println("Basic Observable scenario end\n");
    }


    static void mapAndFilter() {
        System.out.println("Map + Filter scenario start");

        Observable.<Integer>create(emitter -> {
                    for (int i = 1; i <= 6; i++) emitter.onNext(i);
                    emitter.onComplete();
                })
                .filter(item -> item % 2 == 0)
                .map(item -> "MappedItem=" + (item * 10))
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        System.out.println("onNext: " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("onError: " + t);
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });

        System.out.println("Map + Filter scenario end\n");
    }

    static void flatMap() {
        System.out.println("FlatMap scenario start");

        Observable.<String>create(emitter -> {
                    emitter.onNext("a");
                    emitter.onNext("b");
                    emitter.onComplete();
                })
                .flatMap(item -> Observable.<String>create(innerEmitter -> {
                    innerEmitter.onNext(item + "1");
                    innerEmitter.onNext(item + "2");
                    innerEmitter.onComplete();
                }))
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        System.out.println("onNext: " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("onError: " + t);
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });

        System.out.println("FlatMap scenario end\n");
    }

    static void errorHandling() {
        System.out.println("Error handling scenario start");

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(10);
                    emitter.onNext(0);
                    emitter.onNext(5);
                })
                .map(n -> 100 / n)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("onNext: " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("onError: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });

        System.out.println("Error handling scenario end");
    }

    static void disposable() {
        System.out.println("Disposable scenario start");

        Disposable[] ref = new Disposable[1];

        ref[0] = Observable.<Integer>create(emitter -> {
                    for (int i = 1; i <= 5; i++) {
                        if (emitter.isDisposed()) break;
                        emitter.onNext(i);
                    }
                    emitter.onComplete();
                })
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("onNext: " + item);
                        if (item == 3) {
                            ref[0].dispose();
                            System.out.println("Disposed");
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("onError: " + t);
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });

        System.out.println("Disposable scenario end\n");
    }

    static void subscribeOnIO() throws InterruptedException {
        System.out.println("SubscribeOn (io) scenario start");

        CountDownLatch latch = new CountDownLatch(1);

        Observable.<String>create(emitter -> {
                    System.out.println("Source thread: " + Thread.currentThread().getName());
                    emitter.onNext("data");
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        System.out.println("onNext [" + Thread.currentThread().getName() + "]: " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println(t);
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                        latch.countDown();
                    }
                });

        latch.await(2, TimeUnit.SECONDS);
        System.out.println("SubscribeOn (io) scenario end\n");
    }

    static void observeOnSingle() throws InterruptedException {
        System.out.println("ObserveOn (single) scenario start");

        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
                    emitter.onNext(1);
                    emitter.onNext(2);
                    emitter.onComplete();
                })
                .observeOn(Schedulers.single())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("onNext [" + Thread.currentThread().getName() + "]: " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println(t);
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete [" + Thread.currentThread().getName() + "]");
                        latch.countDown();
                    }
                });

        latch.await(2, TimeUnit.SECONDS);

        System.out.println("ObserveOn (single) scenario end\n");
    }

    static void subscribeOnAndObserveOn() throws InterruptedException {
        System.out.println("SubscribeOn (io) + ObserveOn (computation) scenario start");

        CountDownLatch latch = new CountDownLatch(1);

        Observable.<String>create(emitter -> {
                    System.out.println("Producing in: " + Thread.currentThread().getName());
                    emitter.onNext("result");
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .map(s -> s.toUpperCase())
                .observeOn(Schedulers.computation())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String item) {
                        System.out.println("Consuming [" + Thread.currentThread().getName() + "]: " + item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println(t);
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete [" + Thread.currentThread().getName() + "]");
                        latch.countDown();
                    }
                });

        latch.await(2, TimeUnit.SECONDS);

        System.out.println("SubscribeOn (io) + ObserveOn (computation) scenario end\n");
    }
}
