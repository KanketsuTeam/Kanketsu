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

import java.util.Objects;

public class Option {
    private final Converter converter;
    private final String longOpt;
    private final String shortOpt;
    private final String description;
    private final boolean hasArg;
    private final boolean required;
    private final String defaultValue;
    private final boolean global;

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

    public String getLongOpt() { return longOpt; }
    public String getShortOpt() { return shortOpt; }
    public String getDescription() { return description; }
    public boolean hasArg() { return hasArg; }
    public boolean isRequired() { return required; }
    public String getDefaultValue() { return defaultValue; }
    public Converter getConverter() { return converter; }
    public boolean isGlobal() { return global; }

    public Object convert(String value) {
        return converter.convert(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option option = (Option) o;
        return Objects.equals(longOpt, option.longOpt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longOpt);
    }

    public static Builder builder(String longOpt) {
        return new Builder(longOpt);
    }

    public static class Builder {
        private Converter converter;
        private String longOpt;
        private String shortOpt;
        private String description = "";
        private boolean hasArg = false;
        private boolean required = false;
        private String defaultValue = null;
        private boolean global = false;

        public Builder(String longOpt) {
            this.longOpt = longOpt;
        }

        public Builder shortOpt(String shortOpt) {
            this.shortOpt = shortOpt;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder hasArg(boolean hasArg) {
            this.hasArg = hasArg;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder converter(Converter converter) {
            this.converter = converter;
            return this;
        }

        public Builder global(boolean global) {
            this.global = global;
            return this;
        }

        public Option build() {
            if (longOpt == null || longOpt.isBlank()) {
                throw new IllegalArgumentException("longOpt cannot be null or empty");
            }
            return new Option(this);
        }
    }
}