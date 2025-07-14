import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AuthGuard} from '../app/_helpers';
import {TurLoggingRootPageComponent} from "./component/root/logging-root-page.component";
import {TurLoggingIndexingPageComponent} from "./component/indexing/logging-indexing-page.component";
import {TurLoggingAemPageComponent} from "./component/aem/logging-aem-page.component";
import {TurLoggingServerPageComponent} from "./component/server/logging-server-page.component";

const routes: Routes = [
  {
    path: '', component: TurLoggingRootPageComponent, canActivate: [AuthGuard],
    children: [
      {
        path: 'server', component: TurLoggingServerPageComponent, canActivate: [AuthGuard]
      },
      {
        path: 'indexing', component: TurLoggingIndexingPageComponent, canActivate: [AuthGuard]
      },
      {
        path: 'aem', component: TurLoggingAemPageComponent, canActivate: [AuthGuard]
      },
      {path: '', redirectTo: '/logging/server', pathMatch: 'full'}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TurLoggingRoutingModule {
}
