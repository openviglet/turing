package com.viglet.turing.system.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.shared.exception.TurNotFoundException;
import com.viglet.turing.system.cache.TurReloadableCache;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/system/cache")
@Tag(name = "Cache", description = "Application Cache API")
public class TurCacheAPI {

    private static final String SUCCESS = "success";

    private final CacheManager cacheManager;
    /** T1322l — the caches feature modules hold in beans, reached through an SPI system declares. */
    private final List<TurReloadableCache> reloadableCaches;

    public TurCacheAPI(CacheManager cacheManager, List<TurReloadableCache> reloadableCaches) {
        this.cacheManager = cacheManager;
        this.reloadableCaches = List.copyOf(reloadableCaches);
    }

    @Operation(summary = "List all application caches")
    @GetMapping
    public Map<String, Object> listCaches() {
        List<String> cacheNames = cacheManager.getCacheNames().stream().sorted().toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(SUCCESS, true);
        result.put("totalCaches", cacheNames.size());
        result.put("cacheNames", cacheNames);
        return result;
    }

    @Operation(summary = "Clear all application caches")
    @PostMapping("/clear")
    public Map<String, Object> clearAllCaches() {
        List<String> cacheNames = cacheManager.getCacheNames().stream().sorted().toList();
        cacheNames.forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(SUCCESS, true);
        result.put("clearedCaches", cacheNames.size());
        result.put("cacheNames", cacheNames);
        // T942 / §LVIII.7 — the compiled ranking lexicons are held in a bean, not by
        // the CacheManager, so "clear all caches" used to leave them standing. An
        // admin who clears everything and still sees stale ranking vocabulary has no
        // way left to tell the difference between a cache and a bug.
        reloadableCaches.forEach(cache -> result.put(cache.name(), cache.reload()));
        return result;
    }

    /**
     * T942 / §LVIII.7 — recompile the copilot's ranking lexicons alone, so an
     * operator who edited a file under
     * {@code turing.genai.copilot.ranking.lexicon-dir} does not have to flush every
     * other cache (or restart) to see it take effect.
     */
    @Operation(summary = "Reload one bean-held cache, e.g. the copilot ranking lexicons (ranking-lexicons)")
    @PostMapping("/{path}/reload")
    public Map<String, Object> reloadCache(@PathVariable String path) {
        TurReloadableCache cache = reloadableCaches.stream()
                .filter(candidate -> candidate.path().equals(path))
                .findFirst()
                .orElseThrow(() -> TurNotFoundException.of("Reloadable cache", path));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(SUCCESS, true);
        result.put(cache.name(), cache.reload());
        return result;
    }
}
