# Changelog

## [2.0.3] - 2026-8-7

### ✨ Added

- Make the error method of the Logger interface able to get more information, such as: the index of the erroneous item, and the complete raw input.
- JavaDoc documentation of Kanketsu-core
- Optimized the core part of Kanketsu-core
- Added about 20 test cases

### 🐛 Fixed

- Downgrade the Java version to 17. Note: The REPL module is still Java 25

## [2.0.2] - 2026-8-6

### ✨ Added

- Added some unit tests

### 🐛 Fixed

- Fixed some parsing issues

## [2.0.1] - 2026-08-05

### ✨ Added

- **Shell Completion Maven Plugin** — Automatically generate Bash/Zsh/Fish completion scripts from your command tree.
  - Zero manual maintenance; completions stay in sync with your code.
  - Context‑sensitive completions for commands, subcommands, and options.
  - Fully compatible with GraalVM Native Image (no runtime reflection).
- **Documentation** — Added dedicated shell completion guide and updated Native Image documentation.

### 🐛 Fixed

- Various minor fixes in help generation and error messages.

## [2.0.0] - 2026-08-05

### ⚠️ Breaking Changes

- **Removed `Category` enum** — Use `Converters` instead
  ```diff
  - .category(Category.INT)
  + .converter(Converters.INT)
  ```
- **`CommandContext.getOption()` now returns pre-converted `Object`** — Use `getOptionValue(String, Class<T>)` for type-safe access
  ```diff
  - String msg = (String) ctx.getOption("message");
  + String msg = ctx.getOptionValue("message", String.class);
  ```
- **`CommandContext` constructor now requires a `List<TypeConverter>`** — All commands must provide registered converters when constructing command contexts.

### ✨ Added

- **Mixed options support** — Options can now appear before subcommands: `git --verbose commit`
- **Strict subcommand matching** — Unknown subcommands now throw `UnknownSubcommandException`
- **Auto-help for commands without action** — Commands with no action now display help automatically
- **System property `kanketsu.autoHelp`** — Control automatic help interception
- **Type-safe `getOptionValue()`** — Generic method with compile-time type checking
- **`Converters` utility class** — Predefined converters: `STRING`, `BOOLEAN`, `INT`, `LONG`, and custom `of()`
- **`TypeConverter` SPI** — Core extension point for custom type conversions
- **`getOptionValueAs(String, Class<T>)`** — Convert option values to arbitrary types using registered `TypeConverter` instances
- **`kanketsu-json` module** — JSON support with automatic detection of JSON strings vs. file paths, precise error positioning, and full GraalVM Native Image compatibility
- **`JsonTypeConverter`** — Zero-reflection converter for `JsonObject` and `JsonArray` types
- **Refined exception hierarchy** — 7 specialized exceptions for different error scenarios
- **`shortToLongMap` cache** — Performance optimization for option parsing
- **`JsonExample`** — Comprehensive example demonstrating JSON module usage

### 🐛 Fixed

- Option value conversion exceptions now throw `OptionValueInvalidException` with proper exit code 2
- `HelpGenerator` properly handles null commands
- `CommandBuilder` uses `CommandBuildException` for duplicate subcommands
- `OptionValueInvalidException` now accepts a `Throwable` cause parameter

## [1.0.1] - 2026-07-XX

- Initial release