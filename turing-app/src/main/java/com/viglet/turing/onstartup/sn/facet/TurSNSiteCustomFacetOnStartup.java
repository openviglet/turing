package com.viglet.turing.onstartup.sn.facet;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.persistence.model.sn.facet.TurSNSiteCustomFacetModel;
import com.viglet.turing.persistence.model.sn.facet.TurSNSiteCustomFacetParentModel;
import com.viglet.turing.persistence.repository.sn.facet.TurSNSiteCustomFacetParentRepository;
import com.viglet.turing.persistence.repository.sn.facet.TurSNSiteCustomFacetRepository;

@Component
@Transactional
public class TurSNSiteCustomFacetOnStartup {

	private final TurSNSiteCustomFacetRepository turSNSiteCustomFacetRepository;
	private final TurSNSiteCustomFacetParentRepository turSNSiteCustomFacetParentRepository;

	public TurSNSiteCustomFacetOnStartup(
			TurSNSiteCustomFacetRepository turSNSiteCustomFacetRepository,
			TurSNSiteCustomFacetParentRepository turSNSiteCustomFacetParentRepository) {
		this.turSNSiteCustomFacetRepository = turSNSiteCustomFacetRepository;
		this.turSNSiteCustomFacetParentRepository = turSNSiteCustomFacetParentRepository;
	}

	public void createDefaultRows() {

		if (turSNSiteCustomFacetParentRepository.findAll().isEmpty()) {
			TurSNSiteCustomFacetParentModel parent = new TurSNSiteCustomFacetParentModel();
			parent.setIdName("price");
			parent.setAttribute("price");
			parent.setSelection("price");
			parent = turSNSiteCustomFacetParentRepository.save(parent);

			TurSNSiteCustomFacetModel facet1 = new TurSNSiteCustomFacetModel();
			facet1.setLabel("< 2000");
			facet1.setRangeEnd("2000");
			facet1.setParent(parent);
			turSNSiteCustomFacetRepository.save(facet1);

			TurSNSiteCustomFacetModel facet2 = new TurSNSiteCustomFacetModel();
			facet2.setLabel("2000 - 5000");
			facet2.setRangeStart("2000");
			facet2.setRangeEnd("5000");
			facet2.setParent(parent);
			turSNSiteCustomFacetRepository.save(facet2);

			TurSNSiteCustomFacetModel facet3 = new TurSNSiteCustomFacetModel();
			facet3.setLabel("> 5000");
			facet3.setRangeStart("5000");
			facet3.setParent(parent);
			turSNSiteCustomFacetRepository.save(facet3);
		}
	}
}
