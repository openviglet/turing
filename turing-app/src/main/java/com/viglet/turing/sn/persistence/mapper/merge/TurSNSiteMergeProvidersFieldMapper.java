package com.viglet.turing.sn.persistence.mapper.merge;

import com.viglet.turing.sn.persistence.dto.merge.TurSNSiteMergeProvidersFieldDto;
import com.viglet.turing.sn.persistence.model.merge.TurSNSiteMergeProvidersField;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteMergeProvidersFieldMapper {
    TurSNSiteMergeProvidersFieldDto toDto(TurSNSiteMergeProvidersField entity);

    TurSNSiteMergeProvidersField toEntity(TurSNSiteMergeProvidersFieldDto dto);

    List<TurSNSiteMergeProvidersFieldDto> toDtoList(List<TurSNSiteMergeProvidersField> entities);

    Set<TurSNSiteMergeProvidersFieldDto> toDtoSet(Set<TurSNSiteMergeProvidersField> entities);
}