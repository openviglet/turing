package com.viglet.turing.agent.persistence.mapper;

import com.viglet.turing.agent.persistence.dto.TurAIAgentDto;
import com.viglet.turing.agent.persistence.model.TurAIAgent;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurAIAgentMapper {
    TurAIAgentDto toDto(TurAIAgent entity);

    TurAIAgent toEntity(TurAIAgentDto dto);

    List<TurAIAgentDto> toDtoList(List<TurAIAgent> entities);

    Set<TurAIAgentDto> toDtoSet(Set<TurAIAgent> entities);
}
