
# Module injection
Basic dependency injection support.

### Install Dependency

=== "build.gradle"

    ```groovy
    repositories {
        mavenCentral()
    }

    implementation("com.hexagonkt.extra:injection:$hexagonVersion")
    ```

=== "pom.xml"

    ```xml
    <dependency>
      <groupId>com.hexagonkt.extra</groupId>
      <artifactId>injection</artifactId>
      <version>$hexagonVersion</version>
    </dependency>
    ```

# Package com.hexagonkt.injection
Utilities to bind classes to creation closures or instances, and inject instances of those classes
later.
