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
package io.github.kanketsuteam.kanketsu.core.converter;

/**
 * A functional interface for converting a String value into an object.
 * <p>
 * This is used by {@link io.github.kanketsuteam.kanketsu.core.Option} to convert
 * command-line argument strings into typed values (e.g., Integer, Boolean).
 * </p>
 * <p>
 * Implementations should be stateless and thread-safe.
 * </p>
 *
 * @see Converters
 * @see io.github.kanketsuteam.kanketsu.core.Option
 */
@FunctionalInterface
public interface Converter {

    /**
     * Converts the given String value to an object.
     *
     * @param value the string to convert (never {@code null})
     * @return the converted object
     * @throws IllegalArgumentException if the value cannot be converted
     */
    Object convert(String value);
}