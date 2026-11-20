package com.viglet.turing.system.persistence.mapper;

import com.viglet.turing.system.persistence.dto.TurConfigVarDto;
import com.viglet.turing.system.persistence.model.TurConfigVar;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurConfigVarMapper {
    TurConfigVarDto toDto(TurConfigVar entity);

    TurConfigVar toEntity(TurConfigVarDto dto);

    List<TurConfigVarDto> toDtoList(List<TurConfigVar> entities);

    Set<TurConfigVarDto> toDtoSet(Set<TurConfigVar> entities);
}