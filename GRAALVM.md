
## Creating a GraalVM Native Image

This doesn't work. It's a record of my best attempt.

Create a file called `reflect.json` at the root of the project that looks like this:

```json
[
  {
    "name"                    : "com.sun.xml.internal.bind.v2.ContextFactory",
    "allDeclaredConstructors" : true,
    "allPublicConstructors"   : true,
    "allDeclaredMethods"      : true,
    "allPublicMethods"        : true,
    "allDeclaredClasses"      : true,
    "allPublicClasses"        : true
  },
  {
    "name"                    : "classes.Main$Data",
    "allDeclaredConstructors" : true,
    "allPublicConstructors"   : true,
    "allDeclaredMethods"      : true,
    "allPublicMethods"        : true,
    "allDeclaredClasses"      : true,
    "allPublicClasses"        : true
  }
]
```

Then do this to produce binary `itac-g`

```
cs bootstrap itac                                                \
  --channel cs                                                   \
  --native-image                                                 \
  -o itac-g                                                      \
  -J                                                             \
  --graalvm-opt  --                                              \
  --no-fallback                                                  \
  --initialize-at-build-time=scala.UniquenessCache,scala.Symbol$ \
  -H:IncludeResourceBundles=javax.xml.bind.Messages              \
  -H:ReflectionConfigurationFiles=reflect.json                   \
  -H:-UseServiceLoaderFeature                                    \
  -H:+AllowIncompleteClasspath
```
