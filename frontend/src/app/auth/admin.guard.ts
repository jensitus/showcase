import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {LoginService} from './login.service';

export const adminGuard: CanActivateFn = () => {
    const loginService = inject(LoginService);
    const router = inject(Router);

    if (loginService.currentUser()?.role === 'ADMIN') {
        return true;
    }
    router.navigate(['/home']);
    return false;
};
