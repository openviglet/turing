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
package com.viglet.turing.transcription.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.transcription.TurAudioChunker;
import com.viglet.turing.transcription.TurAudioChunker.TurFfmpegStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * T688 — probes whether the {@code ffmpeg} toolchain is reachable from the
 * running server. Chunking audio above a backend's per-request limit requires
 * ffmpeg; small clips do not. Lets the admin validate the host before relying
 * on large-file transcription. Never sends anything sensitive — only an
 * availability flag, the ffmpeg version banner, and an error string when
 * unavailable.
 *
 * <p>T1322v — moved from system's global-settings controller into transcription
 * with its REST path unchanged, so system does not import transcription (ADR
 * 0005 section 6).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/system/global-settings/transcription/ffmpeg")
@Tag(name = "Global Settings", description = "Global Settings API")
public class TurTranscriptionFfmpegStatusAPI {

    private final TurAudioChunker turAudioChunker;

    public TurTranscriptionFfmpegStatusAPI(TurAudioChunker turAudioChunker) {
        this.turAudioChunker = turAudioChunker;
    }

    @Operation(summary = "Check whether ffmpeg is available for large-audio transcription chunking")
    @GetMapping("/status")
    public TurFfmpegStatus getFfmpegStatus() {
        return turAudioChunker.checkFfmpeg();
    }
}
