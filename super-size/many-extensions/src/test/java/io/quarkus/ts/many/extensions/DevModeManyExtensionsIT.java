package io.quarkus.ts.many.extensions;

import io.quarkus.test.bootstrap.DevModeQuarkusService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
public class DevModeManyExtensionsIT extends ManyExtensionsIT {
    private static final String RED_PANDA_VERSION = "v21.10.3";
    private static final String RED_PANDA_IMAGE = "quay.io/quarkusqeteam/redpanda";

    @DevModeQuarkusApplication
    static RestService app = new DevModeQuarkusService()
            .withProperty("quarkus.kafka.devservices.enabled", Boolean.TRUE.toString())
            .withProperty("quarkus.kafka.devservices.image-name", String.format("%s:%s", RED_PANDA_IMAGE, RED_PANDA_VERSION));
}
