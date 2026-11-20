package com.viglet.turing.auth.persistence.mapper;

import com.viglet.turing.auth.persistence.dto.TurUserDto;
import com.viglet.turing.auth.persistence.model.TurUser;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurUserMapper {
    TurUserDto toDto(TurUser entity);

    TurUser toEntity(TurUserDto dto);

    List<TurUserDto> toDtoList(List<TurUser> entities);

    Set<TurUserDto> toDtoSet(Set<TurUser> entities);
}