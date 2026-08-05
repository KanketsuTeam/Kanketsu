# Changelog

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

### ✨ Added

- **Mixed options support** — Options can now appear before subcommands: `git --verbose commit`
- **Strict subcommand matching** — Unknown subcommands now throw `UnknownSubcommandException`
- **Auto-help for commands without action** — Commands with no action now display help automatically
- **System property `kanketsu.autoHelp`** — Control automatic help interception
- **Type-safe `getOptionValue()`** — Generic method with compile-time type checking
- **`Converters` utility class** — Predefined converters: `STRING`, `BOOLEAN`, `INT`, `LONG`, and custom `of()`
- **Refined exception hierarchy** — 7 specialized exceptions for different error scenarios
- **`shortToLongMap` cache** — Performance optimization for option parsing

### 🐛 Fixed

- Option value conversion exceptions now throw `OptionValueInvalidException`
- `HelpGenerator` properly handles null commands
- `CommandBuilder` uses `CommandBuildException` for duplicate subcommands

## [1.0.1] - 2026-07-XX

- Initial release