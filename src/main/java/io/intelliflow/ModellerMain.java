package io.intelliflow;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ModellerMain {

    public static void main(String args[]) {

        Quarkus.run(ModellerRun.class, args);
    }


}
