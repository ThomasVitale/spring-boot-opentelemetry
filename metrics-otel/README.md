# Sample with OpenTelemetry metrics

First, ensure the observabiltiy stack is up and running. From the root directory, runs the following command.

```shell
docker compose up -d
```

Grafana will be accessible from `http://localhost:8080`.

Then, run the application.

```shell
./gradlew bootRun
```

You can call the application as follows.

```shell
http :8080/greeting
```