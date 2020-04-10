
Run from project root to produce `itac-g` (not working yet)

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
