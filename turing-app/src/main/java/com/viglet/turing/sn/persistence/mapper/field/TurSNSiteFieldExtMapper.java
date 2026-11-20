package com.viglet.turing.sn.persistence.mapper.field;

import java.util.List;
import java.util.Set;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.viglet.turing.sn.persistence.dto.field.TurSNSiteFieldExtDto;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = TurSNSiteFieldExtFacetMapper.class)
public interface TurSNSiteFieldExtMapper {
    TurSNSiteFieldExtDto toDto(TurSNSiteFieldExt entity);

    @Mapping(target = "customFacets", ignore = true)
    @Mapping(target = "facetSort", ignore = true)
    @Mapping(target = "facetPosition", ignore = true)
    TurSNSiteFieldExt toEntity(TurSNSiteFieldExtDto dto);

    List<TurSNSiteFieldExtDto> toDtoList(List<TurSNSiteFieldExt> entities);

    Set<TurSNSiteFieldExtDto> toDtoSet(Set<TurSNSiteFieldExt> entities);
}