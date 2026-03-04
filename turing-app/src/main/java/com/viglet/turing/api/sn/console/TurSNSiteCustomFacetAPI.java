package com.viglet.turing.api.sn.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.api.sn.bean.TurSNSiteCustomFacetResponse;
import com.viglet.turing.persistence.model.sn.facet.TurSNSiteCustomFacetModel;
import com.viglet.turing.persistence.model.sn.facet.TurSNSiteCustomFacetParentModel;
import com.viglet.turing.persistence.repository.sn.facet.TurSNSiteCustomFacetParentRepository;
import com.viglet.turing.persistence.repository.sn.facet.TurSNSiteCustomFacetRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/sn/custom-facet")
@Tag(name = "Semantic Navigation Custom Facet", description = "Semantic Navigation Custom Facet API")
public class TurSNSiteCustomFacetAPI {

    private final TurSNSiteCustomFacetRepository repository;
    private final TurSNSiteCustomFacetParentRepository parentRepository;

    public TurSNSiteCustomFacetAPI(TurSNSiteCustomFacetRepository repository,
            TurSNSiteCustomFacetParentRepository parentRepository) {
        this.repository = repository;
        this.parentRepository = parentRepository;
    }

    @GetMapping("/all")
    @Operation(summary = "Show all Semantic Navigation Site Custom Facets")
    public List<TurSNSiteCustomFacetModel> allAlias() {
        return repository.findAll();
    }

    @GetMapping("/custom/all")
    @Operation(summary = "Show all Semantic Navigation Site Custom Facets (grouped)")
    public List<TurSNSiteCustomFacetResponse> allCustom() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(m -> m.getParent() != null ? m.getParent().getIdName() : ""))
                .entrySet().stream()
                .map(entry -> {
                    List<Map<String, String>> items = new ArrayList<>();
                    entry.getValue().forEach(model -> {
                        String rangeStart = (model.getRangeStart() == null || model.getRangeStart().isEmpty()) ? "*"
                                : model.getRangeStart();
                        String rangeEnd = (model.getRangeEnd() == null || model.getRangeEnd().isEmpty()) ? "*"
                                : model.getRangeEnd();
                        items.add(Map.of(
                                "label", model.getLabel(),
                                "range", String.format("[%s TO %s]", rangeStart, rangeEnd)));
                    });
                    return TurSNSiteCustomFacetResponse.builder()
                            .label(entry.getKey())
                            .attribute(entry.getKey())
                            .facetItems(items)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a Semantic Navigation Site Custom Facet by id")
    public ResponseEntity<TurSNSiteCustomFacetModel> get(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-label/{label}")
    @Operation(summary = "Get a Semantic Navigation Site Custom Facet by Label")
    public ResponseEntity<TurSNSiteCustomFacetModel> getByLabel(@PathVariable String label) {
        return repository.findByLabel(label)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a Semantic Navigation Site Custom Facet")
    public ResponseEntity<TurSNSiteCustomFacetModel> create(@RequestBody TurSNSiteCustomFacetModel payload,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String parentIdName) {
        payload.setId(null);
        if (parentIdName != null && !parentIdName.isBlank()) {
            parentRepository.findByIdName(parentIdName).ifPresent(payload::setParent);
        }
        TurSNSiteCustomFacetModel saved = repository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Semantic Navigation Site Custom Facet")
    public ResponseEntity<TurSNSiteCustomFacetModel> update(@PathVariable String id,
            @RequestBody TurSNSiteCustomFacetModel payload,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String parentIdName) {
        Optional<TurSNSiteCustomFacetModel> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TurSNSiteCustomFacetModel existing = existingOpt.get();
        existing.setLabel(payload.getLabel());
        existing.setRangeStart(payload.getRangeStart());
        existing.setRangeEnd(payload.getRangeEnd());
        if (parentIdName != null && !parentIdName.isBlank()) {
            parentRepository.findByIdName(parentIdName).ifPresent(existing::setParent);
        }
        TurSNSiteCustomFacetModel saved = repository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/by-label/{label}")
    @Operation(summary = "Update a Semantic Navigation Site Custom Facet by Label")
    public ResponseEntity<TurSNSiteCustomFacetModel> updateByLabel(@PathVariable String label,
            @RequestBody TurSNSiteCustomFacetModel payload,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String parentIdName) {
        Optional<TurSNSiteCustomFacetModel> existingOpt = repository.findByLabel(label);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TurSNSiteCustomFacetModel existing = existingOpt.get();
        existing.setLabel(payload.getLabel());
        existing.setRangeStart(payload.getRangeStart());
        existing.setRangeEnd(payload.getRangeEnd());
        if (parentIdName != null && !parentIdName.isBlank()) {
            parentRepository.findByIdName(parentIdName).ifPresent(existing::setParent);
        }
        TurSNSiteCustomFacetModel saved = repository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Semantic Navigation Site Custom Facet")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/by-label/{label}")
    @Operation(summary = "Delete a Semantic Navigation Site Custom Facet by Label")
    public ResponseEntity<Void> deleteByLabel(@PathVariable String label) {
        Optional<TurSNSiteCustomFacetModel> existingOpt = repository.findByLabel(label);
        if (existingOpt.isPresent()) {
            repository.delete(existingOpt.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/parent")
    @Operation(summary = "List all Custom Facet Parents")
    public List<TurSNSiteCustomFacetParentModel> listParents() {
        return parentRepository.findAll();
    }

    @GetMapping("/parent/{idName}")
    @Operation(summary = "Get Custom Facet Parent by NameID")
    public ResponseEntity<TurSNSiteCustomFacetParentModel> getParent(@PathVariable String idName) {
        return parentRepository.findByIdName(idName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/parent")
    @Operation(summary = "Create Custom Facet Parent")
    public ResponseEntity<TurSNSiteCustomFacetParentModel> createParent(
            @RequestBody TurSNSiteCustomFacetParentModel payload) {
        payload.setId(null);
        if (payload.getIdName() == null || payload.getIdName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (parentRepository.existsByIdName(payload.getIdName())) {
            return ResponseEntity.badRequest().build();
        }
        TurSNSiteCustomFacetParentModel saved = parentRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/parent/{idName}")
    @Operation(summary = "Update Custom Facet Parent by NameID")
    public ResponseEntity<TurSNSiteCustomFacetParentModel> updateParent(@PathVariable String idName,
            @RequestBody TurSNSiteCustomFacetParentModel payload) {
        Optional<TurSNSiteCustomFacetParentModel> existingOpt = parentRepository.findByIdName(idName);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TurSNSiteCustomFacetParentModel existing = existingOpt.get();
        existing.setAttribute(payload.getAttribute());
        existing.setSelection(payload.getSelection());
        TurSNSiteCustomFacetParentModel saved = parentRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/parent/{idName}")
    @Operation(summary = "Delete Custom Facet Parent by NameID")
    public ResponseEntity<Void> deleteParent(@PathVariable String idName) {
        Optional<TurSNSiteCustomFacetParentModel> existingOpt = parentRepository.findByIdName(idName);
        if (existingOpt.isPresent()) {
            parentRepository.delete(existingOpt.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
