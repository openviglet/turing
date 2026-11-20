package com.viglet.turing.kb.persistence.repository;

import org.junit.jupiter.api.Test;

import com.viglet.turing.db.repository.RepositoryContractAssertions;

class TurKnowledgeBaseRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurKnowledgeBaseRepository.class);
    }
}
