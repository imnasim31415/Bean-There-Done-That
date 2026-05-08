# Bean There Done That ☕

A Spring Boot web application that **visualizes Spring Dependency Injection and Bean Lifecycle in real time** — built as a learning and educational tool for developers who want to understand what Spring is actually doing under the hood.

---

## What This App Does

Most Spring tutorials tell you *what* to write. This app shows you *what Spring does with it*.

When you start the application, it:

1. Intercepts every bean Spring creates using a custom `BeanPostProcessor`
2. Records the exact moment each lifecycle stage happens — instantiation, injection, `@PostConstruct`, full initialization, and `@PreDestroy`
3. Captures the full application startup timeline from `SpringApplication.run()` all the way to "ready to serve requests"
4. Builds a live dependency graph showing which beans depend on which other beans
5. Exposes all of this through a REST API and a dark-themed web UI

The result: you can open a browser and see your entire Spring context — every bean, every dependency, every lifecycle event — laid out clearly.

---

## Features

### Bean Lifecycle Tracking

Every bean goes through these stages, and all of them are captured:

```
INSTANTIATED → DEPENDENCIES_INJECTED → POST_CONSTRUCT → INITIALIZED → PRE_DESTROY
```

Each event is stored with a timestamp and a human-readable detail message. You can inspect the full lifecycle of any single bean at `/dashboard/beans/{beanName}`.

### Dependency Graph

The app resolves the dependency edges between beans by inspecting constructor parameters using reflection. The result is exposed as:

- A JSON graph at `GET /debug/beans` — suitable for plugging into any visualization library (D3, vis.js, Cytoscape)
- An SVG graph rendered directly in the browser at `/dashboard/beans`

The graph shows bean nodes colored by scope (purple = singleton, orange = prototype), with directed arrows from dependent → dependency.

### Application Startup Timeline

All 7 Spring startup phases are captured in order with timestamps:

| Phase | What it means |
|---|---|
| `APPLICATION_STARTING` | `SpringApplication.run()` called |
| `ENVIRONMENT_PREPARED` | `application.properties` loaded, profiles resolved |
| `CONTEXT_INITIALIZED` | `ApplicationContext` object created, `BeanFactory` ready |
| `CONTEXT_LOADED` | All `@Bean` definitions registered, not yet instantiated |
| `CONTEXT_REFRESHED` | All beans instantiated, wired, and `@PostConstruct` called |
| `APPLICATION_STARTED` | Application started, `CommandLineRunner`s not yet called |
| `APPLICATION_READY` | Ready to serve HTTP requests |

> The first four phases fire *before* the Spring context exists, so `@EventListener` cannot catch them. This app solves that with `EarlyStartupCapture` — a `SmartApplicationListener` registered directly on `SpringApplication` that stores events in a static buffer, then drains them into `BeanRegistry` once the context is ready.

### Bean Scope Demonstration

Hit `GET /scope/demo` multiple times and compare `instanceId` values:

- **Singleton** — same ID every request. Spring creates it once at startup.
- **Prototype** — different ID on every `ObjectProvider.getObject()` call. New instance each time.
- **Request-scoped** — same ID within one HTTP request, new ID on the next request. Powered by a CGLIB scoped proxy so the singleton controller can hold a reference without getting a stale instance.

### Circular Dependency Detection (heuristic)

`GET /debug/circular` runs a lightweight name-match check: if bean A depends on type B, and bean B depends on type A, it flags the pair. Spring itself prevents true circular dependencies by default (throws on detection), so this is most useful for catching *near-cycles* or mis-designed graphs.

---

## Architecture

```
com.beanthere/
├── BeanThereDoneThatApplication.java   Entry point. Registers EarlyStartupCapture before run().
│
├── model/
│   ├── BeanInfo.java                   Mutable, thread-safe bean record. Holds events + deps.
│   ├── LifecycleEvent.java             Immutable: stage + timestamp + detail.
│   ├── LifecycleStage.java             Enum of lifecycle stages.
│   ├── DependencyNode.java             JSON projection of BeanInfo for the graph API.
│   └── StartupEvent.java               Immutable startup phase record.
│
├── store/
│   └── BeanRegistry.java               ConcurrentHashMap central in-memory store.
│                                       All writes thread-safe — BPP runs on parallel init threads.
│
├── processor/
│   └── LifecycleBeanPostProcessor.java BeanPostProcessor + ApplicationContextAware.
│                                       postProcessBeforeInitialization → records INSTANTIATED
│                                         and DEPENDENCIES_INJECTED, resolves dep edges.
│                                       postProcessAfterInitialization → records INITIALIZED.
│                                       Uses ApplicationContextAware (not @Autowired) to get
│                                         BeanRegistry, avoiding circular BPP registration.
│
├── listener/
│   ├── EarlyStartupCapture.java        SmartApplicationListener added to SpringApplication
│   │                                   directly. Captures pre-context events into a static list.
│   └── ApplicationStartupListener.java @EventListener bean. On ContextRefreshedEvent, drains
│                                       EarlyStartupCapture's static list into BeanRegistry,
│                                       then records post-context phases.
│
├── service/                            Demo DI graph (coffee-themed):
│   ├── CoffeeRepository.java           Leaf. @PostConstruct warms connection pool.
│   ├── OrderService.java               Depends on CoffeeRepository.
│   ├── NotificationService.java        Depends on OrderService.
│   ├── BaristaService.java             Root. Depends on OrderService + NotificationService.
│   └── DependencyGraphService.java     Builds List<DependencyNode> from BeanRegistry.
│                                       Runs circular dependency heuristic.
│
├── config/
│   └── ScopedBeans.java                Declares prototype and request-scoped CoffeeOrder beans.
│                                       CoffeeOrder is a plain class (not a record) because
│                                         CGLIB must subclass it for the request-scoped proxy —
│                                         records are implicitly final and cannot be subclassed.
│
└── controller/
    ├── HomeController.java             / → redirect to /dashboard
    ├── DebugController.java            REST API: /debug/**
    ├── ScopeController.java            REST API: /scope/demo
    └── DashboardController.java        MVC: /dashboard/**  (Thymeleaf views)
```

