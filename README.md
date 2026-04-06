# Custom RxJava

## 1. Архитектура системы

Библиотека реализует паттерн **Observer** (наблюдатель) с поддержкой реактивных потоков данных. Архитектура построена на следующих принципах:

- **Ленивость**: поток не производит данные до момента подписки.
- **Цепочки операторов**: каждый оператор возвращает новый `Observable`, не изменяя предыдущий (иммутабельность).
- **Терминальное состояние**: после `onError` или `onComplete` поток считается завершённым — дальнейшие события игнорируются.
- **Безопасность отмены**: `Disposable` защищён от race condition через `AtomicBoolean`.

### Структура пакетов

```
src/
├── main/java/
│   ├── rx/
│   │   ├── interfaces/
│   │   │   ├── Observer.java               ← интерфейс подписчика
│   │   │   ├── Disposable.java             ← интерфейс отмены подписки
│   │   │   ├── Scheduler.java              ← интерфейс планировщика
│   │   │   ├── ObservableEmitter.java      ← интерфейс эмиттера (extends Disposable)
│   │   │   └── ObservableOnSubscribe.java  ← функциональный интерфейс источника
│   │   │
│   │   ├── schedulers/
│   │   │   ├── IOThreadScheduler.java      ← CachedThreadPool (I/O задачи)
│   │   │   ├── ComputationScheduler.java   ← FixedThreadPool (CPU задачи)
│   │   │   ├── SingleThreadScheduler.java  ← SingleThreadExecutor (последовательность)
│   │   │   └── Schedulers.java             ← фабрика планировщиков
│   │   │
│   │   ├── Emitter.java                    ← реализация ObservableEmitter и Disposable
│   │   └── Observable.java                 ← главный класс (операторы + подписка)
│   │
│   └── Main.java                           ← демонстрационные сценарии
│
└── test/java/
    └── rx/
        ├── ObservableBasicTest.java        ← жизненный цикл потока, обработка ошибок
        ├── MapOperatorTest.java            ← оператор map
        ├── FilterOperatorTest.java         ← оператор filter
        ├── FlatMapOperatorTest.java        ← оператор flatMap
        ├── DisposableTest.java             ← управление подпиской
        └── SchedulerTest.java              ← планировщики, subscribeOn, observeOn
```

### Взаимодействие компонентов

Пользователь создаёт `Observable` через фабричный метод `create`, передавая в него лямбду `ObservableOnSubscribe`. Эта лямбда описывает логику источника данных и будет вызвана позже — в момент подписки.

После создания `Observable` к нему можно применять операторы (`map`, `filter`, `flatMap`). Каждый оператор возвращает новый `Observable`, не изменяя предыдущий. Таким образом строится цепочка преобразований.

Методы `subscribeOn` и `observeOn` также возвращают новый `Observable`, но с изменённым планировщиком: `subscribeOn` определяет, в каком потоке запустится источник данных, а `observeOn` — в каком потоке `Observer` будет получать уведомления.

Когда вызывается `subscribe(Observer)`, запускается цепочка: `Observable` создаёт экземпляр `Emitter`, передаёт его в источник, и тот начинает испускать элементы вниз по цепочке до конечного `Observer`. `Emitter` реализует оба интерфейса — `ObservableEmitter` и `Disposable`, — что позволяет в любой момент отменить подписку вызовом `dispose()`. После отмены все последующие события игнорируются.

---

## 2. Описание компонентов

### `Observer<T>`

Интерфейс с тремя методами жизненного цикла:

| Метод | Назначение |
|-------|-----------|
| `onNext(T item)` | Получает очередной элемент потока |
| `onError(Throwable t)` | Получает уведомление об ошибке (терминальное) |
| `onComplete()` | Получает уведомление о завершении потока (терминальное) |

### `Observable<T>`

Центральный класс. Хранит ссылку на `ObservableOnSubscribe<T>` — источник данных. Методы:

- `create(ObservableOnSubscribe<T>)` — фабричный метод создания потока
- `subscribe(Observer<T>)` — запускает поток и возвращает `Disposable`
- `map(Function<T,R>)` — преобразование элементов
- `filter(Predicate<T>)` — фильтрация элементов
- `flatMap(Function<T, Observable<R>>)` — разворачивание вложенных потоков
- `subscribeOn(Scheduler)` — задаёт поток источника
- `observeOn(Scheduler)` — задаёт поток доставки уведомлений

### `Emitter<T>`

