package io.github.fascesaedi.kanketsu.spi;

import io.github.fascesaedi.kanketsu.core.command.CommandContext;
import io.github.fascesaedi.kanketsu.core.exception.CommandException;

public interface TypeConverter {
    boolean supports(Class<?> targetType);
    Object convert(String source, CommandContext context) throws CommandException;
}
