import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { first } from 'rxjs/operators';

import { AuthenticationService } from '../../_services';

@Component({
    templateUrl: 'login-page.component.html',
    standalone: false
})
export class TurLoginPageComponent implements OnInit {
  loginForm!: UntypedFormGroup;
  loading = false;
  submitted = false;
  returnUrl!: string;
  error = '';

  constructor(
    private formBuilder: UntypedFormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authenticationService: AuthenticationService
  ) {
    // redirect to home if already logged in
    console.log(this.authenticationService.userValue.authdata);
    if (this.authenticationService.userValue && this.authenticationService.userValue.authdata ) {

      window.location.href = '/console';
    }
  }

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });

    // get return url from route parameters or default to '/console'
    const queryReturnUrl = this.route.snapshot.queryParams['returnUrl'];
    this.returnUrl = this.isSafeRelativePath(queryReturnUrl) ? queryReturnUrl : '/console';
  }

  // convenience getter for easy access to form fields
  get f() { return this.loginForm.controls; }

  onSubmit() {
    this.submitted = true;

    // stop here if form is invalid
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.authenticationService.login(this.f.username.value, this.f.password.value)
      .pipe(first())
      .subscribe(
        data => {
          window.location.href = this.returnUrl;
        },
        error => {
          this.error = error;
          this.loading = false;
        });
  }
  /**
   * Checks if the given path is a safe, relative URL for internal navigation.
   */
  private isSafeRelativePath(path: string): boolean {
    // Only allow paths starting with '/' and not containing protocol, double slash, or blank
    return !!path && /^\/(?!\/)[\w\-./]*$/.test(path);
  }
}
