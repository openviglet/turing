package com.viglet.turing.persistence.model.nlp;

import java.io.Serializable;
import javax.persistence.*;

/**
 * The persistent class for the vigServicesNLPEntities database table.
 * 
 */
@Entity
@Table(name = "turNLPInstanceEntity")
@NamedQuery(name = "TurNLPInstanceEntity.findAll", query = "SELECT ne FROM TurNLPInstanceEntity ne")
public class TurNLPInstanceEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(unique = true, nullable = false)
	private int id;

	@Column(nullable = false)
	private int enabled;

	@Column(nullable = false, length = 255)
	private String name;

	// bi-directional many-to-one association to VigEntity
	@ManyToOne(cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "entity_id", nullable = false)
	private TurEntity turEntity;

	@ManyToOne(cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nlp_instance_id", nullable = false)
	private TurNLPInstance turNLPInstance;

	public TurNLPInstanceEntity() {
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getEnabled() {
		return this.enabled;
	}

	public void setEnabled(int enabled) {
		this.enabled = enabled;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TurEntity getTurEntity() {
		return this.turEntity;
	}

	public void setTurEntity(TurEntity turEntity) {
		this.turEntity = turEntity;
	}

	public TurNLPInstance getTurNLPInstance() {
		return this.turNLPInstance;
	}

	public void setTurNLPInstance(TurNLPInstance turNLPInstance) {
		this.turNLPInstance = turNLPInstance;
	}
}