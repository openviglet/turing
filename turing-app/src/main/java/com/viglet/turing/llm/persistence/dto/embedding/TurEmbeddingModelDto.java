package com.viglet.turing.llm.persistence.dto.embedding;

import com.viglet.turing.llm.persistence.model.embedding.TurEmbeddingModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for Embedding Model.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TurEmbeddingModelDto extends TurEmbeddingModel {
}
