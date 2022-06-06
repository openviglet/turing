/*
 * Copyright (C) 2016-2022 the original author or authors. 
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.utils;

import com.viglet.turing.commons.utils.TurCommonsUtils;
import com.viglet.turing.spring.security.auth.ITurAuthenticationFacade;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

@Component
public class TurUtils {
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	@Autowired
	private ITurAuthenticationFacade authenticationFacade;

	private static File getTempDirectory() {
		return TurCommonsUtils.addSubDirToStoreDir("tmp");
	}

	public static File getFileFromMultipart(MultipartFile file) {
		File localFile = new File(
				randomTempFileOrDirectory());

		try {
			file.transferTo(localFile);
		} catch (IllegalStateException | IOException e) {
			logger.error(e.getMessage(), e);
		}

		return localFile;
	}

	public static File extractZipFile(MultipartFile file) {

		File zipFile = getFileFromMultipart(file);

		File extractFolder = new File(
				randomTempFileOrDirectory());
		try {
			TurCommonsUtils.unZipIt(zipFile, new File(
					randomTempFileOrDirectory()));
			FileUtils.deleteQuietly(zipFile);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return extractFolder;
	}

	private static String randomTempFileOrDirectory() {
		return getTempDirectory().getAbsolutePath().concat(File.separator + "imp_" + UUID.randomUUID());
	}

	public String getCurrentUsername() {
		Authentication authentication = authenticationFacade.getAuthentication();
		return authentication.getName();
	}
}
