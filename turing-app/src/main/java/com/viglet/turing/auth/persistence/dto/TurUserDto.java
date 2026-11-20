package com.viglet.turing.auth.persistence.dto;

import com.viglet.turing.auth.persistence.model.TurUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TurUserDto extends TurUser {
}