package com.viglet.turing.api.sn.bean;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TurSNSiteCustomFacetResponse {
    private String label;
    private String attribute;
    private List<Map<String, String>> facetItems;
}
