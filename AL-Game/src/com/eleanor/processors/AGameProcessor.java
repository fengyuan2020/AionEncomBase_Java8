package com.eleanor.processors;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.concurrent.AionRejectedExecutionHandler;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import com.aionemu.gameserver.configs.main.ThreadConfig;

public class AGameProcessor {

    private static final Logger log = LoggerFactory.getLogger(AGameProcessor.class);

    private final ScheduledThreadPoolExecutor processorPool;

    protected AGameProcessor(int threadsCount) {
        this.processorPool = new ScheduledThreadPoolExecutor(threadsCount);
        // Installing the handler for rejected tasks. This is important for handling situations.,
        // when the task queue is full. AionRejectedExecutionHandler must implement
        // a processing strategy suitable for your game (for example, logging, discarding,
        // execution in the current thread, etc.).
        this.processorPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
        // We run all the main threads in the pool. This allows you to avoid delays when
        // when the task is completed for the first time, because the threads will already be ready.
        this.processorPool.prestartAllCoreThreads();
    }

    /**
     * Performs a task in the thread pool.
     *
     * @param r Задача для выполнения.
     */
    public void execute(Runnable r) {
        this.processorPool.execute(r);
    }

    /**
     * Schedules the task to be completed with a specified delay.
     *
     * @param r     Задача для выполнения.
     * @param delay Задержка в миллисекундах.
     * @return ScheduledFuture<?> Объект, представляющий запланированную задачу.
     */
    public ScheduledFuture<?> schedule(Runnable r, long delay) {
        // Wrapping the Runnable in a RunnableTaskWrapper to track the execution time.
        r = new RunnableTaskWrapper(r);
        // We validate the delay to avoid errors and unpredictable behavior.
        long validatedDelay = Math.max(0L, Math.min(Integer.MAX_VALUE, delay));

        if (validatedDelay < delay) {
            log.warn("Attempt to schedule task with delay {}, but maximal is {}. Delay will be trimmed to maximal", delay, validatedDelay);
        }

        // We are planning a task.
        return this.processorPool.schedule(r, validatedDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules the periodic execution of a task with a fixed frequency.
     *
     * @param r      Задача для выполнения.
     * @param delay  Начальная задержка в миллисекундах.
     * @param period Период между выполнениями в миллисекундах.
     * @return ScheduledFuture<?> Объект, представляющий запланированную задачу.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period) {
        // Wrapping the Runnable in a RunnableTaskWrapper to track the execution time.
        r = new RunnableTaskWrapper(r);
        // We validate the delay to avoid errors and unpredictable behavior.
        long validatedDelay = Math.max(0L, Math.min(Integer.MAX_VALUE, delay));

        if (validatedDelay < delay) {
            log.warn("Attempt to schedule task with delay {}, but maximal is {}. Delay will be trimmed to maximal", delay, validatedDelay);
        }

        // Планируем задачу.
        return this.processorPool.scheduleAtFixedRate(r, validatedDelay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules the task to be completed with a specified delay and saves the ScheduledFuture in the Task.
     *
     * @param r     Задача для выполнения.
     * @param delay Задержка в миллисекундах.
     * @param out   Task, в который будет сохранен ScheduledFuture.
     * @return boolean true, если задача успешно запланирована, false - если нет.
     */
    public boolean schedule(Runnable r, long delay, Task out) {
        // Wrapping the Runnable in a RunnableTaskWrapper to track the execution time.
        r = new RunnableTaskWrapper(r);
        // We validate the delay to avoid errors and unpredictable behavior.
        long validatedDelay = Math.max(0L, Math.min(Integer.MAX_VALUE, delay));

        if (validatedDelay < delay) {
            log.warn("Attempt to post scheduled task with delay {}, but maximal is {}. Action will not be triggered", delay, validatedDelay);
            return false;
        }

        // We plan the task and save the ScheduledFuture in the Task.
        out.setTask(this.processorPool.schedule(r, validatedDelay, TimeUnit.MILLISECONDS));
        return true;
    }

    /**
     * A class for storing ScheduledFuture tasks. Allows you to cancel the task later.
     */
    public static class Task {
        private ScheduledFuture<?> task;

        public static Task create() {
            return new Task();
        }

        public ScheduledFuture<?> getTask() {
            return this.task;
        }

        private void setTask(ScheduledFuture<?> task) {
            this.task = task;
        }

        /**
         * Cancels a scheduled task.
         * @param mayInterruptIfRunning true, if the thread executing the task must be terminated, otherwise it is false.
         * @return true, if the task was successfully canceled, false - if the task has already been completed or cannot be canceled.
         */
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (task != null) {
                return task.cancel(mayInterruptIfRunning);
            }
            return false;
        }

        public boolean isDone() {
            return task != null && task.isDone();
        }
    }

    /**
     * A wrapper class for Runnable that tracks the task execution time and logs a warning.,
     * if the task is taking too long to complete.
     */
    private static final class RunnableTaskWrapper extends RunnableWrapper {
        private RunnableTaskWrapper(Runnable runnable) {
            super(runnable, ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING);
        }
    }
}