Реализует одновременно `ObservableEmitter<T>` и `Disposable`. Создаётся внутри `Observable.subscribe()` и передаётся в источник данных. Ключевые детали:

- Использует `AtomicBoolean isTerminated` для thread-safe управления состоянием.
- `onError` и `onComplete` используют `compareAndSet(false, true)` — гарантируют единственный вызов терминального события.
- После `dispose()` все последующие `onNext`, `onError`, `onComplete` игнорируются.
- Так как `ObservableEmitter` наследует `Disposable`, метод `dispose()` доступен прямо внутри `create`-лямбды через параметр `emitter`.

---

## 3. Операторы преобразования

### `map`

Применяет функцию к каждому элементу потока. Создаёт новый `Observable`, который подписывается на источник и передаёт transformed-элементы вниз по цепочке.

```java
Observable.create(emitter -> {
    emitter.onNext(2);
    emitter.onNext(3);
    emitter.onComplete();
})
.map(n -> n * n) // 4, 9
.subscribe(...);
```

**Обработка ошибок**: если функция-маппер бросает исключение, оно перехватывается и передаётся в `onError` — поток не крашится.

### `filter`

Пропускает только элементы, удовлетворяющие предикату. Элементы, не прошедшие проверку, просто отбрасываются.

```java
Observable.create(emitter -> {
    for (int i = 1; i <= 5; i++) emitter.onNext(i);
    emitter.onComplete();
})
.filter(n -> n % 2 == 0) // 2, 4
.subscribe(...);
```

**Обработка ошибок**: исключение в предикате перехватывается и передаётся в `onError`.

### `flatMap`

Преобразует каждый элемент в `Observable` и объединяет (merge) все внутренние потоки в один выходной. Используется для асинхронных операций, зависящих от элементов потока.

```java
Observable.create(emitter -> {
    emitter.onNext("user1");
    emitter.onNext("user2");
    emitter.onComplete();
})
.flatMap(userId -> fetchOrdersFor(userId)) // каждый → Observable<Order>
.subscribe(...);
```

**Важно**: завершение внутреннего `Observable` (`onComplete`) не завершает внешний поток — внешний завершается только после завершения источника верхнего уровня.

---

## 4. Планировщики (Schedulers)

Планировщики управляют тем, в каком потоке выполняется та или иная часть цепочки. Интерфейс `Scheduler` содержит два метода: `execute(Runnable)` и `shutdown()`.

### IOThreadScheduler

**Реализация**: `Executors.newCachedThreadPool()`

**Поведение**: создаёт новые потоки по мере необходимости; неиспользуемые потоки уничтожаются через 60 секунд. Количество потоков не ограничено.

**Когда применять**: операции блокирующего I/O — сетевые запросы, чтение файлов, обращения к базе данных, вызовы внешних API. Поскольку эти операции большую часть времени ожидают ответа (блокируются), большое количество потоков не создаёт конкуренцию за CPU.

```java
observable
    .subscribeOn(Schedulers.io()) // источник читает файл в io-потоке
    .subscribe(observer);
```

### ComputationScheduler

**Реализация**: `Executors.newFixedThreadPool(N)`, где N = `Runtime.getRuntime().availableProcessors()`

**Поведение**: фиксированный пул потоков, размер которого равен числу ядер процессора. Избыточные задачи ставятся в очередь.

**Когда применять**: CPU-интенсивные вычисления без блокировок — математические операции, обработка изображений/видео, парсинг, агрегация данных. Фиксированный размер пула предотвращает избыточное переключение контекста между потоками.

```java
observable
    .observeOn(Schedulers.computation()) // тяжёлые вычисления в computation-потоке
    .map(this::processHeavyData)
    .subscribe(observer);
```

### SingleThreadScheduler

**Реализация**: `Executors.newSingleThreadExecutor()`

**Поведение**: все задачи выполняются последовательно в одном и том же потоке. Гарантирует порядок выполнения.

**Когда применять**: обновление UI-компонентов, последовательная запись в файл, работа с non-thread-safe структурами данных. Исключает необходимость дополнительной синхронизации.

```java
observable
    .observeOn(Schedulers.single()) // все обновления UI — в одном потоке
    .subscribe(view::updateUI);
```

### Сравнительная таблица

| Планировщик | Пул потоков | Размер | Применение |
|-------------|------------|--------|-----------|
| `IOThreadScheduler` | CachedThreadPool | Динамический (без лимита) | Сеть, файлы, БД |
| `ComputationScheduler` | FixedThreadPool | = число CPU | Вычисления, обработка данных |
| `SingleThreadScheduler` | SingleThreadExecutor | 1 | UI, последовательная запись |

