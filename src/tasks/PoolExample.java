package tasks;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolExample {

    final static Queue<Runnable> taskStorage = new ArrayDeque<>();

    private static void addTaskToQueue(Runnable task) {
        synchronized (taskStorage) {
            taskStorage.add(task);
        }
    }
    private static Runnable getLastTaskFromQueue() {
        synchronized (taskStorage) {
            return taskStorage.poll();
        }
    }
    private static boolean isQueueEmpty() {
        synchronized (taskStorage) {
            return taskStorage.isEmpty();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // создаем пул для выполнения наших задач
        //   максимальное количество созданных задач - 3
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                // не изменяйте эти параметры
                3, 3, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(3));

        // сколько задач выполнилось
        AtomicInteger count = new AtomicInteger(0);

        // сколько задач выполняется
        AtomicInteger inProgress = new AtomicInteger(0);

        int generalQuantityOfTasks = 30;

        Thread taskProducer = new Thread(() -> {
            // отправляем задачи на выполнение
            for (int i = 0; i < generalQuantityOfTasks; i++) {
                final int number = i;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("creating #" + number);
                Runnable newTask = () -> {
                    int working = inProgress.incrementAndGet();
                    System.out.println("start #" + number + ", in progress: " + working);
                    try {
                        // тут какая-то полезная работа
                        Thread.sleep(Math.round(1000 + Math.random() * 2000));
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    working = inProgress.decrementAndGet();
                    System.out.println("end #" + number + ", in progress: " + working + ", done tasks: " + count.incrementAndGet());
                };
                addTaskToQueue(newTask);
            }
        });

        Thread taskConsumer = new Thread(() -> {
            while (count.get() != generalQuantityOfTasks) {
                if (!isQueueEmpty() && executor.getQueue().size() < 3) {
                    Runnable task = getLastTaskFromQueue();
                    executor.submit(task);
                }
            }
        });
        taskConsumer.start();
        taskProducer.start();

        taskProducer.join();
        taskConsumer.join();
        executor.shutdown();
        System.out.println("Done");
    }
}