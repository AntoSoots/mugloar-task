# 🐉 Mugloar Task (Backend • Java 21)

[![CI](https://github.com/AntoSoots/mugloar-task/actions/workflows/ci.yml/badge.svg)](https://github.com/AntoSoots/mugloar-task/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-007396.svg?logo=java&logoColor=white)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/build-Maven-8A2BE2.svg?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Runner%20Only-6DB33F.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/tests-JUnit%205%20%7C%20Mockito%20%7C%20AssertJ-25A162.svg)](#-tests)

A command-line bot that plays **Dragons of Mugloar** via the public API and reliably achieves **1000+** points.

> This project is a simple **CLI app** (not a web service).  
> You can run it with `mvn spring-boot:run` or build a JAR and run `java -jar target/...jar`.

---

<details>
<summary><b>📑 Table of Contents</b></summary>

- [Overview](#-overview)
- [Requirements](#-requirements)
- [Quick Start](#-quick-start)
  - [Dev Run](#dev-run)
  - [Run as JAR](#run-as-jar)
- [Configuration](#-configuration)
- [How It Works](#-how-it-works)
- [Project Structure](#-project-structure)
- [Design Notes](#-design-notes)
- [Tests](#-tests)
</details>

---

## 🌟 Overview

- **Language:** Java 21  
- **Packaging:** Maven  
- **Runner:** Spring Boot plugin (used only to launch the `main` — no web endpoints)  
- **HTTP:** `java.net.http.HttpClient` + Jackson  
- **Goal:** Automatically play a game and finish with **1000+** points

---

## ✅ Requirements

- **JDK:** 21  
- **Maven:** 3.9+  
- **Network:** Access to `https://www.dragonsofmugloar.com/api/v2`

---

## 🚀 Quick Start

### Dev Run
```bash
mvn spring-boot:run
```

### Run as JAR
1) Build the package (this compiles and runs tests):
```bash
mvn clean package
# or skip tests if needed:
mvn -DskipTests package
```

2) Run the built JAR:
```bash
java -jar target/mugloar-task-0.0.1-SNAPSHOT.jar
```

You should see a summary like:
```
Game finished: id=mBAHpXdI score=5327 turns=123
```

---

## ⚙️ Configuration

- **Base URL** is hard-coded in `MugloarTaskApplication`:
  ```
  https://www.dragonsofmugloar.com/api/v2
  ```
- **Logging level** : `src/main/resources/application.properties`:
  ```properties
  logging.level.ee.bigbank.task=INFO
  ```

---

## 🧠 How It Works

1. **Start game** → receive `gameId`, `lives`, `gold`, `score`.
2. **Fetch messages** → retrieve available tasks (“messagess”).
3. **Decode** → if `encrypted = "1"` decode **Base64**; if `"2"` decode **ROT13**; otherwise keep as is.
4. **Choose the best task** using a comparator:
   - highest **probability** (mapped from label via `Probability.valueForLabel(label)`),
   - then higher **expiresIn**,
   - then higher **reward**.
5. **Solve** → submit the chosen task.
6. **Shop policy**
   - Buy **Healing potion** if `lives <= 1` (or after all other items are already purchased).
   - Buy each **non-HP item once** per game when affordable, keeping a **gold reserve** for healing.
7. Repeat while `lives > 0` → produce a final `GameResult` and log it.

**Tiny sketch**

```
+-----------+    start     +------------+
| Main/CLI  | ------------> | GameClient |
+-----------+               +------------+
      |                          |
      | playGame()               v
      v                    [Mugloar API v2]
+------------+     ads     +------------+
| GameService| <---------- |  /messages |
+------------+             +------------+
      | decode()                     ^
      v                              |
+---------------+   buy if needed    |
| MessageDecoder| ------------------+
+---------------+
      |
      v
+------------+    solve    +------------+
| Probability| ----------> |  /solve    |
+------------+             +------------+
```

---

## 🗂 Project Structure

```
ee.bigbank.task
├─ MugloarTaskApplication         # main (CLI runner)
├─ api
│  ├─ GameClient                  # API client: start/messages/solve/shop/buy/investigate
│  └─ dto                         # API DTOs (records)
├─ core
│  ├─ GameService                 # game loop (selection + solve + shop)
│  ├─ MessageDecoder              # Base64/ROT13 support
│  ├─ Probability                 # probability label ↔ numeric value + lookups
│  ├─ ShopService                 # purchase policy + healing reserve
│  └─ model
│     └─ GameResult               # final outcome per run
└─ util
   └─ HttpHelper                  # HTTP + JSON parsing, basic logging
```

---

## 📝 Design Notes

- **CLI-only:** There is no HTTP server; Spring Boot is used only to run the `main`.
- **Probability mapping:** Encoded in `Probability` enum; convenience method `valueForLabel(String)` is used in the comparator.
- **Robust decoding:** Only produce a decoded `Message` when the probability label is recognized; otherwise keep the original.
- **Safe HTTP:** Path segments are percent-encoded (IDs may contain `=` etc.), errors throw a concise `ApiClientException`.
- **Shop caching:** Shop items are fetched once per game and reused.
- **Logs:** INFO for end-of-game summary; DEBUG contains step-by-step details if enabled.

---

## 🧪 Tests

Run all tests:
```bash
mvn test
```

Examples:
```bash
mvn -Dtest=GameClientTest test
mvn -Dtest=GameServiceTest#playGame_shouldSolveBestMessage_andStopWhenLivesDeplete test
```

**Coverage (by intent):**
- `GameClientTest` – URL building & encoding, per-endpoint calls
- `MessageDecoderTest` – Base64/ROT13, invalid inputs, passthrough
- `ProbabilityTest` – label mapping (case-insensitive + trimmed), valid ranges
- `ShopServiceTest` – purchase policy & healing reserve
- `HttpHelperTest` – 2xx parsing, list parsing, 4xx errors, malformed JSON
- `MugloarTaskApplicationTests` – minimal Spring context sanity checks

---
