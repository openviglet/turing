package com.viglet.turing.persistence.service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class TurBaseService {

	EntityManagerFactory factory = Persistence.createEntityManagerFactory("turing-app");
	protected EntityManager em = factory.createEntityManager();

}