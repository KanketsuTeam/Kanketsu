/*
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
package io.github.kanketsuteam.kanketsu.core;

import io.github.kanketsuteam.kanketsu.core.converter.Converter;
import io.github.kanketsuteam.kanketsu.core.converter.Converters;
import io.github.kanketsuteam.kanketsu.core.exception.CommandBuildException;

import java.util.Objects;

/**
 * Represents a command-line option that can be used in a command definition.
 * <p>
 * An option has a long name (required), an optional short name, a description,
 * a flag indicating whether it accepts an argument, a required flag, a default value,
 * a type converter for its argument, and a global flag.
 * </p>
 *
 * @see Builder
 */
public class Option {

    private final Converter converter;
    private final String longOpt;
    private final String shortOpt;
    private final String description;
    private final boolean hasArg;
    private final boolean required;
    private final String defaultValue;
    private final boolean global;

    /**
     * Constructs an {@code Option} instance using the given builder.
     *
     * @param builder the builder containing all option attributes
     */
    private Option(Builder builder) {
        this.longOpt = builder.longOpt;
        this.shortOpt = builder.shortOpt;
        this.description = builder.description;
        this.hasArg = builder.hasArg;
        this.required = builder.required;
        this.defaultValue = builder.defaultValue;
        this.converter = builder.converter != null
                ? builder.converter
                : Converters.STRING;
        this.global = builder.global;
    }

    /**
     * Returns the long name of this option.
     *
     * @return the long option name (never {@code null})
     */
    public String getLongOpt() {
        return longOpt;
    }

    /**
     * Returns the short name of this option.
     *
     * @return the short option name, or {@code null} if none was set
     */
    public String getShortOpt() {
        return shortOpt;
    }

    /**
     * Returns the description of this option.
     *
     * @return the description (never {@code null}, may be empty)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns whether this option accepts an argument.
     *
     * @return {@code true} if this option takes an argument, {@code false} otherwise
     */
    public boolean hasArg() {
        return hasArg;
    }

    /**
     * Returns whether this option is required.
     *
     * @return {@code true} if the option must be present, {@code false} otherwise
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns the default value of this option.
     *
     * @return the default value as a string, or {@code null} if none was set
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the converter used to transform the option's argument from a string
     * to a target type.
     *
     * @return the converter (never {@code null})
     */
    public Converter getConverter() {
        return converter;
    }

    /**
     * Returns whether this option is global.
     * <p>
     * Global options are typically applicable to the entire command hierarchy
     * rather than to a specific sub-command.
     * </p>
     *
     * @return {@code true} if the option is global, {@code false} otherwise
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Converts the given string value using this option's converter.
     *
     * @param value the string value to convert
     * @return the converted object, or {@code null} if the value is {@code null}
     */
    public Object convert(String value) {
        return converter.convert(value);
    }

    /**
     * Compares this option with another object for equality.
     * <p>
     * Two options are considered equal if they have the same long option name.
     * </p>
     *
     * @param o the object to compare with
     * @return {@code true} if equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option option = (Option) o;
        return Objects.equals(longOpt, option.longOpt);
    }

    /**
     * Returns the hash code of this option, based on its long option name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(longOpt);
    }

    /**
     * Creates a new {@code Builder} instance for an option with the given long name.
     *
     * @param longOpt the long name of the option (must not be blank)
     * @return a new builder instance
     */
    public static Builder builder(String longOpt) {
        return new Builder(longOpt);
    }

    /**
     * Builder class for constructing {@code Option} instances.
     * <p>
     * All attributes are optional except the long option name, which must be provided
     * at builder creation and must not be blank. The default converter is
     * {@link Converters#STRING} if not explicitly set.
     * </p>
     */
    public static class Builder {

        private Converter converter;
        private String longOpt;
        private String shortOpt;
        private String description = "";
        private boolean hasArg = false;
        private boolean required = false;
        private String defaultValue = null;
        private boolean global = false;

        /**
         * Constructs a builder with the required long option name.
         *
         * @param longOpt the long name of the option (must not be null or blank)
         */
        public Builder(String longOpt) {
            this.longOpt = longOpt;
        }

        /**
         * Sets the short option name.
         *
         * @param shortOpt the short name (e.g., "v")
         * @return this builder
         */
        public Builder shortOpt(String shortOpt) {
            this.shortOpt = shortOpt;
            return this;
        }

        /**
         * Sets the description of the option.
         *
         * @param description the description text
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets whether this option accepts an argument.
         *
         * @param hasArg {@code true} if the option takes an argument, {@code false} otherwise
         * @return this builder
         */
        public Builder hasArg(boolean hasArg) {
            this.hasArg = hasArg;
            return this;
        }

        /**
         * Sets whether this option is required.
         *
         * @param required {@code true} if the option must be present, {@code false} otherwise
         * @return this builder
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Sets the default value for this option.
         *
         * @param defaultValue the default value as a string
         * @return this builder
         */
        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets the converter used to transform the option's argument.
         * <p>
         * If not set, {@link Converters#STRING} is used.
         * </p>
         *
         * @param converter the converter to use
         * @return this builder
         */
        public Builder converter(Converter converter) {
            this.converter = converter;
            return this;
        }

        /**
         * Sets whether this option is global.
         *
         * @param global {@code true} if the option is global, {@code false} otherwise
         * @return this builder
         */
        public Builder global(boolean global) {
            this.global = global;
            return this;
        }

        /**
         * Builds and returns an {@code Option} instance.
         *
         * @return the constructed {@code Option}
         * @throws CommandBuildException if the long option name is null or blank
         */
        public Option build() {
            if (longOpt == null || longOpt.isBlank()) {
                throw new CommandBuildException("longOpt cannot be null or empty");
            }
            return new Option(this);
        }
    }
}