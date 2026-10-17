package com.jobagent.model;

import java.util.ArrayList;
import java.util.List;

public class FormField {
    private String label;
    private String fieldType; // TEXT, SELECT, RADIO, CHECKBOX, FILE
    private String selector;
    private boolean required;
    private List<String> options = new ArrayList<>();
    private String currentValue;
    private String suggestedValue;

    public FormField() {}

    public FormField(String label, String fieldType, String selector, boolean required) {
        this.label = label;
        this.fieldType = fieldType;
        this.selector = selector;
        this.required = required;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getSuggestedValue() {
        return suggestedValue;
    }

    public void setSuggestedValue(String suggestedValue) {
        this.suggestedValue = suggestedValue;
    }
}
