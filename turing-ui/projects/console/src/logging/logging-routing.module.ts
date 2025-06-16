import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AuthGuard} from '../app/_helpers';
import {TurLoggingRootPageComponent} from "./component/root/logging-root-page.component";
import {TurLoggingGeneralPageComponent} from "./component/general/logging-general-page.component";
import {TurLoggingIndexingPageComponent} from "./component/indexing/logging-indexing-page.component";

const routes: Routes = [
  {
    path: '', component: TurLoggingRootPageComponent, canActivate: [AuthGuard],
    children: [
      {
        path: 'general', component: TurLoggingGeneralPageComponent, canActivate: [AuthGuard]
      },
      {
        path: 'indexing', component: TurLoggingIndexingPageComponent, canActivate: [AuthGuard]
      },
      {path: '', redirectTo: '/logging/general', pathMatch: 'full'}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TurLoggingRoutingModule {
}
