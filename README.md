# Kanketsu

> **Everything Unknown, Now Defined**

[中文 README](README_zh.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kanketsuteam/kanketsu-core)](https://search.maven.org/artifact/io.github.kanketsuteam/kanketsu-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GraalVM](https://img.shields.io/badge/GraalVM-Ready-brightgreen)](https://www.graalvm.org/)

**Kanketsu** is a lightweight, reflection-free Java framework for building command-line applications.

Designed around a **stable core** and an **extensible ecosystem**, Kanketsu focuses on command routing and argument parsing while remaining GraalVM Native Image friendly.

---

## ✨ Key Points

- 🚀 Lightweight CLI framework for Java
- 🔒 Reflection-free by design
- ⚡ GraalVM Native Image friendly
- 📦 Modular ecosystem architecture
- 🧩 Type-safe command and option handling
- 🌳 Supports nested commands and subcommands

---

## ✨ Why Kanketsu?

Kanketsu follows three simple design principles.

* **✨ Explicit over Magic** — Commands are declared in code, never discovered at runtime.
* **⚙️ Static over Dynamic** — Reflection and runtime scanning are avoided whenever practical.
* **📦 Modularity over Monolith** — Keep the Core focused and extend functionality through optional modules.

These principles make Kanketsu predictable, lightweight, and naturally suited for ahead-of-time compilation.

---

## 📦 Modules

| Module                               | Description                                                                          |  Java   |
|:-------------------------------------|:-------------------------------------------------------------------------------------|:-------:|
| **kanketsu-core**                    | Command routing and argument parsing.                                                | **17+** |
| **kanketsu-repl**                    | Interactive shell with history and tab completion powered by JLine.                  | **25+** |
| **kanketsu-json**                    | JSON input support with automatic string/file detection and precise error reporting. | **17+** |
| **kanketsu-completion-maven-plugin** | Maven plugin that auto‑generates Bash/Zsh/Fish completions for your CLI.             | **17+** |

The Core has **zero transitive dependencies** and remains intentionally minimal.

---

## 📥 Installation

**Core module:**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

**JSON module (optional):**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-json</artifactId>
    <version>2.1.0</version>
</dependency>
```

**REPL module (optional):**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-repl</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## 🚀 Hello World

```java
CLI.builder()
    .command("hello", command -> command
        .action(ctx -> System.out.println("Hello, Kanketsu!")))
    .build()
    .execute("hello");
```

---

## 🌳 Nested Commands

```java
CLI cli = CLI.builder()
    .command("git", git -> git
        .command("commit", commit -> commit
            .option("message", option -> option
                .shortOpt("m")
                .hasArg(true)
                .converter(Converters.STRING))
            .action(ctx -> {
                String message = ctx.getOptionValue("message", String.class);
                System.out.println(message);
            })))
    .build();

cli.execute("git", "commit", "-m", "Initial commit");
```

---

## 📄 JSON Input Example

```java
CLI.builder()
    .converter(JsonTypeConverter.INSTANCE)
    .command("parse", cmd -> cmd
        .option("config", opt -> opt
            .hasArg(true)
            .converter(Converters.STRING)
            .required(true))
        .action(ctx -> {
            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
            System.out.println("Host: " + config.get("host"));
        }))
    .build()
    .execute("parse", "--config", "{\"host\":\"localhost\"}");
```

---

## ⚖️ Comparison

| Feature                      |       Kanketsu        |           Picocli           |         Spring Shell        |
| :--------------------------- |:---------------------:| :-------------------------: | :-------------------------: |
| Core Size                    |       **34 KB**       |           ~100 KB           |            >1 MB            |
| Reflection                   |          ❌           |              ✅              |              ✅              |
| Runtime Scanning             |          ❌           |              ⚠️             |              ✅              |
| Zero Transitive Dependencies |          ✅           |              ✅              |              ❌              |
| GraalVM Native Image         | ✅ Zero Configuration | ⚠️ Additional Configuration | ⚠️ Additional Configuration |

Kanketsu does not aim to replace full-featured frameworks.
It focuses on predictable, lightweight CLI infrastructure.

Kanketsu is designed for developers who value:

* 🚀 Lightweight libraries
* 🏗 Explicit APIs
* ⚡ AOT-friendly architecture
* 📦 Modular design

---

## 📚 Documentation

| Guide                                            | Description                                                                                                                              |
|:-------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------|
| [🚀 Getting Started](docs/en/getting-started.md) | Build your first Kanketsu application.                                                                                                   |
| [🧠 Concepts](docs/en/concepts.md)               | Learn the core concepts behind Kanketsu.                                                                                                 |
| [💡 Examples](docs/en/examples.md)               | Practical examples and common CLI patterns.                                                                                              |
| [📦 Modules](docs/en/modules.md)                 | Learn about the Core and optional ecosystem modules.                                                                                     |
| [🏛 Design](docs/en/design.md)                   | Understand the design philosophy of Kanketsu.                                                                                            |
| [⚡ Performance](docs/en/performance.md)         | Benchmark results and Native Image performance.                                                                                          |
| [📋 Changelog](docs/en/changelog.md)             | Version history and migration notes.                                                                                                     |
| [📦 Native Image](docs/en/native-image.md)       | Build by GraalVM Native Image                                                                                                            |
| [💦 Extension Guide](docs/en/extension-guide.md) | This document describes how to inject custom implementations via the Builder chain, as well as the currently available extension points. |

---

## ❤️ Philosophy

Kanketsu intentionally keeps its Core small.

Features such as Spring integration, YAML support, annotation processing, shell completion, and other ecosystem capabilities belong in optional modules rather than the Core itself.

This approach keeps the framework lightweight while allowing the ecosystem to evolve independently.

---

## 📄 License

Licensed under the Apache License 2.0.