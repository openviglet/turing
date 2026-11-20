package com.viglet.turing.sn.persistence.mapper.spotlight;

import com.viglet.turing.sn.persistence.dto.spotlight.TurSNSiteSpotlightDocumentDto;
import com.viglet.turing.sn.persistence.model.spotlight.TurSNSiteSpotlightDocument;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteSpotlightDocumentMapper {
    TurSNSiteSpotlightDocumentDto toDto(TurSNSiteSpotlightDocument entity);

    TurSNSiteSpotlightDocument toEntity(TurSNSiteSpotlightDocumentDto dto);

    List<TurSNSiteSpotlightDocumentDto> toDtoList(List<TurSNSiteSpotlightDocument> entities);

    Set<TurSNSiteSpotlightDocumentDto> toDtoSet(Set<TurSNSiteSpotlightDocument> entities);
}