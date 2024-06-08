# Sample with OpenTelemetry traces

Run the Spring Boot application.

```shell
./gradlew bootTestRun
```

You can call the application as follows.

```shell
http :8181/greeting
```

The application relies on the native Testcontainers support in Spring Boot to spin up a Grafana LGTM service for observability.

Grafana is listening to port 3000. Check your container runtime to find the port to which is exposed to your localhost and access Grafana from http://localhost:<port>.
The credentials are `admin`/`admin`.