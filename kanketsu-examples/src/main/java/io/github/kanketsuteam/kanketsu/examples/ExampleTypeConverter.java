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
