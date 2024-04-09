package io.intelliflow.factory;

import io.intelliflow.modeller.*;
import io.intelliflow.support.ModellerNameException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class GetModellerFactory {

    @Inject
    DMNModeller dmnModeller;

    @Inject
    BPMNModeller bpmnModeller;

    @Inject
    DataModeller dataModeller;

    @Inject
    FormModeller formModeller;

    public enum Modellers {DMNMODELLER, BPMNMODELLER, FORMMODELLER, DATAMODELLER};

    public BaseModeller getModeller(Modellers modeller) throws ModellerNameException {

        switch (modeller) {
            case DATAMODELLER:
                return dataModeller;
            case DMNMODELLER:
                return dmnModeller;
            case BPMNMODELLER:
                return bpmnModeller;
            case FORMMODELLER:
                return formModeller;
            default:
                return null;

        }
    }

}
