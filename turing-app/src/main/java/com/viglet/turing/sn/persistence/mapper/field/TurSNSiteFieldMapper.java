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

    @Mapping(target = "id", ignore = true)
    void updateEntity(TurSNSiteField source, @MappingTarget TurSNSiteField target);
}