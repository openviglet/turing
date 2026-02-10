import { Navigate, Route, useParams } from "react-router-dom"
import SECustomFacetRootPage from "../console/se/se.custom.facet.root.page"
import SNSiteBehaviorPage from "../console/sn/behavior/sn.site.behavior.page"
import SECustomFacetListPage from "../console/sn/custom-facet/sn.custom.facet.list.page"
import SECustomFacetPage from "../console/sn/custom-facet/sn.custom.facet.page"
import SNSiteDetailPage from "../console/sn/detail/sn.site.detail.page"
import SNSiteFacetOrderingPage from "../console/sn/facet-ordering/sn.site.facets.ordering.page"
import SNSiteFieldListPage from "../console/sn/fields/sn.site.field.list.page"
import SNSiteFieldPage from "../console/sn/fields/sn.site.field.page"
import SNSiteGenAIPage from "../console/sn/genai/sn.site.genai.page"
import SNSiteMergeProvidersListPage from "../console/sn/merge-providers/sn.site.merge.providers.list.page"
import SNSiteMergeProvidersPage from "../console/sn/merge-providers/sn.site.merge.providers.page"
import SNSiteMultiLanguageListPage from "../console/sn/multi-language/sn.site.multi.language.list.page"
import SNSiteMultiLanguagePage from "../console/sn/multi-language/sn.site.multi.language.page"
import SNSiteResultRankingListPage from "../console/sn/result-ranking/sn.site.result.ranking.list.page"
import SNSiteResultRankingPage from "../console/sn/result-ranking/sn.site.result.ranking.page"
import SNSiteListPage from "../console/sn/sn.site.list.page"
import SNSitePage from "../console/sn/sn.site.page"
import SNSiteRootPage from "../console/sn/sn.site.root.page"
import SNSiteSpotlightListPage from "../console/sn/spotlight/sn.site.spotlight.list.page"
import SNSiteSpotlightPage from "../console/sn/spotlight/sn.site.spotlight.page"
import SNSiteTopSearchTermsPage from "../console/sn/top-search-terms/sn.site.top.search.terms.page"
import { ROUTES } from "../routes.const"

function RedirectToSNDetail() {
    const { id } = useParams();
    return <Navigate to={`${ROUTES.SN_INSTANCE}/${id}/detail`} replace />;
}

export const SNRoutes = (
    <Route path={ROUTES.SN_ROOT} element={<SNSiteRootPage />}>
        <Route index element={<Navigate to={ROUTES.SN_INSTANCE} replace />} />
        <Route path={ROUTES.SN_INSTANCE} element={<SNSiteListPage />} />
        <Route path={`${ROUTES.SN_INSTANCE}/:id`} element={<SNSitePage />}>
            <Route index element={<RedirectToSNDetail />} />
            <Route path={'detail'} element={<SNSiteDetailPage />} />
            <Route path={'locale'} element={<SNSiteMultiLanguageListPage />} />
            <Route path={'locale/:localeId'} element={<SNSiteMultiLanguagePage />} />
            <Route path={'field'} element={<SNSiteFieldListPage />} />
            <Route path={'field/:fieldId'} element={<SNSiteFieldPage />} />
            <Route path={'behavior'} element={<SNSiteBehaviorPage />} />
            <Route path={'facet-ordering'} element={<SNSiteFacetOrderingPage />} />
            <Route path={'ai'} element={<SNSiteGenAIPage />} />
            <Route path={'result-ranking'} element={<SNSiteResultRankingListPage />} />
            <Route path={'merge-providers'} element={<SNSiteMergeProvidersListPage />} />
            <Route path={'spotlight'} element={<SNSiteSpotlightListPage />} />
            <Route path={'spotlight/:spotlightId'} element={<SNSiteSpotlightPage />} />
            <Route path={'top-terms/:period?'} element={<SNSiteTopSearchTermsPage />} />
            <Route path={'result-ranking/:resultRankingId'} element={<SNSiteResultRankingPage />} />
            <Route path={'merge-providers/:mergeProviderId'} element={<SNSiteMergeProvidersPage />} />
            <Route path={'custom-facet'} element={<SECustomFacetRootPage />}>
                <Route index element={<SECustomFacetListPage />} />
                <Route path=":customFacetId" element={<SECustomFacetPage />} />
            </Route>
        </Route>
    </Route>
)
