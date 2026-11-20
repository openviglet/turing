package com.viglet.turing.sn.persistence.repository.field;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.repository.RepositoryContractAssertions;

class TurSNSiteFieldExtFacetRepositoryTest {
    @Test
    void shouldFollowJpaRepositoryContract() {
        RepositoryContractAssertions.assertJpaRepositoryContract(TurSNSiteFieldExtFacetRepository.class);
    }
}
