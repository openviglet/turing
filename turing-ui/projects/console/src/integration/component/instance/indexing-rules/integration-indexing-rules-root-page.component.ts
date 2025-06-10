import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {TurIntegrationIndexingRulesListPageComponent} from "./integration-indexing-rules-list-page.component";

@Component({
    selector: 'integration-indexing-rules-root-page',
    templateUrl: './integration-indexing-rules-root-page.component.html',
    standalone: false
})
export class TurSNIndexingRulesRootPageComponent {
  constructor(private route: ActivatedRoute) {
  }
}
