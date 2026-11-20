package com.viglet.turing.system.persistence.mapper;

import com.viglet.turing.system.persistence.dto.TurLocaleDto;
import com.viglet.turing.system.persistence.model.TurLocale;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurLocaleMapper {
    TurLocaleDto toDto(TurLocale entity);

    TurLocale toEntity(TurLocaleDto dto);

    List<TurLocaleDto> toDtoList(List<TurLocale> entities);

    Set<TurLocaleDto> toDtoSet(Set<TurLocale> entities);

    @Mapping(target = "initials", ignore = true)
    void updateEntity(TurLocale source, @MappingTarget TurLocale target);
}