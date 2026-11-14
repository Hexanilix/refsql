# IMPORTANT NOTICE
If you get weird table column names, it's probably because the java compiler can optimize variable and parameter names.
Set the `-parameters` flag in the compiler to fix this.

This is also required for inferring column names from Constructor parameters in [ConstructorClassConverter](src/main/java/dev/hexanilix/refsql/table/schema/middleware/ConstructorClassConverter.java)

Maven:
```xml
<configuration>
    <parameters>true</parameters>
</configuration>
```

Gradle:
```kotlin
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += '-parameters'
}
```