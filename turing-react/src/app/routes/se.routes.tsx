import { Route } from "react-router-dom"
import SEInstanceListPage from "../console/se/se.instance.list.page"
import SEInstancePage from "../console/se/se.instance.page"
import SEInstanceRootPage from "../console/se/se.instance.root.page"
import { ROUTES } from "../routes.const"

export const SERoutes = (
    <Route path={ROUTES.SE_ROOT} element={<SEInstanceRootPage />}>
        <Route path={ROUTES.SE_INSTANCE} element={<SEInstanceListPage />} />
        <Route path={`${ROUTES.SE_INSTANCE}/:id`} element={<SEInstancePage />} />
    </Route>
)
