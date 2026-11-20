package com.viglet.turing.auth.persistence.dto.devtoken;

import com.viglet.turing.auth.persistence.model.devtoken.TurDevToken;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TurDevTokenDto extends TurDevToken {
}