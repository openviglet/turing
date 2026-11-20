package com.viglet.turing.sn.persistence.mapper.ranking;

import com.viglet.turing.sn.persistence.dto.ranking.TurSNRankingConditionDto;
import com.viglet.turing.sn.persistence.model.ranking.TurSNRankingCondition;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TurSNRankingConditionMapper {
    TurSNRankingConditionDto toDto(TurSNRankingCondition entity);

    TurSNRankingCondition toEntity(TurSNRankingConditionDto dto);

    List<TurSNRankingConditionDto> toDtoList(List<TurSNRankingCondition> entities);

    Set<TurSNRankingConditionDto> toDtoSet(Set<TurSNRankingCondition> entities);
}