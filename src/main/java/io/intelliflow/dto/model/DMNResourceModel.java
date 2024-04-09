package io.intelliflow.dto.model;

import java.util.List;

public class DMNResourceModel {

    private String name;
    private String namespace;
    private List<DecisionModel> decisions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<DecisionModel> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<DecisionModel> decisions) {
        this.decisions = decisions;
    }
}
