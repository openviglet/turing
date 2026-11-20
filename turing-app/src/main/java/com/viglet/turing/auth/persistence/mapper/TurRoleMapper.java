package com.viglet.turing.auth.persistence.mapper;

import com.viglet.turing.auth.persistence.dto.TurRoleDto;
import com.viglet.turing.auth.persistence.model.TurRole;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurRoleMapper {
    TurRoleDto toDto(TurRole entity);

    TurRole toEntity(TurRoleDto dto);

    List<TurRoleDto> toDtoList(List<TurRole> entities);

    Set<TurRoleDto> toDtoSet(Set<TurRole> entities);
}