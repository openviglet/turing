// TurSNSiteExport.java
package com.viglet.turing.exchange.sn;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.exchange.TurExchange;
import com.viglet.turing.exchange.sn.mixin.TurSNSiteExchangeMixin;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import com.viglet.turing.persistence.model.sn.merge.TurSNSiteMergeProviders;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingCondition;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingExpression;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlight;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class TurSNSiteExportFileService {

	private static final String TMP_DIR = "store/tmp";
	private static final String EXPORT_JSON = "export.json";
	private static final String ZIP_EXTENSION = ".zip";

	public File getOrCreateTmpDir() {
		File userDir = Paths.get(System.getProperty("user.dir")).toFile();
		File tmpDir = Paths.get(userDir.getAbsolutePath(), TMP_DIR).toFile();
		try {
			Files.createDirectories(tmpDir.toPath());
		} catch (IOException e) {
			log.error("Could not create temp directory: {}", e.getMessage(), e);
			throw new IllegalStateException("Temp directory creation failed", e);
		}
		return tmpDir;
	}

	public File prepareExportFile(File tmpDir, String folderName, TurExchange turExchange) {
		File exportDir = Paths.get(tmpDir.getAbsolutePath(), folderName).toFile();
		File exportFile = new File(exportDir, EXPORT_JSON);
		try {
			// Ensure export directory exists
			if (!exportDir.exists() && !exportDir.mkdirs()) {
				throw new IOException("Failed to create export directory: " + exportDir.getAbsolutePath());
			}
			// Configure JsonMapper with mixins and pretty printing
			JsonMapper mapper = JsonMapper.builder()
					.configure(SerializationFeature.INDENT_OUTPUT, true)
					.addMixIn(TurSNRankingCondition.class, TurSNSiteExchangeMixin.class)
					.addMixIn(TurSNRankingExpression.class, TurSNSiteExchangeMixin.class)
					.addMixIn(TurSNSiteLocale.class, TurSNSiteExchangeMixin.class)
					.addMixIn(TurSNSiteSpotlight.class, TurSNSiteExchangeMixin.class)
					.addMixIn(TurSNSiteMergeProviders.class, TurSNSiteExchangeMixin.class)
					.build();
			// Write export file atomically
			Path tempExportFile = Files.createTempFile(exportDir.toPath(), "export-", ".json");
			mapper.writer().writeValue(tempExportFile.toFile(), turExchange);
			Files.move(tempExportFile, exportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("Error writing export file: {}", e.getMessage(), e);
			throw new IllegalStateException("Export file creation failed", e);
		}
		return exportFile;
	}

	public File createZipFile(File exportFile) {
		File zipFile = new File(exportFile.getParent() + ZIP_EXTENSION);
		try {
			TurCommonsUtils.addFilesToZip(exportFile.getParentFile(), zipFile);
		} catch (Exception e) {
			log.error("Error creating zip file: {}", e.getMessage(), e);
			throw new IllegalStateException("Zip file creation failed", e);
		}
		return zipFile;
	}

	public void writeZipToResponseAndCleanup(File zipFile, File exportFile, OutputStream output) {
		try {
			Path path = zipFile.toPath();
			byte[] data = Files.readAllBytes(path);
			output.write(data);
			output.flush();
		} catch (IOException e) {
			log.error("Error writing zip to response: {}", e.getMessage(), e);
		} finally {
			cleanup(exportFile, zipFile);
		}
	}

	private void cleanup(File exportFile, File zipFile) {
		try {
			FileUtils.deleteDirectory(exportFile.getParentFile());
			FileUtils.deleteQuietly(zipFile);
		} catch (Exception e) {
			log.warn("Cleanup failed: {}", e.getMessage(), e);
		}
	}

	public TurSNSiteExchange exportSNSite(TurSNSite turSNSite) {
		return new TurSNSiteExchange(turSNSite);
	}

	public Path exportSNSitesToZip(List<TurSNSite> turSNSites) {
		File tmpDir = getOrCreateTmpDir();
		String folderName = "SNSite_" + System.currentTimeMillis();

		List<TurSNSiteExchange> shSiteExchanges = new ArrayList<>();

		for (TurSNSite turSNSite : turSNSites) {
			shSiteExchanges.add(this.exportSNSite(turSNSite));
		}
		TurExchange turExchange = new TurExchange();
		if (!shSiteExchanges.isEmpty()) {
			turExchange.setSnSites(shSiteExchanges);
		}
		File exportFile = prepareExportFile(tmpDir, folderName, turExchange);
		File zipFile = createZipFile(exportFile);
		return zipFile.toPath();
	}
}
