package com.viglet.turing.sn.persistence.mapper.merge;

import com.viglet.turing.sn.persistence.dto.merge.TurSNSiteMergeProvidersDto;
import com.viglet.turing.sn.persistence.model.merge.TurSNSiteMergeProviders;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNSiteMergeProvidersMapper {
    TurSNSiteMergeProvidersDto toDto(TurSNSiteMergeProviders entity);

    TurSNSiteMergeProviders toEntity(TurSNSiteMergeProvidersDto dto);

    List<TurSNSiteMergeProvidersDto> toDtoList(List<TurSNSiteMergeProviders> entities);

    Set<TurSNSiteMergeProvidersDto> toDtoSet(Set<TurSNSiteMergeProviders> entities);
}