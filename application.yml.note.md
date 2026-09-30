# Backend build notes

- **Gradle wrapper jar not committed here.** This scaffold ships `gradlew`,
  `gradlew.bat` and `gradle/wrapper/gradle-wrapper.properties`, but not the
  binary `gradle-wrapper.jar`. Generate it once where Gradle is available:
  ```
  gradle wrapper --gradle-version 8.10.2
  ```
  CI does this (or caches it) before `./gradlew build`.
- **Local JDK here is 17**; the project targets **Java 21** via the Gradle
  toolchain, which provisions JDK 21 in CI. Do not lower the target.
- Build: `./gradlew build` · Tests + coverage: `./gradlew test jacocoTestReport jacocoTestCoverageVerification`.
- Run locally against Postgres from docker-compose: `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`.
