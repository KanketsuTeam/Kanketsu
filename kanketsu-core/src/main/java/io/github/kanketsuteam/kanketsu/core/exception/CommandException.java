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
package io.github.kanketsuteam.kanketsu.core.exception;

/**
 * The base exception for all command-related errors in the Kanketsu framework.
 * <p>
 * It carries an error code and an optional position in the argument list.
 * </p>
 */
public class CommandException extends RuntimeException {
    /**
     * Constant indicating that the position is unknown or not applicable.
     */
    public static final int POSITION_UNKNOWN = -1;

    private final int code;
    private final int position;

    /**
     * Constructs a new exception with the given code and message.
     *
     * @param code    the error code
     * @param message the detail message
     */
    public CommandException(int code, String message) {
        this(code, message, -1);
    }

    /**
     * Constructs a new exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public CommandException(String message, Throwable cause) {
        this(1, message, -1, cause);
    }

    /**
     * Constructs a new exception with the given code, message, and cause.
     *
     * @param code    the error code
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public CommandException(int code, String message, Throwable cause) {
        this(code, message, -1, cause);
    }

    /**
     * Constructs a new exception with the given code, message, and position.
     *
     * @param code     the error code
     * @param message  the detail message
     * @param position the index in the argument array where the error occurred
     */
    public CommandException(int code, String message, int position) {
        super(message);
        this.code = code;
        this.position = position;
    }

    /**
     * Constructs a new exception with the given code, message, position, and cause.
     *
     * @param code     the error code
     * @param message  the detail message
     * @param position the index in the argument array where the error occurred
     * @param cause    the underlying cause
     */
    public CommandException(int code, String message, int position, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.position = position;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public int getCode() { return code; }

    /**
     * Returns the position in the argument array, or {@link #POSITION_UNKNOWN} if not set.
     *
     * @return the position
     */
    public int getPosition() { return position; }
}