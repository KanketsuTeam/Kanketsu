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
 * A utility class that provides common {@link Converter} implementations.
 * <p>
 * This class contains static factory methods and predefined converters for
 * basic types like String, Boolean, Integer, and Long.
 * </p>
 */
public final class Converters {

    private Converters() {}

    /**
     * A converter that returns the input string unchanged.
     */
    public static final Converter STRING = value -> value;

    /**
     * A converter that parses a string into a Boolean using {@link Boolean#parseBoolean}.
     */
    public static final Converter BOOLEAN = Boolean::parseBoolean;

    /**
     * A converter that parses a string into an Integer using {@link Integer#parseInt}.
     */
    public static final Converter INT = Integer::parseInt;

    /**
     * A converter that parses a string into a Long using {@link Long#parseLong}.
     */
    public static final Converter LONG = Long::parseLong;

    /**
     * Creates a converter from a {@link java.util.function.Function}.
     * <p>
     * Example:
     * <pre>{@code
     * Converter hex = Converters.of(Integer::parseInt);
     * }</pre>
     *
     * @param fn the function that converts a String to the desired type
     * @return a converter that delegates to the given function
     */
    public static Converter of(java.util.function.Function<String, ?> fn) {
        return fn::apply;
    }
}