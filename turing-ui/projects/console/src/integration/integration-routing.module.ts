import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AuthGuard} from '../app/_helpers';
import {TurIntegrationInstanceListPageComponent} from './component/instance/integration-instance-list-page.component';
import {TurIntegrationRootPageComponent} from './component/root/integration-root-page.component';
import {TurIntegrationAemListSourceComponent} from "./component/instance/aem/integration-aem-list-source.component";
import {TurIntegrationAemSourceComponent} from "./component/instance/aem/integration-aem-source.component";
import {
  TurIntegrationInstanceDetailPageComponent
} from "./component/instance/integration-instance-detail-page.component";
import {TurIntegrationAemMenuPageComponent} from "./component/instance/aem/integration-aem-menu-page.component";
import {TurIntegrationWcMenuPageComponent} from "./component/instance/wc/integration-wc-menu-page.component";
import {TurIntegrationWcPageComponent} from "./component/instance/wc/integration-wc-page.component";
import {TurIntegrationWcListPageComponent} from "./component/instance/wc/integration-wc-list-page.component";
import {
  TurIntegrationMonitoringPageComponent
} from "./component/instance/monitoring/integration-monitoring-page.component";
import {
  TurSNIndexingRulesRootPageComponent
} from "./component/instance/indexing-rules/integration-indexing-rules-root-page.component";
import {
  TurIntegrationIndexingRulesListPageComponent
} from "./component/instance/indexing-rules/integration-indexing-rules-list-page.component";
import {
  TurIntegrationIndexingRulesPageComponent
} from "./component/instance/indexing-rules/integration-indexing-rules-page.component";

const routes: Routes = [
  {
    path: '', component: TurIntegrationRootPageComponent, canActivate: [AuthGuard],
    children: [
      {
        path: 'instance', component: TurIntegrationInstanceListPageComponent, canActivate: [AuthGuard]
      },
      {
        path: 'instance/:id', component: TurIntegrationInstanceDetailPageComponent, canActivate: [AuthGuard]
      },
      {
        path: 'aem/:id', component: TurIntegrationAemMenuPageComponent, canActivate: [AuthGuard],
        children: [
          {path: 'source', component: TurIntegrationAemListSourceComponent, canActivate: [AuthGuard]},
          {path: 'source/:aemId', component: TurIntegrationAemSourceComponent, canActivate: [AuthGuard]},
          {path: 'detail', component: TurIntegrationInstanceDetailPageComponent, canActivate: [AuthGuard]},
          {path: 'monitoring', component: TurIntegrationMonitoringPageComponent, canActivate: [AuthGuard]},
          {
            path: 'indexing-rule', component: TurSNIndexingRulesRootPageComponent, canActivate: [AuthGuard],
            children: [
              {path: 'list', component: TurIntegrationIndexingRulesListPageComponent, canActivate: [AuthGuard]},
              {path: ':indexingRuleId', component: TurIntegrationIndexingRulesPageComponent, canActivate: [AuthGuard]},
              {path: '', redirectTo: 'list', pathMatch: 'full'}
            ]
          },
          {path: '', redirectTo: 'detail', pathMatch: 'full'}
        ]
      },
      {
        path: 'web-crawler/:id', component: TurIntegrationWcMenuPageComponent, canActivate: [AuthGuard],
        children: [
          {path: 'source', component: TurIntegrationWcListPageComponent, canActivate: [AuthGuard]},
          {path: 'source/:wcId', component: TurIntegrationWcPageComponent, canActivate: [AuthGuard]},
          {path: 'detail', component: TurIntegrationInstanceDetailPageComponent, canActivate: [AuthGuard]},
          {path: '', redirectTo: 'detail', pathMatch: 'full'}
        ]
      },
      {path: '', redirectTo: '/integration/instance', pathMatch: 'full'}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TurIntegrationRoutingModule {
}
