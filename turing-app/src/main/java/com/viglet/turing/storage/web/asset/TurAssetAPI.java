package com.viglet.turing.storage.web.asset;

import com.viglet.turing.storage.asset.TurAssetItem;

import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.viglet.turing.storage.asset.TurAssetEvent;
import com.viglet.turing.storage.TurStorageObjectStat;
import com.viglet.turing.storage.TurStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.viglet.core.tenancy.VigletTenantContext;

/**
 * REST API for managing assets stored in MinIO.
 *
 * <p><b>Authorization (T645 / §XXXVII.7):</b> every endpoint takes a
 * caller-supplied {@code objectName} and reads/deletes the underlying storage
 * object, so the whole controller requires {@code ROLE_ADMIN} — it was
 * previously unauthenticated, unlike the sibling {@code TurStoreInstanceAPI}.
 * Path containment for the object key is enforced by
 * {@code TurTenantScopedStorageService}.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.14
 */
@RestController
@RequestMapping("/api/asset")
@Secured("ROLE_ADMIN")
@Tag(name = "Assets", description = "Asset Management API (MinIO)")
public class TurAssetAPI {
	private final TurStorageService storageService;
	private final org.springframework.context.ApplicationEventPublisher eventPublisher;
	private final VigletTenantContext tenantContext;

	/**
	 * T1322 — AI training from assets is rag's {@code TurAssetTrainingAPI}, on the same paths.
	 * T1324 — an upload or delete publishes its {@link TurAssetEvent} inside a transaction,
	 * so the event publication registry stores it and delivers it again if the node dies
	 * before rag has indexed the asset.
	 */
	public TurAssetAPI(TurStorageService storageService,
			org.springframework.context.ApplicationEventPublisher eventPublisher,
			VigletTenantContext tenantContext) {
		this.storageService = storageService;
		this.eventPublisher = eventPublisher;
		this.tenantContext = tenantContext;
	}

	@Operation(summary = "List assets in a folder")
	@GetMapping
	public List<TurAssetItem> list(@RequestParam(defaultValue = "") String prefix) {
		return storageService.listObjects(prefix);
	}

	@Operation(summary = "Download an asset")
	@GetMapping("/download")
	public ResponseEntity<InputStreamResource> download(@RequestParam String objectName) {
		TurStorageObjectStat stat = storageService.statObject(objectName);
		InputStream stream = storageService.downloadObject(objectName);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + extractFileName(objectName) + "\"")
				.contentType(MediaType.parseMediaType(stat.contentType()))
				.contentLength(stat.size())
				.body(new InputStreamResource(stream));
	}

	@Operation(summary = "Preview an asset inline (no download header)")
	@GetMapping("/preview")
	public ResponseEntity<InputStreamResource> preview(@RequestParam String objectName) {
		TurStorageObjectStat stat = storageService.statObject(objectName);
		InputStream stream = storageService.downloadObject(objectName);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + extractFileName(objectName) + "\"")
				.header("X-Frame-Options", "ALLOWALL")
				.header("Content-Security-Policy", "frame-ancestors *")
				.contentType(MediaType.parseMediaType(stat.contentType()))
				.contentLength(stat.size())
				.body(new InputStreamResource(stream));
	}

	@Operation(summary = "Get asset metadata")
	@GetMapping("/metadata")
	public TurAssetItem metadata(@RequestParam String objectName) {
		TurStorageObjectStat stat = storageService.statObject(objectName);
		return new TurAssetItem(
				objectName,
				stat.size(),
				stat.contentType(),
				stat.lastModified() != null ? stat.lastModified() : "",
				false);
	}

	@Operation(summary = "Upload assets")
	@Transactional
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> upload(@RequestParam("files") List<MultipartFile> files,
			@RequestParam(defaultValue = "") String prefix) {
		for (MultipartFile file : files) {
			storageService.uploadObject(file, prefix);
			String objectName = (prefix != null && !prefix.isBlank() ? prefix : "") + file.getOriginalFilename();
			String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
			eventPublisher.publishEvent(TurAssetEvent.uploaded(objectName, contentType, file.getSize(),
					tenantContext.getCurrentTenant()));
		}
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Create a folder")
	@PostMapping("/folder")
	public ResponseEntity<Void> createFolder(@RequestParam String path) {
		storageService.createFolder(path);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Delete an asset")
	@Transactional
	@DeleteMapping
	public ResponseEntity<Void> delete(@RequestParam String objectName) {
		storageService.deleteObject(objectName);
		eventPublisher.publishEvent(TurAssetEvent.deleted(objectName, tenantContext.getCurrentTenant()));
		return ResponseEntity.ok().build();
	}

	private String extractFileName(String objectName) {
		int lastSlash = objectName.lastIndexOf('/');
		return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
	}
}
