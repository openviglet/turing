package com.viglet.turing.llm.persistence.mapper;

import com.viglet.turing.llm.persistence.dto.TurLLMInstanceDto;
import com.viglet.turing.llm.persistence.model.TurLLMInstance;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurLLMInstanceMapper {
    TurLLMInstanceDto toDto(TurLLMInstance entity);

    TurLLMInstance toEntity(TurLLMInstanceDto dto);

    List<TurLLMInstanceDto> toDtoList(List<TurLLMInstance> entities);

    Set<TurLLMInstanceDto> toDtoSet(Set<TurLLMInstance> entities);
}