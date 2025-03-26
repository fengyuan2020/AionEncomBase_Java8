package com.eleanor.utils.threading;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс для управления отменой операций в многопоточной среде.
 * Позволяет отменить операцию и выполнить действия при отмене.
 */
public class CancellationToken {

    private static final Logger log = LoggerFactory.getLogger(CancellationToken.class);

    private final AtomicBoolean isCancelled = new AtomicBoolean(false); // Флаг, указывающий, отменена ли операция
    private final BlockingQueue<Runnable> cancelActions = new LinkedBlockingQueue<>(); // Очередь действий для выполнения при отмене

    /**
     * Конструктор класса CancellationToken.
     */
    public CancellationToken() {
    }

    /**
     * Отменяет операцию и выполняет все зарегистрированные действия отмены.
     *
     * @throws InterruptedException Если поток был прерван во время выполнения действий отмены.
     */
    public void cancel() throws InterruptedException {
        // Пытаемся установить флаг отмены, если он еще не установлен.
        if (isCancelled.compareAndSet(false, true)) {
            Runnable action;
            // Извлекаем и выполняем все действия отмены из очереди.
            while ((action = cancelActions.poll()) != null) {
                try {
                    action.run();
                } catch (Exception e) {
                    log.error("Error while executing cancellation action", e);
                }
            }
        }
    }

    /**
     * Добавляет действие для выполнения при отмене операции.
     *
     * @param runnable Действие, которое необходимо выполнить при отмене.
     * @throws InterruptedException Если поток был прерван во время добавления действия в очередь.
     */
    public void addAction(Runnable runnable) throws InterruptedException {
        // Если операция еще не отменена, добавляем действие в очередь.
        if (!isCancelled.get()) {
            cancelActions.put(runnable);
        }
        // Иначе просто игнорируем добавление действия, т.к. отмена уже произошла.
    }

    /**
     * Проверяет, отменена ли операция.
     *
     * @return true, если операция отменена, false в противном случае.
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }
}