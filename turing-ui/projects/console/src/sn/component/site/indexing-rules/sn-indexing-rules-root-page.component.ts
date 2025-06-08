import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {TurSNIndexingRulesListPageComponent} from "./sn-indexing-rules-list-page.component";

@Component({
    selector: 'sn-ranking-expression-root-page',
    templateUrl: './sn-indexing-rules-root-page.component.html',
    standalone: false
})
export class TurSNIndexingRulesRootPageComponent {
  constructor(private route: ActivatedRoute) {
  }
}
