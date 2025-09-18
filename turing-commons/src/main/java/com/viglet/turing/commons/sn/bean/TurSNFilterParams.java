package com.viglet.turing.commons.sn.bean;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TurSNFilterParams implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Builder.Default
    List<String> defaultValues = Collections.emptyList();
    @Builder.Default
    List<String> and = Collections.emptyList();
    @Builder.Default
    List<String> or = Collections.emptyList();
    TurSNFilterQueryOperator operator;
    TurSNFilterQueryOperator itemOperator;
}
