package com.eleanor.utils.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс, представляющий собой примитив синхронизации ManualResetEvent.
 * Позволяет потокам сигнализировать друг другу о событиях.
 */
public class ManualResetEvent {

    private static final Logger log = LoggerFactory.getLogger(ManualResetEvent.class);

    private final Object monitor = new Object(); // Объект монитора для синхронизации
    private volatile boolean open = false;   // Состояние события (открыто/закрыто)

    /**
     * Конструктор класса ManualResetEvent.
     *
     * @param open Начальное состояние события (true = открыто, false = закрыто).
     */
    public ManualResetEvent(boolean open) {
        this.open = open;
    }

    /**
     * Блокирует текущий поток до тех пор, пока событие не будет установлено (открыто).
     *
     * @throws InterruptedException Если поток был прерван во время ожидания.
     */
    public void waitOne() throws InterruptedException {
        synchronized (monitor) {
            while (!open) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted while waiting on ManualResetEvent", e);
                    Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                    throw e; // Перебрасываем исключение
                }
            }
        }
    }

    /**
     * Сбрасывает событие в закрытое состояние и блокирует текущий поток до тех пор, пока
     * событие не будет установлено (открыто).
     *
     * @throws InterruptedException Если поток был прерван во время ожидания.
     */
    public void resetAndWaitOne() throws InterruptedException {
        synchronized (monitor) {
            open = false;
            while (!open) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted while waiting on ManualResetEvent", e);
                    Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                    throw e; // Перебрасываем исключение
                }
            }
        }
    }

    /**
     * Блокирует текущий поток до тех пор, пока событие не будет установлено (открыто) или
     * не истечет заданное время ожидания.
     *
     * @param milliseconds Время ожидания в миллисекундах.
     * @return true, если событие было установлено до истечения времени ожидания, false в противном случае.
     * @throws InterruptedException Если поток был прерван во время ожидания.
     */
    public boolean waitOne(long milliseconds) throws InterruptedException {
        synchronized (monitor) {
            if (open) {
                return true;
            }

            long startTime = System.currentTimeMillis();
            long remainingTime = milliseconds;

            while (!open && remainingTime > 0) {
                try {
                    monitor.wait(remainingTime);
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted while waiting on ManualResetEvent", e);
                    Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                    throw e; // Перебрасываем исключение
                }
                remainingTime = milliseconds - (System.currentTimeMillis() - startTime);
            }

            return open;
        }
    }

    /**
     * Устанавливает событие в открытое состояние, освобождая все ожидающие потоки.
     */
    public void set() {
        synchronized (monitor) {
            open = true;
            monitor.notifyAll();
        }
    }

    /**
     * Сбрасывает событие в закрытое состояние.
     */
    public void reset() {
        open = false;
    }
}