package com.viglet.turing.searchengine;

import java.util.List;

public record TurSECoreInfo(String name, long numDocs, List<TurSECoreSiteUsage> usedBySites) {
}
