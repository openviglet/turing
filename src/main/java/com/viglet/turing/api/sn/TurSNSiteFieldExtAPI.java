package com.viglet.turing.api.sn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.viglet.turing.persistence.model.nlp.TurNLPEntity;
import com.viglet.turing.persistence.model.nlp.TurNLPInstanceEntity;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.TurSNSiteField;
import com.viglet.turing.persistence.model.sn.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.nlp.TurNLPEntityRepository;
import com.viglet.turing.persistence.repository.nlp.TurNLPInstanceEntityRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteFieldRepository;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.se.field.TurSEFieldType;
import com.viglet.turing.sn.TurSNFieldType;

@Component
@Path("sn/{snSiteId}/field/ext")
public class TurSNSiteFieldExtAPI {

	@Autowired
	TurSNSiteRepository turSNSiteRepository;
	@Autowired
	TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;
	@Autowired
	TurSNSiteFieldRepository turSNSiteFieldRepository;
	@Autowired
	TurNLPInstanceEntityRepository turNLPInstanceEntityRepository;
	@Autowired
	TurNLPEntityRepository turNLPEntityRepository;
	
	@GET
	@Produces("application/json")
	public List<TurSNSiteFieldExt> list(@PathParam("snSiteId") int snSiteId) throws JSONException {
		TurSNSite turSNSite = turSNSiteRepository.findById(snSiteId);

		List<TurSNSiteField> turSNSiteFields = turSNSiteFieldRepository.findByTurSNSite(turSNSite);
		List<TurNLPInstanceEntity> turNLPInstanceEntities = turNLPInstanceEntityRepository
				.findByTurNLPInstanceAndEnabled(turSNSite.getTurNLPInstance(), 1);
		List<TurNLPEntity> turNLPEntityThesaurus = turNLPEntityRepository.findByLocal(1);
		
		Map<Integer, TurSNSiteField> fieldMap = new HashMap<Integer, TurSNSiteField>();
		Map<Integer, TurNLPEntity> nerMap = new HashMap<Integer, TurNLPEntity>();
		Map<Integer, TurNLPEntity> thesaurusMap = new HashMap<Integer, TurNLPEntity>();

		for (TurSNSiteField turSNSiteField : turSNSiteFields) {
			fieldMap.put(turSNSiteField.getId(), turSNSiteField);

		}

		for (TurNLPInstanceEntity turNLPInstanceEntity : turNLPInstanceEntities) {
			TurNLPEntity turNLPEntity = turNLPInstanceEntity.getTurNLPEntity();
				nerMap.put(turNLPEntity.getId(), turNLPEntity);
		}

		for (TurNLPEntity turNLPEntityThesaurusSingle : turNLPEntityThesaurus) {
			thesaurusMap.put(turNLPEntityThesaurusSingle.getId(), turNLPEntityThesaurusSingle);
		}

		
		List<TurSNSiteFieldExt> turSNSiteFieldExts = this.turSNSiteFieldExtRepository.findByTurSNSite(turSNSite);

		for (TurSNSiteFieldExt turSNSiteFieldExt : turSNSiteFieldExts) {
			switch (turSNSiteFieldExt.getSnType()) {
			case SE:
				if (fieldMap.containsKey(turSNSiteFieldExt.getExternalId())) {
					fieldMap.remove(turSNSiteFieldExt.getExternalId());
				}
				break;
			case NER:
				if (nerMap.containsKey(turSNSiteFieldExt.getExternalId())) {
					nerMap.remove(turSNSiteFieldExt.getExternalId());
				}
				break;

			case THESAURUS:
				if (thesaurusMap.containsKey(turSNSiteFieldExt.getExternalId())) {
					thesaurusMap.remove(turSNSiteFieldExt.getExternalId());
				}
				break;
			}
		}

		for (TurSNSiteField turSNSiteField : fieldMap.values()) {
			TurSNSiteFieldExt turSNSiteFieldExt = new TurSNSiteFieldExt();
			turSNSiteFieldExt.setEnabled(0);
			turSNSiteFieldExt.setName(turSNSiteField.getName());
			turSNSiteFieldExt.setFacet(0);
			turSNSiteFieldExt.setFacetName(turSNSiteField.getName());
			turSNSiteFieldExt.setHl(0);
			turSNSiteFieldExt.setMultiValued(turSNSiteField.getMultiValued());
			turSNSiteFieldExt.setMlt(0);
			turSNSiteFieldExt.setExternalId(turSNSiteField.getId());
			turSNSiteFieldExt.setSnType(TurSNFieldType.SE);
			turSNSiteFieldExt.setType(turSNSiteField.getType());
			turSNSiteFieldExt.setTurSNSite(turSNSite);
			turSNSiteFieldExtRepository.save(turSNSiteFieldExt);

			turSNSiteFieldExts.add(turSNSiteFieldExt);
		}

		for (TurNLPEntity turNLPEntity : nerMap.values()) {
			TurSNSiteFieldExt turSNSiteFieldExt = new TurSNSiteFieldExt();
			turSNSiteFieldExt.setEnabled(0);
			turSNSiteFieldExt.setName(turNLPEntity.getInternalName());
			turSNSiteFieldExt.setFacet(0);
			turSNSiteFieldExt.setFacetName(turNLPEntity.getName());
			turSNSiteFieldExt.setHl(0);
			turSNSiteFieldExt.setMultiValued(1);
			turSNSiteFieldExt.setMlt(0);
			turSNSiteFieldExt.setExternalId(turNLPEntity.getId());
			turSNSiteFieldExt.setSnType(TurSNFieldType.NER);
			turSNSiteFieldExt.setType(TurSEFieldType.STRING);
			turSNSiteFieldExt.setTurSNSite(turSNSite);
			turSNSiteFieldExtRepository.save(turSNSiteFieldExt);

			turSNSiteFieldExts.add(turSNSiteFieldExt);
		}

		for (TurNLPEntity turNLPEntity : thesaurusMap.values()) {
			TurSNSiteFieldExt turSNSiteFieldExt = new TurSNSiteFieldExt();
			turSNSiteFieldExt.setEnabled(0);
			turSNSiteFieldExt.setName(turNLPEntity.getInternalName());
			turSNSiteFieldExt.setFacet(0);
			turSNSiteFieldExt.setFacetName(turNLPEntity.getName());
			turSNSiteFieldExt.setHl(0);
			turSNSiteFieldExt.setMultiValued(1);
			turSNSiteFieldExt.setMlt(0);
			turSNSiteFieldExt.setExternalId(turNLPEntity.getId());
			turSNSiteFieldExt.setSnType(TurSNFieldType.THESAURUS);
			turSNSiteFieldExt.setType(TurSEFieldType.STRING);
			turSNSiteFieldExt.setTurSNSite(turSNSite);
			turSNSiteFieldExtRepository.save(turSNSiteFieldExt);

			turSNSiteFieldExts.add(turSNSiteFieldExt);
		}

		return this.turSNSiteFieldExtRepository.findByTurSNSite(turSNSite);
	}

