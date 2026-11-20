package com.viglet.turing.llm.persistence.dto;

import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TurLLMInstanceDto extends TurLLMInstance {
}