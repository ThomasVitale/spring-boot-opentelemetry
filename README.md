# Spring Boot OpenTelemetry

## Metrics

Spring Boot Actuator provides dependency management and autoconfiguration for Micrometer,
which offers a convenient facade over many different monitoring systems. One of them is OpenTelemetry.

Spring Boot developers can enable their applications to export metrics via the OTLP protocol to an OpenTelemetry
backend by adding Spring Boot Actuator and the dedicated OpenTelemetry dependency from Micrometer to their projects.

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    runtimeOnly 'io.micrometer:micrometer-registry-otlp'
    ...
}
```

The exporter provided by the [Micrometer Registry OTLP](https://micrometer.io/docs/registry/otlp) is an HTTP exporter
and can be configured via properties thanks to the [autoconfiguration for OpenTelemetry](https://docs.spring.io/spring-boot/docs/3.2.0-SNAPSHOT/reference/html/actuator.html#actuator.metrics.export.otlp).

```yaml
management:
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
        step: 5s
```

OpenTelemetry supports additional key/value pairs (called `resource attributes`) to be included in the telemetry data. Spring Boot Actuator provides
[autoconfiguration](https://docs.spring.io/spring-boot/docs/3.2.0-SNAPSHOT/reference/html/actuator.html#actuator.observability.opentelemetry) for those
and makes it possible to add new resource attributes via properties.

```yaml
management:
  opentelemetry:
    resource-attributes:
      cluster: local
      "service.name": ${spring.application.name}
```

I made a demo application to showcase this setup.

### Issue: OTLP Metrics Exporter is not configurable via OpenTelemetry

The Micrometer Registry OTLP module uses an OTLP-compatible HTTP client to export metrics to an OpenTelemetry backend.
Internally, the `OtlpMeterRegistry` uses a private `HttpSender` object to configure the HTTP client. The benefit of this
approach is that the module is lightweight and doesn't need any dependency on the OpenTelemetry SDK. It only uses the
`io.opentelemetry.proto` library which provides the protobuf configuration for OpenTelemetry.

On the other hand, such an approach means that:

* it's not possible to configure the metrics exporter via standard OpenTelemetry configuration;
* it's not possible to configure compression;
* it's not possible to share OpenTelemetry configuration in Spring Boot between metrics and traces.

The new generic `OpenTelemetryAutoConfiguration` in Spring Boot 3.2 autoconfigures an `OpenTelemetry` bean and makes it possible
to configure an `SdkMeterProvider` bean. What if Micrometer could use that for setting up the OpenTelemetry metrics exporter?

Possible solutions:

* update the existing Micrometer Registry OTLP module to implement a `MeterRegistry` that accepts an `OpenTelemetry` object
  for configuration (similar to the [`OpenTelemetryMeterRegistry`](https://github.com/runningcode/opentelemetry-java-instrumentation/blob/main/instrumentation/micrometer/micrometer-1.5/library/src/main/java/io/opentelemetry/instrumentation/micrometer/v1_5/OpenTelemetryMeterRegistry.java)
  used by the OpenTelemetry Java Instrumentation library). If backward compatibility is necessary, the new implementation would
  need to co-exist with the existing one (maybe a feature flag could switch between the two?);
* create a new Micrometer Registry OpenTelemetry module to implement what described in the previous point, but without having issues
  with backward compatibility.

I have submitted an issue to the Micrometer project and shared these suggestions. Building (or updating) such a module would make it possible
to re-use the same `OpenTelemetryAutoConfiguration` introduced in Spring Boot 3.2 for both metrics and traces (and, in the future, for logs as well).

* An [issue](https://github.com/spring-projects/spring-boot/issues/36546) already exists on the Spring Boot project to autoconfigure an `SdkMeterProvider`
  bean and, in general, for using metrics with OpenTelemetry. But we are missing Micrometer support before that is doable.
* It would also make it possible to switch the client implementation between HTTP and gRPC using the standard OpenTelemetry approach (i.e. configuring either an
  `OtlpHttpMetricExporter` or `OtlpGrpcMetricExporter`).
* Finally, it would make it easier for developers using Micrometer in applications together with the OpenTelemetry Java Agent or, in general, the OpenTelemetry
  Java Instrumentation. Those projects currently maintain a shim between Micrometer and OpenTelemetry. That shim would probably not be needed anymore.
  So, such a change would also reduce the workload on maintaining the OpenTelemetry Java Instrumentation compatible with Micrometer.

For more context about the current challenges, refer to this [issue](https://github.com/spring-projects/spring-boot/issues/34023) on the Spring Boot project.

## Traces

Spring Boot Actuator provides dependency management and autoconfiguration for Micrometer Tracing,
which offers a convenient facade over a few different distributed tracing backends. One of them is OpenTelemetry.

Spring Boot developers can enable their applications to export traces via the OTLP protocol to an OpenTelemetry backend by adding Spring Boot Actuator, Micrometer Tracing and the dedicated OpenTelemetry dependency from Micrometer to their projects.

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
    ...
}
```

