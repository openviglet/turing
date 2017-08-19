package com.viglet.turing.persistence.service.nlp;

import java.util.List;

import javax.persistence.TypedQuery;

import com.viglet.turing.persistence.model.nlp.TurNLPVendor;
import com.viglet.turing.persistence.service.TurBaseService;

public class TurNLPVendorService extends TurBaseService {
	public void save(TurNLPVendor turNLPVendor) {
		em.getTransaction().begin();
		em.persist(turNLPVendor);
		em.getTransaction().commit();
	}

	public List<TurNLPVendor> listAll() {
		TypedQuery<TurNLPVendor> q = em.createNamedQuery("TurNLPVendor.findAll", TurNLPVendor.class);
		return q.getResultList();
	}

	public TurNLPVendor get(String nlpVendorId) {
		return em.find(TurNLPVendor.class, nlpVendorId);
	}

	public boolean delete(String nlpVendorId) {
		TurNLPVendor turNLPVendor = em.find(TurNLPVendor.class, nlpVendorId);
		em.getTransaction().begin();
		em.remove(turNLPVendor);
		em.getTransaction().commit();
		return true;
	}
	
}