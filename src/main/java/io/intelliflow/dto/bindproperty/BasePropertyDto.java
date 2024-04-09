package io.intelliflow.dto.bindproperty;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public class BasePropertyDto {

    @NotBlank
    private String workspaceName;

    @NotBlank
    private String miniAppName;

    @NotBlank
    private String fileType;

    private String qualifiedObjectName;
    private String propertyName;
    private String bpmnFileName;
    private String id;

    private String dmnName;
    private String dmnNamespace;
    private String decisionName;
    private String businessRuleTaskName;
    private String businessRuleTaskId;
    private String ruleFlowGroup;
    private List<String> propertyNames;
    private boolean dataModelBinding;


    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    public BasePropertyDto() {
        this.id = UUID.randomUUID().toString();
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getMiniAppName() {
        return miniAppName;
    }

    public void setMiniAppName(String miniAppName) {
        this.miniAppName = miniAppName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getQualifiedObjectName() {
        return qualifiedObjectName;
    }

    public void setQualifiedObjectName(String qualifiedObjectName) {
        this.qualifiedObjectName = qualifiedObjectName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getBpmnFileName() {
        return bpmnFileName;
    }

    public void setBpmnFileName(String bpmnFileName) {
        this.bpmnFileName = bpmnFileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDmnName() {
        return dmnName;
    }

    public void setDmnName(String dmnName) {
        this.dmnName = dmnName;
    }

    public String getDmnNamespace() {
        return dmnNamespace;
    }

    public void setDmnNamespace(String dmnNamespace) {
        this.dmnNamespace = dmnNamespace;
    }

    public String getDecisionName() {
        return decisionName;
    }

    public void setDecisionName(String decisionName) {
        this.decisionName = decisionName;
    }

    public String getBusinessRuleTaskName() {
        return businessRuleTaskName;
    }

    public void setBusinessRuleTaskName(String businessRuleTaskName) {
        this.businessRuleTaskName = businessRuleTaskName;
    }

    public String getBusinessRuleTaskId() {
        return businessRuleTaskId;
    }

    public void setBusinessRuleTaskId(String businessRuleTaskId) {
        this.businessRuleTaskId = businessRuleTaskId;
    }

    public String getRuleFlowGroup() {
        return ruleFlowGroup;
    }

    public void setRuleFlowGroup(String ruleFlowGroup) {
        this.ruleFlowGroup = ruleFlowGroup;
    }

    public boolean getDataModelBinding() {
        return dataModelBinding;
    }

    public void setDataModelBinding(boolean dataModelBinding) {
        this.dataModelBinding = dataModelBinding;
    }
}
