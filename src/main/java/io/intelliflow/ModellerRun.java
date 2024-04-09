package io.intelliflow;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

public class ModellerRun implements QuarkusApplication {

    @Override
    public int run(String... args) {
        // keeping the main thread running, until Ctrl+C or has called Quarkus.asyncExit()
        Quarkus.waitForExit();

        return 0;
    }
}
