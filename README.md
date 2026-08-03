# Kanketsu

> **Everything Unknown, Now Defined**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.fascesaedi/kanketsu-core)](https://search.maven.org/artifact/io.github.fascesaedi/kanketsu-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GraalVM](https://img.shields.io/badge/GraalVM-Ready-brightgreen)](https://www.graalvm.org/)

**Kanketsu** is a lightweight, reflection-free Java framework for building command-line applications.

Designed around a **stable core** and an **extensible ecosystem**, Kanketsu focuses on command routing and argument parsing while remaining GraalVM Native Image friendly.

---

## ✨ Why Kanketsu?

Kanketsu follows three simple design principles.

* **✨ Explicit over Magic** — Commands are declared in code, never discovered at runtime.
* **⚙️ Static over Dynamic** — Reflection and runtime scanning are avoided whenever practical.
* **📦 Modularity over Monolith** — Keep the Core focused and extend functionality through optional modules.

These principles make Kanketsu predictable, lightweight, and naturally suited for ahead-of-time compilation.

---

## 📦 Modules

| Module            | Description                                                         |   Java  |
| :---------------- | :------------------------------------------------------------------ | :-----: |
| **kanketsu-core** | Command routing and argument parsing.                               | **21+** |
| **kanketsu-repl** | Interactive shell with history and tab completion powered by JLine. | **24+** |

The Core has **zero transitive dependencies** and remains intentionally minimal.

---

## 📥 Installation

```xml
<dependency>
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-core</artifactId>
    <version>1.0.1</version>
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
                .category(Category.STRING))
            .action(ctx -> {
                String message = (String) ctx.getOption("message");
                System.out.println(message);
            })))
    .build();

cli.execute("git", "commit", "-m", "Initial commit");
```

---

## ⚖️ Comparison

| Feature                      |       Kanketsu       |           Picocli           |         Spring Shell        |
| :--------------------------- | :------------------: | :-------------------------: | :-------------------------: |
| Core Size                    |       **22 KB**      |           ~100 KB           |            >1 MB            |
| Reflection                   |           ❌          |              ✅              |              ✅              |
| Runtime Scanning             |           ❌          |              ⚠️             |              ✅              |
| Zero Transitive Dependencies |           ✅          |              ✅              |              ❌              |
| GraalVM Native Image         | ✅ Zero Configuration | ⚠️ Additional Configuration | ⚠️ Additional Configuration |

Kanketsu is designed for developers who value:

* 🚀 Lightweight libraries
* 🏗 Explicit APIs
* ⚡ AOT-friendly architecture
* 📦 Modular design

---

## 📚 Documentation

| Guide                                         | Description                                          |
| :-------------------------------------------- | :--------------------------------------------------- |
| [🚀 Getting Started](docs/getting-started.md) | Build your first Kanketsu application.               |
| [🧠 Concepts](docs/concepts.md)               | Learn the core concepts behind Kanketsu.             |
| [💡 Examples](docs/examples.md)               | Practical examples and common CLI patterns.          |
| [📦 Modules](docs/modules.md)                 | Learn about the Core and optional ecosystem modules. |
| [🏛 Design](docs/design.md)                   | Understand the design philosophy of Kanketsu.        |
| [⚡ Performance](docs/performance.md)          | Benchmark results and Native Image performance.      |
| [🤝 Contributing](docs/contributing.md)       | Build, test, and contribute to the project.          |

---

## ❤️ Philosophy

Kanketsu intentionally keeps its Core small.

Features such as Spring integration, YAML support, annotation processing, shell completion, and other ecosystem capabilities belong in optional modules rather than the Core itself.

This approach keeps the framework lightweight while allowing the ecosystem to evolve independently.

---

## 📄 License

Licensed under the Apache License 2.0.