	@Path("{snSiteFieldId}")
	@GET
	@Produces("application/json")
	public TurSNSiteFieldExt mlSolution(@PathParam("snSiteId") int snSiteId, @PathParam("snSiteFieldId") int id)
			throws JSONException {
		return this.turSNSiteFieldExtRepository.findById(id);
	}

	@Path("/{snSiteFieldId}")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public TurSNSiteFieldExt update(@PathParam("snSiteId") int snSiteId, @PathParam("snSiteFieldId") int id,
			TurSNSiteFieldExt turSNSiteFieldExt) throws Exception {
		TurSNSiteFieldExt turSNSiteFieldExtEdit = this.turSNSiteFieldExtRepository.findById(id);
		turSNSiteFieldExtEdit.setFacetName(turSNSiteFieldExt.getFacetName());
		turSNSiteFieldExtEdit.setMultiValued(turSNSiteFieldExt.getMultiValued());
		turSNSiteFieldExtEdit.setName(turSNSiteFieldExt.getName());
		turSNSiteFieldExtEdit.setType(turSNSiteFieldExt.getType());
		turSNSiteFieldExtEdit.setFacet(turSNSiteFieldExt.getFacet());
		turSNSiteFieldExtEdit.setHl(turSNSiteFieldExt.getHl());
		turSNSiteFieldExtEdit.setEnabled(turSNSiteFieldExt.getEnabled());
		turSNSiteFieldExtEdit.setMlt(turSNSiteFieldExt.getMlt());
		turSNSiteFieldExtEdit.setExternalId(turSNSiteFieldExt.getExternalId());
		turSNSiteFieldExtEdit.setSnType(turSNSiteFieldExt.getSnType());
		this.turSNSiteFieldExtRepository.save(turSNSiteFieldExtEdit);
		return turSNSiteFieldExtEdit;
	}

	@Path("{snSiteFieldId}")
	@DELETE
	@Produces("application/json")
	public boolean deleteEntity(@PathParam("snSiteId") int snSiteId, @PathParam("snSiteFieldId") int id) {
		this.turSNSiteFieldExtRepository.delete(id);
		return true;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public TurSNSiteFieldExt add(@PathParam("snSiteId") int snSiteId, TurSNSiteFieldExt turSNSiteFieldExt)
			throws Exception {
		TurSNSite turSNSite = turSNSiteRepository.findById(snSiteId);
		turSNSiteFieldExt.setTurSNSite(turSNSite);
		this.turSNSiteFieldExtRepository.save(turSNSiteFieldExt);
		return turSNSiteFieldExt;

	}
}