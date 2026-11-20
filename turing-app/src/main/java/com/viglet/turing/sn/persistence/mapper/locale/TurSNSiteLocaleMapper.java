package com.viglet.turing.sn.persistence.mapper.locale;

import com.viglet.turing.sn.persistence.dto.locale.TurSNSiteLocaleDto;
import com.viglet.turing.sn.persistence.model.locale.TurSNSiteLocale;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteLocaleMapper {
    TurSNSiteLocaleDto toDto(TurSNSiteLocale entity);

    TurSNSiteLocale toEntity(TurSNSiteLocaleDto dto);

    List<TurSNSiteLocaleDto> toDtoList(List<TurSNSiteLocale> entities);

    Set<TurSNSiteLocaleDto> toDtoSet(Set<TurSNSiteLocale> entities);

    @Mapping(target = "id", ignore = true)
    void updateEntity(TurSNSiteLocale source, @MappingTarget TurSNSiteLocale target);
}