### How the BeanPostProcessor Works

`LifecycleBeanPostProcessor` is the heart of the tracking system. Spring calls it for every bean it creates:

```
Spring creates bean
        │
        ▼
postProcessBeforeInitialization()   ← we record INSTANTIATED + DEPENDENCIES_INJECTED here
        │                              we also inspect constructor fields to find dep edges
        ▼
@PostConstruct method runs          ← the bean itself records POST_CONSTRUCT in its own @PostConstruct
        │
        ▼
postProcessAfterInitialization()    ← we record INITIALIZED here
        │
        ▼
Bean is fully ready
```

A key design constraint: `LifecycleBeanPostProcessor` cannot use `@Autowired BeanRegistry` because Spring registers `BeanPostProcessor` beans early — before other beans are created — and using `@Autowired` would force `BeanRegistry` to be created during that early phase, causing a circular dependency warning and potentially incorrect behavior. The fix is `ApplicationContextAware`: the processor gets the `ApplicationContext` injected, then lazily looks up `BeanRegistry` on first use.

### How the Startup Timeline Works

Spring's early lifecycle events (`APPLICATION_STARTING`, `ENVIRONMENT_PREPARED`, etc.) fire before the `ApplicationContext` exists. No `@EventListener` bean can receive them because no beans exist yet.

The solution is a two-layer capture:

```
SpringApplication.run() starts
        │
        ├── EarlyStartupCapture (registered via app.addListeners())
        │   Fires for: APPLICATION_STARTING, ENVIRONMENT_PREPARED,
        │              CONTEXT_INITIALIZED, CONTEXT_LOADED
        │   Stores events in a static CopyOnWriteArrayList
        │
        ▼  (context created, beans wired)
        │
        ├── ApplicationStartupListener (@EventListener bean)
        │   ContextRefreshedEvent fires → drains EarlyStartupCapture's static list
        │                                  into BeanRegistry, then clears it
        │   Records: CONTEXT_REFRESHED, APPLICATION_STARTED, APPLICATION_READY
```

---

## REST API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/debug/beans` | Full dependency graph — all tracked beans with scope and dependency edges |
| GET | `/debug/beans/{beanName}` | One bean — class, scope, dependencies, and all lifecycle events with timestamps |
| GET | `/debug/startup` | Full 7-phase startup timeline with timestamps |
| GET | `/debug/summary` | Bean counts by scope + number of startup phases captured |
| GET | `/debug/circular` | Heuristic circular dependency hints |
| GET | `/scope/demo` | Live singleton vs prototype vs request-scoped instance ID comparison |

---

## Web UI

| Route | What you see |
|-------|-------------|
| `/dashboard` | Overview: total beans, scope counts, startup phases, links to all sections |
| `/dashboard/beans` | Filterable bean table + live SVG dependency graph |
| `/dashboard/beans/{name}` | Full lifecycle event timeline for one bean |
| `/dashboard/lifecycle` | Startup phase timeline + all bean events in a single table |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.4.5 |
| Language | Java 21 |
| Web | Spring MVC |
| Templating | Thymeleaf |
| Monitoring | Spring Boot Actuator |
| Storage | In-memory (ConcurrentHashMap, CopyOnWriteArrayList) |
| Build | Maven 3.8+ |
| Logging | SLF4J (via Spring Boot) |
| Graph rendering | Inline SVG (no external JS dependencies) |

No database required. No external services. Everything lives in memory for the lifetime of the JVM process.

---

## Running the App

**Requirements:** Java 21, Maven 3.8+

```bash
git clone https://github.com/imnasim31415/Bean-There-Done-That.git
cd Bean-There-Done-That
mvn spring-boot:run
```

Open **http://localhost:8080**

---

## Demo DI Graph

The app ships with a coffee-themed service graph to demonstrate multi-level dependency injection:

```
BaristaService
├── OrderService
│   └── CoffeeRepository    @PostConstruct: warms connection pool
└── NotificationService
    └── OrderService         (same singleton — Spring reuses it)
```

`BaristaService` is the root. It depends on both `OrderService` and `NotificationService`. `NotificationService` also depends on `OrderService` — but since `OrderService` is a singleton, Spring injects the same instance, not a second one. You can verify this in the lifecycle UI: `OrderService` appears once in the registry with a single set of lifecycle events.

---

## Extending This

**Add your own beans to the graph**

Any `@Component`, `@Service`, `@Repository`, or `@Bean` in the `com.beanthere` package is automatically tracked. Add a new service class and it will appear in the dashboard on next startup.

**Visualize in a browser with D3 or vis.js**

Fetch `GET /debug/beans` and feed the JSON into [vis-network](https://visjs.github.io/vis-network/docs/network/) or [D3-force](https://d3js.org/d3-force). The `id`, `dependsOn`, and `scope` fields map directly to node/edge structures those libraries expect.

**Measure bean creation time**

In `LifecycleBeanPostProcessor`, store `Instant.now()` at the start of `postProcessBeforeInitialization` and diff it against `postProcessAfterInitialization`. Add a `durationMs` field to `BeanInfo`.

**Detect true circular dependencies**

Walk `BeanRegistry.getAll()` and run DFS with a visited + recursion-stack set on the dependency edges. Flag any node whose dependency chain leads back to itself.
