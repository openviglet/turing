package com.viglet.turing.commons.sn.bean;

import java.util.List;
import java.util.Locale;

import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TurSNSearchParams {
    private String q = "*";
    private Integer p = 1;
    private List<String> fq;
    private List<String> fqAnd;
    private List<String> fqOr;
    private TurSNFilterQueryOperator fqOp = TurSNFilterQueryOperator.NONE;
    private TurSNFilterQueryOperator fqiOp = TurSNFilterQueryOperator.NONE;
    private String sort = "relevance";
    private Integer rows = -1;
    private Locale locale;
    private List<String> fl;
    /** T1250 — facet fields this request leaves out ({@code exf=year&exf=type}). */
    private List<String> exf;
    private String group;
    private Integer nfpr = 1;

}
