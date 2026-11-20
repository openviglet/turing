package com.viglet.turing.auth.persistence.mapper;

import com.viglet.turing.auth.persistence.dto.TurGroupDto;
import com.viglet.turing.auth.persistence.model.TurGroup;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurGroupMapper {
    TurGroupDto toDto(TurGroup entity);

    TurGroup toEntity(TurGroupDto dto);

    List<TurGroupDto> toDtoList(List<TurGroup> entities);

    Set<TurGroupDto> toDtoSet(Set<TurGroup> entities);
}