package com.viglet.turing.client.sn.job;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import com.viglet.turing.commons.se.field.TurSEFieldType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TurSNJobAttributeSpec implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    protected String name;
    protected TurSEFieldType type;
    protected boolean mandatory;
    protected boolean multiValued;
    protected String description;
    protected boolean facet;
    @SuppressWarnings("java:S1948")
    protected Map<String, String> facetName;

    @Override
    public String toString() {
        return "TurSNJobAttributeSpec{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", mandatory=" + mandatory +
                ", multiValued=" + multiValued +
                ", description='" + description + '\'' +
                ", facet=" + facet +
                ", facetName='" + facetName + '\'' +
                '}';
    }
}
