# 🏛 Design

Kanketsu is built on a simple belief:

> **A CLI framework should be explicit, predictable, and easy to understand.**

Every design decision in Kanketsu follows this belief. Rather than maximizing features, the framework focuses on maintaining a stable foundation that can evolve without sacrificing simplicity.

---

# 🎯 Design Principles

## ✨ Explicit over Magic

Kanketsu favors explicit APIs over implicit behavior.

Commands, options, and execution flow are declared directly in Java code. The framework performs no runtime discovery, hidden registration, or automatic wiring.

This approach makes applications easier to navigate, easier to debug, and easier to refactor.

**Code should be the source of truth.**

---

## 🧱 Static over Dynamic

Kanketsu prefers static structures over dynamic mechanisms.

Reflection, runtime classpath scanning, and other runtime discovery techniques are intentionally avoided whenever practical.

This philosophy results in:

* predictable runtime behavior;
* excellent GraalVM Native Image compatibility;
* no reflection configuration;
* faster startup and lower runtime overhead.

Static code is easier for both developers and compilers to understand.

---

## 🧩 Modularity over Monolith

The core framework should remain focused.

Kanketsu intentionally separates essential CLI functionality from optional ecosystem features. Instead of continuously expanding the core, additional capabilities are implemented as independent modules.

This keeps the core:

* lightweight;
* stable;
* easy to maintain;
* easy to adopt.

A feature is not excluded because it is unnecessary—it is excluded because it belongs somewhere else.

---

# 🏗 How These Principles Shape Kanketsu

The design principles are reflected throughout the framework.

| Decision                     | Principle                |
| ---------------------------- | ------------------------ |
| Builder-based API            | Explicit over Magic      |
| No Reflection                | Static over Dynamic      |
| No Runtime Scanning          | Static over Dynamic      |
| Zero Transitive Dependencies | Modularity over Monolith |
| Independent Modules          | Modularity over Monolith |

The implementation follows the principles—not the other way around.

---

# 📦 Core and Ecosystem

The responsibility of the core is intentionally limited.

The **Core** module focuses on:

* command routing;
* argument parsing;
* command execution;
* the public API required to build CLI applications.

Additional functionality belongs to the ecosystem.

Examples include:

* interactive REPL;
* logging integration;
* Spring integration;
* YAML or JSON configuration;
* annotation processing;
* shell completion;
* other platform-specific extensions.

Keeping these capabilities outside the core allows them to evolve independently without increasing the complexity of the framework itself.

---

# ⚖️ Trade-offs

Every architectural decision involves trade-offs.

Kanketsu intentionally chooses:

* explicit configuration over automatic discovery;
* modularity over convenience;
* long-term maintainability over feature accumulation.

As a result, applications may require slightly more code than annotation-driven frameworks.

This is not a limitation—it is a deliberate design decision that favors clarity, predictability, and maintainability.

---

# 🏗 A Stable Foundation

Kanketsu is not designed to become a monolithic CLI platform.

Instead, it aims to provide a small, stable, and predictable foundation upon which a rich ecosystem of modules can be built.

The core should remain small.

The ecosystem should remain free to grow.

Every new feature should strengthen these principles—not compromise them.
