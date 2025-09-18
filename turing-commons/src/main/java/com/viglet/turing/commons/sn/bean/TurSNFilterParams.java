package com.viglet.turing.commons.sn.bean;

import java.util.Collections;
import java.util.List;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TurSNFilterParams {
    @Builder.Default
    List<String> defaultValues = Collections.emptyList();
    @Builder.Default
    List<String> and = Collections.emptyList();
    @Builder.Default
    List<String> or = Collections.emptyList();
    TurSNFilterQueryOperator operator;
    TurSNFilterQueryOperator itemOperator;
}
