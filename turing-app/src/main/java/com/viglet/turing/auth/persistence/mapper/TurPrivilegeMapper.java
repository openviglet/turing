package com.viglet.turing.auth.persistence.mapper;

import com.viglet.turing.auth.persistence.dto.TurPrivilegeDto;
import com.viglet.turing.auth.persistence.model.TurPrivilege;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurPrivilegeMapper {
    TurPrivilegeDto toDto(TurPrivilege entity);

    TurPrivilege toEntity(TurPrivilegeDto dto);

    List<TurPrivilegeDto> toDtoList(List<TurPrivilege> entities);

    Set<TurPrivilegeDto> toDtoSet(Set<TurPrivilege> entities);
}