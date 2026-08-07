/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.kanketsuteam.kanketsu.spi;

/**
 * A simple logging interface used by the framework to output messages.
 * <p>
 * Implementations can direct logs to any destination (console, file, etc.)
 * and can add custom formatting or filtering.
 * </p>
 * <p>
 * Default implementations are provided via {@link #system()} (logs to {@code System.out})
 * and {@link #noop()} (discards all messages).
 * </p>
 *
 * @see #system()
 * @see #noop()
 */
public interface Logger {

    /**
     * Logs a raw message. This is the only abstract method; all other logging
     * methods delegate to this one.
     *
     * @param message the message to log (never {@code null})
     */
    void log(String message);

    /**
     * Logs a debug message. The output may be suppressed if debug is not enabled.
     *
     * @param message the debug message
     */
    default void debug(String message) {
        log("[DEBUG] " + message);
    }

    /**
     * Logs an informational message.
     *
     * @param message the info message
     */
    default void info(String message) {
        log("[INFO] " + message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the warning message
     */
    default void warn(String message) {
        log("[WARN] " + message);
    }

    /**
     * Logs an error message.
     *
     * @param message the error message
     */
    default void error(String message) {
        log("[ERROR] " + message);
    }

    /**
     * Logs an error message with an associated throwable.
     * <p>
     * The default implementation only logs the message, discarding the throwable.
     * Implementations may override to print stack traces.
     * </p>
     *
     * @param message the error message
     * @param t       the throwable causing the error
     */
    default void error(String message, Throwable t) {
        error(message);
    }

    /**
     * Logs an error message with a throwable and the argument index where the error occurred.
     *
     * @param message the error message
     * @param t       the throwable
     * @param index   the index of the problematic argument
     */
    default void error(String message, Throwable t, int index) {
        error(message, t);
    }

    /**
     * Logs an error message with a throwable, index, and raw argument values.
     *
     * @param message   the error message
     * @param t         the throwable
     * @param index     the index of the problematic argument
     * @param rawValues the full array of raw arguments (may be {@code null})
     */
    default void error(String message, Throwable t, int index, String[] rawValues) {
        error(message, t, index);
    }

    /**
     * Logs a success message.
     *
     * @param message the success message
     */
    default void success(String message) {
        log("[SUCCESS] " + message);
    }

    /**
     * Returns whether debug logging is enabled.
     * <p>
     * The default implementation checks the system property {@code kanketsu.debug}.
     * Implementations may override to use their own configuration.
     * </p>
     *
     * @return {@code true} if debug messages should be logged
     */
    default boolean isDebugEnabled() {
        return Boolean.getBoolean("kanketsu.debug");
    }

    /**
     * Returns a logger that outputs to {@code System.out}.
     *
     * @return a system-out logger
     */
    static Logger system() {
        return System.out::println;
    }

    /**
     * Returns a logger that discards all messages (no-op).
     *
     * @return a no-op logger
     */
    static Logger noop() {
        return message -> {};
    }
}