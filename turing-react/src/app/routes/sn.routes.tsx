import { Navigate, Route, useParams } from "react-router-dom"
import SNSiteBehaviorPage from "../console/sn/sn.site.behavior.page"
import SNSiteDetailPage from "../console/sn/sn.site.detail.page"
import SNSiteFacetOrderingPage from "../console/sn/sn.site.facets.ordering.page"
import SNSiteFieldPage from "../console/sn/sn.site.field.page"
import SNSiteFieldsPage from "../console/sn/sn.site.fields.page"
import SNSiteGenAIPage from "../console/sn/sn.site.genai.page"
import SNSiteListPage from "../console/sn/sn.site.list.page"
import SNSiteMergeProvidersListPage from "../console/sn/sn.site.merge.providers.list.page"
import SNSiteMergeProvidersPage from "../console/sn/sn.site.merge.providers.page"
import SNSiteMultiLanguagePage from "../console/sn/sn.site.multi.language.page"
import SNSitePage from "../console/sn/sn.site.page"
import SNSiteResultRankingListPage from "../console/sn/sn.site.result.ranking.list.page"
import SNSiteResultRankingPage from "../console/sn/sn.site.result.ranking.page"
import SNSiteRootPage from "../console/sn/sn.site.root.page"
import SNSiteSpotlightListPage from "../console/sn/sn.site.spotlight.list.page"
import SNSiteSpotlightPage from "../console/sn/sn.site.spotlight.page"
import SNSiteTopSearchTermsPage from "../console/sn/sn.site.top.search.terms.page"
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
            <Route path={`${ROUTES.SN_INSTANCE}/:id/detail`} element={<SNSiteDetailPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/locale`} element={<SNSiteMultiLanguagePage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/field`} element={<SNSiteFieldsPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/behavior`} element={<SNSiteBehaviorPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/facet-ordering`} element={<SNSiteFacetOrderingPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/ai`} element={<SNSiteGenAIPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/result-ranking`} element={<SNSiteResultRankingListPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/merge-providers`} element={<SNSiteMergeProvidersListPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/spotlight`} element={<SNSiteSpotlightListPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/top-terms`} element={<SNSiteTopSearchTermsPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/field/:fieldId`} element={<SNSiteFieldPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/result-ranking/:resultRankingId`} element={<SNSiteResultRankingPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/merge-providers/:mergeProviderId`} element={<SNSiteMergeProvidersPage />} />
            <Route path={`${ROUTES.SN_INSTANCE}/:id/spotlight/:spotlightId`} element={<SNSiteSpotlightPage />} />
        </Route>
    </Route>
)
