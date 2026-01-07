import { Navigate, Route } from "react-router-dom"
import StoreInstanceListPage from "../console/store/store.instance.list.page"
import StoreInstancePage from "../console/store/store.instance.page"
import StoreInstanceRootPage from "../console/store/store.instance.root.page"
import { ROUTES } from "../routes.const"

export const StoreRoutes = (
    <Route path={ROUTES.STORE_ROOT} element={<StoreInstanceRootPage />}>
        <Route index element={<Navigate to={ROUTES.STORE_INSTANCE} replace />} />
        <Route path={ROUTES.STORE_INSTANCE} element={<StoreInstanceListPage />} />
        <Route path={`${ROUTES.STORE_INSTANCE}/:id`} element={<StoreInstancePage />} />
    </Route>
)
