package com.viglet.turing.sn.persistence.mapper.spotlight;

import com.viglet.turing.sn.persistence.dto.spotlight.TurSNSiteSpotlightDto;
import com.viglet.turing.sn.persistence.model.spotlight.TurSNSiteSpotlight;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteSpotlightMapper {
    TurSNSiteSpotlightDto toDto(TurSNSiteSpotlight entity);

    TurSNSiteSpotlight toEntity(TurSNSiteSpotlightDto dto);

    List<TurSNSiteSpotlightDto> toDtoList(List<TurSNSiteSpotlight> entities);

    Set<TurSNSiteSpotlightDto> toDtoSet(Set<TurSNSiteSpotlight> entities);
}