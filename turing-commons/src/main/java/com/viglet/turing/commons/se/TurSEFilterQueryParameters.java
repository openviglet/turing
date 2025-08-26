package com.viglet.turing.commons.se;

import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ToString
public class TurSEFilterQueryParameters implements Serializable {
    private List<String> fq;
    private List<String> and;
    private List<String> or;
    private TurSNFilterQueryOperator operator;
    private TurSNFilterQueryOperator itemOperator;

    public TurSEFilterQueryParameters(List<String> fq, List<String> and, List<String> or,
                                      TurSNFilterQueryOperator operator, TurSNFilterQueryOperator itemOperator) {
        this.fq = fq != null ? fq : Collections.emptyList();
        this.and = and != null ? and : Collections.emptyList();
        this.or = or != null ? or : Collections.emptyList();
        this.operator = operator;
        this.itemOperator = itemOperator;
    }
}
