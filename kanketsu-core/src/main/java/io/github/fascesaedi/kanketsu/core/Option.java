package io.github.fascesaedi.kanketsu.core;

public class Option {
    private final String longOpt;
    private final String shortOpt;
    private final String description;
    private final boolean hasArg;
    private final boolean required;
    private final String defaultValue;

    private Option(Builder builder) {
        this.longOpt = builder.longOpt;
        this.shortOpt = builder.shortOpt;
        this.description = builder.description;
        this.hasArg = builder.hasArg;
        this.required = builder.required;
        this.defaultValue = builder.defaultValue;
    }

    public String getLongOpt() { return longOpt; }
    public String getShortOpt() { return shortOpt; }
    public String getDescription() { return description; }
    public boolean hasArg() { return hasArg; }
    public boolean isRequired() { return required; }
    public String getDefaultValue() { return defaultValue; }

    public static class Builder {
        private String longOpt;
        private String shortOpt;
        private String description = "";
        private boolean hasArg = false;
        private boolean required = false;
        private String defaultValue = null;

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

        public Option build() {
            return new Option(this);
        }
    }
}
