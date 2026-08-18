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
 * Thrown when the user enters a command that is not registered.
 */
public class UnknownCommandException extends CommandException {
    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public UnknownCommandException(String message) {
        super(1, message);
    }

    /**
     * Constructs a new exception with the specified message and position.
     *
     * @param message  the detail message
     * @param position the position in the argument array where the error occurred
     */
    public UnknownCommandException(String message, int position) {
        super(3, message, position);
    }
}