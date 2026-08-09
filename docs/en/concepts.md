# 🧠 Concepts

This document introduces the core concepts behind Kanketsu.

Understanding these concepts will help you design CLI applications naturally, without memorizing APIs.

---

# 🌳 The Command Tree

Every Kanketsu application is organized as a **command tree**.

For example:

```text
git
├── add
├── commit
│   └── amend
└── push
```

Each node in the tree represents a command.

A command may contain:

* 📦 Child commands
* ⚙️ Options
* ▶️ An action

Nested commands are simply nested nodes in the same tree.

---

# 📦 Commands

A command represents a single operation.

Examples include:

```text
build
test
deploy
copy
commit
```

Each command has a clear responsibility.

A command defines:

* 🏷️ Its name
* ⚙️ Available options
* 📦 Child commands
* ▶️ What happens when it is executed

Commands do not perform parsing themselves. They describe the structure of the application.

---

# ⚙️ Options

Options modify how a command behaves.

For example:

```text
--file config.yml
-v
--force
```

Options always belong to a command.

Kanketsu supports both short and long options, optional values, required values, and default values.

Once parsing is complete, option values become part of the command context.

---

# 📍 Positional Arguments

Arguments that are not interpreted as options become **positional arguments**.

For example:

```text
copy source.txt destination.txt
```

Here:

* 📦 `copy` is the command.
* 📍 `source.txt` is the first positional argument.
* 📍 `destination.txt` is the second positional argument.

Positional arguments preserve their original order.

---

# 🧾 Command Context

When a command is executed, Kanketsu creates a **Command Context**.

The context contains everything required by the command.

Typical information includes:

* ⚙️ Parsed options
* 📍 Positional arguments
* 📦 The matched command
* 📝 Execution metadata

Instead of accessing global state, command actions receive all required information through the context object.

This makes commands easier to test and easier to reason about.

---

# 🔍 Parsing

The parser transforms user input into structured data.

Conceptually, the process looks like this:

```text
⌨️ User Input
        │
        ▼
🔍 Argument Parser
        │
        ▼
🌳 Command Resolution
        │
        ▼
🧾 Command Context
        │
        ▼
▶️ Command Action
```

Application code only interacts with the final result—the command context.

Everything before that is handled by the framework.

---

# ▶️ Execution

A Kanketsu application follows the same execution flow every time.

```text
⌨️ User Input
        │
        ▼
🔍 Parse Arguments
        │
        ▼
🌳 Resolve Command
        │
        ▼
🧾 Create Context
        │
        ▼
▶️ Execute Action
```

This predictable flow makes command execution easy to understand and easy to debug.

---

# 🚀 What's Next?

Now that you understand how Kanketsu models a CLI application, continue with:

* 💡 **Examples** — Practical command-line applications built with Kanketsu.
* 🏛 **Design** — The principles behind Kanketsu's architecture.
* 🧩 **Modules** — Extend the core framework with optional components.
