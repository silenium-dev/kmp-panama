# kmp-panama

A KMP wrapper for Panama API supporting all JVM-based platforms

## Supported platforms

- JVM
- Android

## Usage

You can add the dependency to your project as follows:

```kotlin
dependencies {
    implementation("dev.silenium.libs.panama:kmp-panama:0.1.0")
}
```

### Development Snapshots

Snapshots are available
from [silenium-dev-snapshots](https://nexus.silenium.dev/repository/maven-snapshots/).

The exposed API is a subset of the [Panama API](https://openjdk.org/projects/panama/), just with a
different package name: `dev.silenium.libs.foreign`
If you're missing anything, please open an issue or, even better, a pull request.
