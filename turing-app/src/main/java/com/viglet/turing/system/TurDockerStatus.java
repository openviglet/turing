/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.system;

/**
 * Whether the Docker daemon the code interpreter runs in answers, and its
 * version or the error that stopped it.
 *
 * @param available     whether the daemon answered
 * @param serverVersion the daemon's version; {@code null} when it did not answer
 * @param error         why it did not answer; {@code null} when it did
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public record TurDockerStatus(boolean available, String serverVersion, String error) {
}
