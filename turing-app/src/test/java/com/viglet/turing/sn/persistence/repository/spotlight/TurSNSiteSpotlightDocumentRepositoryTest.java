package com.viglet.turing.sn.persistence.repository.spotlight;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurSNSiteSpotlightDocumentRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSNSiteSpotlightDocumentRepository.class);
    }
}
