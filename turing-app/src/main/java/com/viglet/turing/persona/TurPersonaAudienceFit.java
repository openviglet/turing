/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.persona;

import java.util.Optional;
import java.util.OptionalDouble;

import org.springframework.stereotype.Component;

import com.viglet.turing.persona.persistence.model.TurPersona;
import com.viglet.turing.persona.persistence.repository.TurPersonaRepository;
import com.viglet.turing.persona.readability.TurReadabilityScorer;
import com.viglet.turing.sn.api.TurSNAudienceFit;

/**
 * T1322h — answers sn's content-fit questions from the persona module: whether
 * a persona is an audience, and the {@link TurReadabilityScorer} fit of a text
 * for it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurPersonaAudienceFit implements TurSNAudienceFit {

    private final TurPersonaRepository personaRepository;
    private final TurReadabilityScorer readabilityScorer;

    public TurPersonaAudienceFit(TurPersonaRepository personaRepository, TurReadabilityScorer readabilityScorer) {
        this.personaRepository = personaRepository;
        this.readabilityScorer = readabilityScorer;
    }

    @Override
    public boolean isAudience(String personaId) {
        return audience(personaId).isPresent();
    }

    @Override
    public OptionalDouble fitScore(String personaId, String text) {
        return audience(personaId)
                .map(persona -> OptionalDouble.of(readabilityScorer.score(text, persona.getAudience()).fitScore()))
                .orElse(OptionalDouble.empty());
    }

    @Override
    public Optional<String> personaName(String personaId) {
        return personaId == null ? Optional.empty() : personaRepository.findById(personaId).map(TurPersona::getName);
    }

    private Optional<TurPersona> audience(String personaId) {
        return personaId == null ? Optional.empty()
                : personaRepository.findById(personaId).filter(TurPersona::isUsableAsAudience);
    }
}
