package com.viglet.turing.sn.persistence.mapper.field;

import com.viglet.turing.sn.persistence.dto.field.TurSNSiteFieldDto;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteField;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteFieldMapper {
    TurSNSiteFieldDto toDto(TurSNSiteField entity);

    TurSNSiteField toEntity(TurSNSiteFieldDto dto);

    List<TurSNSiteFieldDto> toDtoList(List<TurSNSiteField> entities);

    Set<TurSNSiteFieldDto> toDtoSet(Set<TurSNSiteField> entities);

    /**
     * T1355 — the site is not the payload's to change: an update keeps the field's
     * own, managed site, and the payload's (an id with no lock version, read as
     * unsaved since T1319) never reaches the entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "turSNSite", ignore = true)
    void updateEntity(TurSNSiteField source, @MappingTarget TurSNSiteField target);
}