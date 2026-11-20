package com.viglet.turing.auth.persistence.mapper.devtoken;

import com.viglet.turing.auth.persistence.dto.devtoken.TurDevTokenDto;
import com.viglet.turing.auth.persistence.model.devtoken.TurDevToken;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurDevTokenMapper {
    TurDevTokenDto toDto(TurDevToken entity);

    TurDevToken toEntity(TurDevTokenDto dto);

    List<TurDevTokenDto> toDtoList(List<TurDevToken> entities);

    Set<TurDevTokenDto> toDtoSet(Set<TurDevToken> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "token", ignore = true)
    void updateEntity(TurDevToken source, @MappingTarget TurDevToken target);
}