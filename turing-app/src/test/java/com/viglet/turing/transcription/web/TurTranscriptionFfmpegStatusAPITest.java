/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.transcription.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.viglet.turing.transcription.TurAudioChunker;
import com.viglet.turing.transcription.TurAudioChunker.TurFfmpegStatus;

/**
 * T1322v — the ffmpeg probe moved from system into transcription; its path and
 * its answer are what the console relies on, so both are pinned.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurTranscriptionFfmpegStatusAPITest {

    @Test
    void returnsTheChunkersProbe() {
        TurAudioChunker chunker = mock(TurAudioChunker.class);
        TurFfmpegStatus status = new TurFfmpegStatus(true, "ffmpeg version 7.1", null);
        when(chunker.checkFfmpeg()).thenReturn(status);

        assertThat(new TurTranscriptionFfmpegStatusAPI(chunker).getFfmpegStatus()).isSameAs(status);
    }

    @Test
    void keepsTheGlobalSettingsPath() throws NoSuchMethodException {
        String base = TurTranscriptionFfmpegStatusAPI.class.getAnnotation(RequestMapping.class).value()[0];
        String leaf = TurTranscriptionFfmpegStatusAPI.class.getMethod("getFfmpegStatus")
                .getAnnotation(GetMapping.class).value()[0];

        assertThat(base + leaf).isEqualTo("/api/system/global-settings/transcription/ffmpeg/status");
    }
}
