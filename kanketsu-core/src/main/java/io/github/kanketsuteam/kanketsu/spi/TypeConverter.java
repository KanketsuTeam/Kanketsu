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

import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.CommandException;

/**
 * A service provider interface for converting a string value to a target type
 * within the context of a command execution.
 * <p>
 * Unlike the simple {@link io.github.kanketsuteam.kanketsu.core.converter.Converter},
 * this interface provides access to the full {@link CommandContext}, allowing
 * converters to make decisions based on other option values or the overall state.
 * </p>
 * <p>
 * Implementations must be stateless and thread-safe.
 * </p>
 *
 * @see io.github.kanketsuteam.kanketsu.core.command.CommandContext#getOptionValueAs(String, Class)
 */
public interface TypeConverter {

    /**
     * Returns whether this converter can convert to the given target type.
     *
     * @param targetType the target class (never {@code null})
     * @return {@code true} if conversion is supported, {@code false} otherwise
     */
    boolean supports(Class<?> targetType);

    /**
     * Converts the given string value to an object of the supported type.
     * <p>
     * The {@code context} parameter provides access to other parsed options,
     * which can be used for complex conversions (e.g., converting a file path
     * relative to a base directory option).
     * </p>
     *
     * @param source  the string to convert (never {@code null})
     * @param context the command context providing access to other options
     * @return the converted object
     * @throws CommandException if conversion fails, with an appropriate error code
     */
    Object convert(String source, CommandContext context) throws CommandException;
}