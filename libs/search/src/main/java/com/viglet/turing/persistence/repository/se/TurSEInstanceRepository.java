package com.viglet.turing.persistence.repository.se;

import com.viglet.turing.persistence.model.se.TurSEInstance;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TurSEInstanceRepository extends JpaRepository<TurSEInstance, Integer> {

	@Cacheable("turSEInstancefindAll")
	List<TurSEInstance> findAll();

	@Cacheable("turSEInstancefindById")
	TurSEInstance findById(int id);

	@CacheEvict(value = { "turSEInstancefindAll", "turSEInstancefindById" }, allEntries = true)
	TurSEInstance save(TurSEInstance turSEInstance);

	@Modifying
	@Query("delete from  TurSEInstance si where si.id = ?1")
	@CacheEvict(value = { "turSEInstancefindAll", "turSEInstancefindById" }, allEntries = true)
	void delete(int id);
}