package com.viglet.turing.sn.persistence.mapper.spotlight;

import com.viglet.turing.sn.persistence.dto.spotlight.TurSNSiteSpotlightTermDto;
import com.viglet.turing.sn.persistence.model.spotlight.TurSNSiteSpotlightTerm;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteSpotlightTermMapper {
    TurSNSiteSpotlightTermDto toDto(TurSNSiteSpotlightTerm entity);

    TurSNSiteSpotlightTerm toEntity(TurSNSiteSpotlightTermDto dto);

    List<TurSNSiteSpotlightTermDto> toDtoList(List<TurSNSiteSpotlightTerm> entities);

    Set<TurSNSiteSpotlightTermDto> toDtoSet(Set<TurSNSiteSpotlightTerm> entities);
}