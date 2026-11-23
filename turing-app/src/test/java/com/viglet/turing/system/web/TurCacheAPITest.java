package com.viglet.turing.system.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.viglet.turing.shared.exception.TurNotFoundException;
import com.viglet.turing.system.cache.TurReloadableCache;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TurCacheAPITest {

    private MockMvc mockMvc;

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cacheA;
    @Mock
    private Cache cacheB;
    // T942 / §LVIII.7 — the compiled ranking lexicons live in a bean, not in the
    // CacheManager, so clearing "all caches" has to reach them explicitly; since
    // T1322l through the TurReloadableCache SPI, so system imports no sn type.
    @Mock
    private TurReloadableCache rankingLexicons;

    private TurCacheAPI api;

    @BeforeEach
    void setUp() {
        api = new TurCacheAPI(cacheManager, List.of(rankingLexicons));
        mockMvc = MockMvcBuilders.standaloneSetup(api).build();
    }

    @Test
    void shouldListCachesSorted() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of("z-cache", "a-cache", "m-cache"));

        mockMvc.perform(get("/api/system/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalCaches").value(3))
                .andExpect(jsonPath("$.cacheNames[0]").value("a-cache"))
                .andExpect(jsonPath("$.cacheNames[1]").value("m-cache"))
                .andExpect(jsonPath("$.cacheNames[2]").value("z-cache"));
    }

    @Test
    void shouldClearExistingCachesAndIgnoreMissingOnes() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of("b-cache", "a-cache", "missing-cache"));
        when(cacheManager.getCache("a-cache")).thenReturn(cacheA);
        when(cacheManager.getCache("b-cache")).thenReturn(cacheB);
        when(cacheManager.getCache("missing-cache")).thenReturn(null);
        when(rankingLexicons.name()).thenReturn("rankingLexicons");
        when(rankingLexicons.reload()).thenReturn(4);

        mockMvc.perform(post("/api/system/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.clearedCaches").value(3))
                .andExpect(jsonPath("$.rankingLexicons").value(4))
                .andExpect(jsonPath("$.cacheNames[0]").value("a-cache"))
                .andExpect(jsonPath("$.cacheNames[1]").value("b-cache"))
                .andExpect(jsonPath("$.cacheNames[2]").value("missing-cache"));

        verify(cacheA).clear();
        verify(cacheB).clear();
        verify(cacheManager).getCache("missing-cache");
    }

    @Test
    void shouldReturnSuccessWhenThereAreNoCaches() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of());
        when(rankingLexicons.name()).thenReturn("rankingLexicons");

        mockMvc.perform(post("/api/system/cache/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.clearedCaches").value(0));

        verify(cacheManager, never()).getCache(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldReloadTheRankingLexiconsWithoutFlushingEveryOtherCache() throws Exception {
        // The targeted door: an operator who edited a file under lexicon-dir should not
        // have to evict every Spring cache in the application to see it take effect.
        when(rankingLexicons.path()).thenReturn("ranking-lexicons");
        when(rankingLexicons.name()).thenReturn("rankingLexicons");
        when(rankingLexicons.reload()).thenReturn(2);

        mockMvc.perform(post("/api/system/cache/ranking-lexicons/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rankingLexicons").value(2));

        verify(cacheManager, never()).getCacheNames();
    }

    @Test
    void anUnknownReloadableCacheIsNotFound() {
        when(rankingLexicons.path()).thenReturn("ranking-lexicons");

        assertThatThrownBy(() -> api.reloadCache("nope")).isInstanceOf(TurNotFoundException.class);
        verify(rankingLexicons, never()).reload();
    }
}