The exporter provided by the Spring Boot Actuator autoconfiguration is an HTTP exporter (an `OtlpHttpSpanExporter` bean) and can be configured via properties thanks to the tracing configuration in `OtlpAutoConfiguration`.

```yaml
management:
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

OpenTelemetry supports additional key/value pairs (called `resource attributes`) to be included in the telemetry data. Spring Boot Actuator provides [autoconfiguration](https://docs.spring.io/spring-boot/docs/3.2.0-SNAPSHOT/reference/html/actuator.html#actuator.observability.opentelemetry) for those and makes it possible to add new resource attributes via properties. The standard OpenTelemetry `service.name` resource attribute is configured automatically to the value of `spring.application.name` (if defined) or else to a default `application` value.

```yaml
management:
  opentelemetry:
    resource-attributes:
      cluster: local
```

I made a demo application to showcase this setup.

### Issue Exporting traces via gRPC

In this [issue](https://github.com/spring-projects/spring-boot/issues/35596) on the Spring Boot project, autoconfiguration for an `OtlpHttpSpanExporter` bean has been added to export traces via HTTP.

A common requirement is to export traces via gRPC (the most used approach in OpenTelemetry). Currently, developers can configure an `OtlpGrpcSpanExporter` bean by themselves. It would be nice if Spring Boot Actuator would provide autoconfiguration for that, enhancing the existing `OtlpAutoConfiguration`.

There is already an [issue](https://github.com/spring-projects/spring-boot/issues/35863) on the Spring Boot project to add such autoconfiguration.

## Summary

Overall, these are the issues and suggestions I've been describing so far.

### Minimal proposed changes to consolidate OpenTelemetry support in Spring for metrics and traces

**Micrometer**

* Introduce a `MeterRegistry` implementation built on top of OpenTelemetry and configurable via its standard approaches, so that it's possible to share configuration between metrics and traces.

**Spring Boot Actuator**

* Introduce autoconfiguration for OpenTelemetry metrics. The `OpenTelemetry` and `Resource` beans can be reused from the existing OpenTelemetry configuration. Besides that, the autoconfiguration should define defaults for `SdkMeterProvider`, `OtlpHttpMetricExporter` and `OtlpGrpcMetricExporter`.
* Introduce autoconfiguration for exporting OpenTelemetry traces via gRPC by defining a `OtlpGrpcSpanExporter` bean.
* Consolidate OpenTelemetry autoconfiguration classes across metrics and traces. The existing [OpenTelemetryAutoConfiguration](https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/opentelemetry/OpenTelemetryAutoConfiguration.java#L71) could be used for shared configuration. Besides that, dedicated autoconfiguration classes are needed to configure the specific exporters for each type of telemetry. Traces have that already in the [OtlpAutoConfiguration](https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-actuator-autoconfigure/src/main/java/org/springframework/boot/actuate/autoconfigure/tracing/otlp/OtlpAutoConfiguration.java).

**Spring Initializr**

* After going through all the previous changes, it should become straightforward adding an _OpenTelemetry_ option to the Spring Initializr. Adding it now with the current state of things, it might make things confusing considering the different ways of configuring OpenTelemtry between metrics and traces.

### Additional changes to extend OpenTelemetry support in Spring to logs

**Spring Boot Actuator**

* Introduce autoconfiguration for OpenTelemetry logs. The `OpenTelemetry` and `Resource` beans can be reused from the existing OpenTelemetry configuration. Besides that, the autoconfiguration should define defaults for `SdkLoggerProvider`, `OtlpHttpLogRecordExporter` and `OtlpGrpcLogRecordExporter`.
* Add support for Logback (default) and Log4J2. For inspiration, OpenTelemetry Java Instrumentation provides appenders for [Logback](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0/library) and [Log4J2](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/log4j/log4j-appender-2.17/library).
* Possibly related [issue](https://github.com/spring-projects/spring-boot/issues/25847) on the Spring Boot project.

### Further improvements

* An [issue](https://github.com/spring-projects/spring-boot/issues/36248) has been opened to investigate how to limit the number of configurable beans currently available in the tracing `OpenTelemetryAutoConfiguration` (this issue might be considered for the global `OpenTelemetryAutoConfiguration` once the tracing specific one gets deleted).
