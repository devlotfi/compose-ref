<img src="https://raw.githubusercontent.com/devlotfi/compose-ref/master/github-assets/github-banner.png">

# 📜 compose-ref

A tiny **Jetpack Compose** utility that brings React's `useRef` to Compose. 
Keep a mutable value alive across recompositions **without** triggering a recomposition when it changes.

```kotlin
val counter = rememberRef(0)

Button(onClick = { counter.current++ }) { Text("Click me") }
```

## Features

- **Mutable container**: a `Ref<T>` holds a single value in `current` that you can read and write freely.
- **Survives recomposition**: the same `Ref` instance is returned on every recomposition.
- **No recomposition on write**: updating `current` never invalidates the UI, unlike `mutableStateOf`.
- **Works with any type**: nullable values, jobs, counters, flags, callbacks, previous values...
- **Tiny API**: one class, one composable, zero dependencies besides Compose runtime.

# 📌 Contents

- [Installation](#installation)
- [Docs](#docs)

# Installation

ComposeRef is distributed through [JitPack](https://jitpack.io/#devlotfi/compose-ref).

[![](https://jitpack.io/v/devlotfi/compose-ref.svg)](https://jitpack.io/#devlotfi/compose-ref)

Add the JitPack repository at the end of the repositories block in your root `settings.gradle.kts`:

# Docs

## API

### `Ref<T>`

```kotlin
class Ref<T>(var current: T)
```

A simple mutable holder. Read and write the value through `current`.

### `rememberRef`

```kotlin
@Composable
fun <T> rememberRef(initialValue: T): Ref<T>
```

| Parameter | Type | Description |
|---|---|---|
| `initialValue` | `T` | The starting value of `current`. Only used the first time the composable enters the composition. |

Returns the same `Ref<T>` instance for as long as the composable stays in the composition.

---

## Basic usage

```kotlin
@Composable
fun ClickCounter() {
    val clicks = rememberRef(0)

    Button(onClick = {
        clicks.current++
        println("Clicked ${clicks.current} times")
    }) {
        Text("Click me")
    }
}
```

The value is preserved between recompositions, but the UI is **not** redrawn when `clicks.current` changes. This is on purpose.

---

## Ref vs. state

| | `rememberRef` | `remember { mutableStateOf() }` |
|---|---|---|
| Survives recomposition | ✅ | ✅ |
| Triggers recomposition on change | ❌ | ✅ |
| Use for | Data the UI doesn't display | Data the UI displays |

Rule of thumb: if changing the value should update the screen, use state. If it's just bookkeeping, use a ref.

---

## Examples

### Holding a coroutine `Job`

Cancel the previous request before starting a new one:

```kotlin
@Composable
fun SearchScreen() {
    val scope = rememberCoroutineScope()
    val searchJob = rememberRef<Job?>(null)
    var results by remember { mutableStateOf<List<String>>(emptyList()) }

    fun search(query: String) {
        searchJob.current?.cancel()
        searchJob.current = scope.launch {
            results = api.search(query)
        }
    }

    // ...
}
```

### Discarding stale responses with a request counter

```kotlin
val requestCounter = rememberRef(0)

fun load() {
    val requestId = ++requestCounter.current
    scope.launch {
        val res = api.fetch()
        if (requestId == requestCounter.current) {
            // only the latest request updates the UI
            data = res
        }
    }
}
```

### Skipping the first run of an effect

```kotlin
@Composable
fun AnalyticsOnChange(value: String) {
    val isFirstRun = rememberRef(true)

    LaunchedEffect(value) {
        if (isFirstRun.current) {
            isFirstRun.current = false
            return@LaunchedEffect
        }
        analytics.log("value changed to $value")
    }
}
```

### Remembering the previous value

```kotlin
@Composable
fun <T> rememberPrevious(value: T): T? {
    val previous = rememberRef<T?>(null)
    val last = previous.current
    SideEffect { previous.current = value }
    return last
}
```

### Counting recompositions (debugging)

```kotlin
@Composable
fun DebugScreen() {
    val renders = rememberRef(0)
    renders.current++
    Log.d("Debug", "Recomposed ${renders.current} times")
}
```

### Reading the latest value inside long-lived callbacks

Avoid stale captures in callbacks or effects that outlive a single composition:

```kotlin
@Composable
fun Timer(onTick: (Int) -> Unit) {
    val latestOnTick = rememberRef(onTick)
    SideEffect { latestOnTick.current = onTick }

    LaunchedEffect(Unit) {
        var seconds = 0
        while (true) {
            delay(1000)
            latestOnTick.current(++seconds)
        }
    }
}
```

### Keeping a reference to a non-UI object

```kotlin
@Composable
fun VideoScreen(context: Context) {
    val player = rememberRef<ExoPlayer?>(null)

    DisposableEffect(Unit) {
        player.current = ExoPlayer.Builder(context).build()
        onDispose {
            player.current?.release()
            player.current = null
        }
    }
}
```

---

## Good to know

- **Not observable**: writing to `current` does not recompose anything. Never use a ref for values the UI must display.
- **Initial value only once**: `initialValue` is read the first time; later changes to the argument are ignored.
- **Not persisted**: the ref is lost when the composable leaves the composition, including configuration changes and process death. Use `rememberSaveable` or a `ViewModel` if you need to keep the value.
- **Avoid writing during composition**: write to `current` in event handlers, effects (`LaunchedEffect`, `SideEffect`, `DisposableEffect`) or callbacks. Writing during composition makes your composable depend on side effects and may behave unexpectedly if composition is retried or skipped.
- **Not thread-safe**: `Ref` doesn't synchronize access. If several threads write to it, add your own synchronization or use atomic types.
- **Reference equality**: `Ref` is a plain class (not a `data class`), so two refs are never equal unless they are the same instance.

---

## Used by

- [compose-query](https://github.com/devlotfi/compose-query): uses refs to track in-flight jobs, request counters and the first-resume flag.
