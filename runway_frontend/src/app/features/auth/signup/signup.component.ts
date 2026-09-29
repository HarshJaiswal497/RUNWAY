import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { SignupRequest } from '../../../core/models/auth.model';
import { TitleCasePipe } from '@angular/common';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink, TitleCasePipe],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.css'
})
export class SignupComponent {
  private authService = inject(AuthService);
  private router      = inject(Router);

  form: SignupRequest = {
    username: '',
    password: '',
    name:     '',
    gender:   '',
    email:    ''
  };

  invalidPasswordMsg = "Password must have 8 characters, and atleast one \n uppercase, lowercase and special symbol.";
  invalidUsernameMsg = "Username must have atleast 4 characters, and no spaces.";
  invalidGenderMsg = "Please select gender.";
  invalidEmailmsg = "Please enter valid email address."

  loading      = signal(false);
  error        = signal<string | null>(null);
  success      = signal(false);
  showPassword = signal(false);
  checkIfPasswordValid = signal<boolean | null>(null);
  checkIfUsnValid = signal<boolean | null>(null);
  checkIfGenderValid = signal<boolean | null>(null);
  checkIfEmailValid = signal<boolean | null>(null);
  
  passwordRegex = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%#*?&])[A-Za-z\d@$#!%*?&]{8,}$/
  usnRegex = /^[^\s]{4,}$/
  emailRegex = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
  checkPasswordValidity(pass:string){
    this.checkIfPasswordValid.set(this.passwordRegex.test(pass));
  }
  checkUsernameValidity(usn:string){
    this.checkIfUsnValid.set(this.usnRegex.test(usn));
  }
  checkEmailValidity(email:string){
    this.checkIfEmailValid.set(this.emailRegex.test(email));
  }

  checkGenderValidity(gender:string){
    if(gender== undefined || gender==''){
      this.checkIfGenderValid.set(false);
    }
    else{
      this.checkIfGenderValid.set(true);
    }
  }
  onSubmit(): void {
    if (!this.form.username || !this.form.password ||
        !this.form.name || !this.form.email) {
      this.error.set('Please fill in all required fields.');
      return;
    }
    
    this.checkPasswordValidity(this.form.password);
    this.checkUsernameValidity(this.form.username);
    this.checkGenderValidity(this.form.gender);
    this.checkEmailValidity(this.form.email);

    if(
      !this.checkIfEmailValid() || !this.checkIfGenderValid() || !this.checkIfPasswordValid() || 
      !this.checkIfUsnValid()
    )
    {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    
    this.authService.signup(this.form).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        // Auto redirect to login after 2 seconds
        setTimeout(() => this.router.navigate(['/login'], { state: { fromSignup: true } }), 2000);
      },
      error: (err) => {
        this.loading.set(false);
        if(err.status===409){
          this.error.set('Username or email already exists');
        }
        else if(err.status===500){
          this.error.set("Something went wrong. Please try again");
        }
      }
    });
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }
}