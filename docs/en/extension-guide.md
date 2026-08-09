# Extension Guide

Kanketsu's core module provides multiple SPI extension points, allowing you to customize logging, help message generation, type conversion, and other behaviors.

This document describes how to inject custom implementations via the Builder chain, as well as the currently available extension points.

## Format

We uniformly use explicit method injection in the Builder chain to add extensions. For example:
```java
CLI cli = CLI.builder()
        .logger(yourLogger)
        .helpGenerator(yourHelpGenerator)
        .converter(yourConverter)
    .build();
```

---

## Extending Logging

First, create a class that implements the `Logger` interface:
```java
package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.spi.Logger;

public class ExampleLogger implements Logger {
    @Override
    public void log(String message) {
        
    }
}
```

> You can implement your desired output method in this `log` method, such as writing to a file, using `System.err`, etc.

Typically, the methods you can override include:
```java
package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.spi.Logger;

public class ExampleLogger implements Logger {
    // Must implement
    @Override
    public void log(String message) {}

    // Optionally override
    @Override
    public void debug(String message) {}

    @Override
    public void info(String message) {}

    @Override
    public void warn(String message) {}

    @Override
    public void error(String message) {}

    @Override
    public void error(String message, Throwable t) {}

    @Override
    public void error(String message, Throwable t, int index) {}

    @Override
    public void error(String message, Throwable t, int index, String[] rawValues) {}
    
    // Non-standard level, CLI-specific method
    @Override
    public void success(String message) {}
}
```
> `TerminalLogger` comes from the `kanketsu-repl` module; it already handles terminal encoding issues, and the `log()` method directly outputs messages to the terminal. If your Logger only needs to output to the terminal, extending `TerminalLogger` saves you the trouble of handling encoding.
>
> **Note**: Using `TerminalLogger` requires adding the `kanketsu-repl` dependency.

A complete Logger example, from Gitetsu:
```java
package io.github.kanketsuteam.gitetsu;

import io.github.kanketsuteam.kanketsu.repl.TerminalLogger;

// GitetsuLogger extends TerminalLogger, reusing terminal output capability directly
// Uses ANSI escape codes to color different log levels
public class GitettsuLogger extends TerminalLogger {
    
    // Handling of error messages
    @Override
    public void error(String message) {log("[\u001B[31mERROR\u001B[0m] " + message);}
    // Optimized output using the three-parameter error method
    @Override
    public void error(String message, Throwable t, int index){
        if(index == -1){
            error(message);
        }
        else {
            log("[\u001B[31mERROR AT " + index + "\u001B[0m] " + message);
        }
    }
    // Optimized output using the four-parameter error method. index - specific index of the erroneous token. rawValues - all tokens entered by the user.
    @Override
    public void error(String message, Throwable t, int index, String[] rawValues) {
        if(index == -1){
            error(message);
        }
        else {
            log("[\u001B[31mERROR AT " + index + "\u001B[0m] " + message +
                    " (RawValue: \u001B[31m'" + rawValues[index] + "'\u001B[0m)");
        }
    }
    // Coloring for other methods...
    @Override
    public void info(String message) {log("[\u001B[94mINFO \u001B[0m] " + message);}

    @Override
    public void success(String message) {log("[\u001B[32mSUCCESS\u001B[0m] " + message);}

    @Override
    public void warn(String message) {log("[\u001B[33mWARN \u001B[0m] " + message);}
}

```
After implementation, in the Builder chain:
```java
Logger logger = new ExampleLogger();
CLI cli = CLI.builder()
        .logger(logger)
        .build();
```
Now you can use your Logger.

---

## Extending Help Generation

First, create a class that implements the `HelpGenerator` interface:
```java
package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.spi.HelpGenerator;

public class ExampleHelpGenerator implements HelpGenerator {
    @Override
    public void output(String helpText) {

    }
}
```
> You can implement your desired output method in this `output` method, such as using Terminal to bypass encoding, using `System.err`, etc.

A common usage is to adjust the width and height of the built-in help generator:
```java
package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.spi.HelpGenerator;

public class ExampleHelpGenerator implements HelpGenerator {
    @Override
    public void output(String helpText) {

    }

    @Override
    public int getCommandWidth() {
        return 30;  // Reserve more space for long command names
    }

    @Override
    public int getOptionWidth() {
        return 50;
    }
}
```
For deeper customization, you can override the `generateDetailedHelp` method.

After implementation, inject it in the Builder chain:
```java
HelpGenerator helpGenerator = new ExampleHelpGenerator();
CLI cli = CLI.builder()
        .helpGenerator(helpGenerator)
        .build();
```

Now you can use your HelpGenerator.

---

## Extending Type Converters

First, create a class that implements the `TypeConverter` interface:
```java
package io.github.kanketsuteam.kanketsu.examples;

import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.CommandException;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

public class ExampleTypeConverter implements TypeConverter {
    @Override
    public boolean supports(Class<?> targetType) {
        return false;
    }

    @Override
    public Object convert(String source, CommandContext context) throws CommandException {
        return null;
    }
}
```
> In `supports`, determine whether `targetType` is your target type.
>
> In `convert`, implement your conversion logic. `source` is the raw data, and `context` is the context (generally not needed).

Here is the type converter implementation from kanketsu-json:
```java
package io.github.kanketsuteam.kanketsu.json;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.OptionValueInvalidException;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonTypeConverter implements TypeConverter {

    public static final JsonTypeConverter INSTANCE = new JsonTypeConverter();

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == JsonObject.class || targetType == JsonArray.class;
    }

    @Override
    public Object convert(String source, CommandContext context) {
        if (source == null || source.isBlank()) {
            throw new OptionValueInvalidException("JSON source cannot be null or empty");
        }

        String trimmed = source.trim();

        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        char firstChar = trimmed.charAt(0);

        String content;
        if (firstChar == '{' || firstChar == '[') {
            content = trimmed;
        } else {
            try {
                Path path = Path.of(trimmed);
                if (!Files.exists(path)) {
                    throw new OptionValueInvalidException("JSON file not found: " + trimmed);
                }
                content = Files.readString(path);
            } catch (IOException e) {
                throw new OptionValueInvalidException("Failed to read JSON file: " + trimmed, e);
            }
        }

        try {
            Object parsed = Jsoner.deserialize(content);
            if (parsed instanceof JsonObject || parsed instanceof JsonArray) {
                return parsed;
            } else {
                throw new OptionValueInvalidException("Parsed JSON is not an object or array: " + parsed.getClass().getName());
            }
        } catch (JsonException e) {
            String message = "Invalid JSON";
            int pos = e.getPosition();
            if (pos >= 0) {
                message += " at position " + pos;
            }
            message += ": " + e.getMessage();
            throw new OptionValueInvalidException(message, e);
        } catch (Exception e) {
            throw new OptionValueInvalidException("Failed to parse JSON", e);
        }
    }
}
```
After implementation, inject it in the Builder chain:
```java
TypeConverter typeConverter = new ExampleTypeConverter();
CLI cli = CLI.builder()
        .typeConverter(typeConverter)
        .build();
```
Now you can use your TypeConverter.

---

## Using All Three Extensions Together
```java
Logger logger = new ExampleLogger();
HelpGenerator helpGenerator = new ExampleHelpGenerator();
TypeConverter typeConverter = new ExampleTypeConverter();

CLI cli = CLI.builder()
        .logger(logger)
        .helpGenerator(helpGenerator)
        .converter(typeConverter)
        .build();
```