### `subscribeOn` vs `observeOn`

| Метод | Что переносит в другой поток | Влияние на цепочку |
|-------|---------------------------|--------------------|
| `subscribeOn(s)` | Источник данных (upstream) | Влияет на весь upstream; первый `subscribeOn` выигрывает |
| `observeOn(s)` | Доставку уведомлений (downstream) | Влияет только на то, что ниже в цепочке; можно переключать несколько раз |

---

## 5. Управление подписками (Disposable)

`Disposable` позволяет отменить подписку, тем самым прекращая доставку элементов Observer'у:

```java
Disposable subscription = observable.subscribe(observer);

// Когда результат больше не нужен:
subscription.dispose();

// Проверка состояния:
boolean isCancelled = subscription.isDisposed();
```

Источник может (и должен) проверять `emitter.isDisposed()` в циклах, чтобы не производить ненужную работу:

```java
Observable.create(emitter -> {
    for (int i = 0; i < 1_000_000; i++) {
        if (emitter.isDisposed()) break; // ранний выход
        emitter.onNext(compute(i));
    }
    emitter.onComplete();
});
```

---

## 6. Обработка ошибок

Ошибки обрабатываются на нескольких уровнях:

1. **Исключение в источнике** (`create` лямбда): перехватывается в `subscribe()`, передаётся в `onError`.
2. **Исключение в операторе** (`map`, `filter`, `flatMap`): перехватывается внутри оператора, передаётся в `onError`.
3. **Явный вызов** `emitter.onError(t)`: передаётся напрямую.

После первого `onError` поток завершается — последующие `onNext` и `onComplete` игнорируются.

```java
Observable.<Integer>create(emitter -> {
    emitter.onNext(10);
    emitter.onNext(0);
})
.map(n -> 100 / n) // при n=0 → ArithmeticException
.subscribe(new Observer<Integer>() {
    @Override public void onNext(Integer item)  { System.out.println(item); }
    @Override public void onError(Throwable t)  {
        System.err.println("Error: " + t.getMessage()); // обрабатываем
    }
    @Override public void onComplete() { }
});
// Вывод:
// 10
// Error: / by zero
```

## 7. Тестирование

### Инструменты

- **JUnit 5** (Jupiter) — фреймворк юнит-тестирования
- **CountDownLatch** — синхронизация асинхронных тестов
- **AtomicReference / AtomicBoolean** — thread-safe сбор результатов в тестах
- **ConcurrentHashMap.newKeySet()** — сбор уникальных имён потоков

### Тестовые классы

Тесты разбиты по отдельным файлам согласно тестируемому компоненту.

#### `ObservableBasicTest` — 8 тестов

Жизненный цикл потока: порядок элементов, пустой поток, однократность `onComplete` и `onError`, игнорирование `onError` после `onComplete`, перехват исключений из источника, NullPointerException на null-аргументах.

#### `MapOperatorTest` — 4 теста

Преобразование всех элементов, цепочка из двух `map`, маршрутизация исключения в маппере в `onError`, NullPointerException на null-маппере.

#### `FilterOperatorTest` — 5 тестов

Фильтрация по предикату, пустой результат с вызовом `onComplete`, маршрутизация исключения в предикате в `onError`, комбинация `filter + map`, NullPointerException на null-предикате.

#### `FlatMapOperatorTest` — 5 тестов

Разворачивание вложенных потоков, пустой внутренний поток, маршрутизация исключений из маппера и из внутреннего Observable в `onError`, NullPointerException на null-маппере.

#### `DisposableTest` — 4 теста

Состояние `isDisposed` после `onComplete` и `onError`, прекращение доставки элементов после `dispose()`, игнорирование `onNext` после `dispose()`.

#### `SchedulerTest` — 12 тестов

Выполнение задач `IOThreadScheduler` в io-потоке и обработка конкурентных задач, выполнение задач `ComputationScheduler` в computation-потоке и соответствие размера пула числу ядер процессора, выполнение всех задач `SingleThreadScheduler` в одном потоке с сохранением порядка, возврат синглтон-инстансов фабрикой `Schedulers`, запуск источника в заданном потоке через `subscribeOn`, доставка событий observer в заданном потоке через `observeOn`, NullPointerException на null-планировщике в обоих методах, совместная работа `subscribeOn` и `observeOn` с разными потоками для источника и observer.

### Запуск тестов

```bash
mvn test
```

Ожидаемый результат: все 38 тестов проходят успешно.
