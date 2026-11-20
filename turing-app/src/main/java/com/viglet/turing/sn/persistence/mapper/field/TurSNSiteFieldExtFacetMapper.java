package com.viglet.turing.sn.persistence.mapper.field;

import java.util.List;
import java.util.Set;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.viglet.turing.sn.persistence.dto.field.TurSNSiteFieldExtFacetDto;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExtFacet;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteFieldExtFacetMapper {
    TurSNSiteFieldExtFacetDto toDto(TurSNSiteFieldExtFacet entity);

    @Mapping(target = "turSNSiteFieldExt", ignore = true)
    TurSNSiteFieldExtFacet toEntity(TurSNSiteFieldExtFacetDto dto);

    List<TurSNSiteFieldExtFacetDto> toDtoList(List<TurSNSiteFieldExtFacet> entities);

    Set<TurSNSiteFieldExtFacetDto> toDtoSet(Set<TurSNSiteFieldExtFacet> entities);
}