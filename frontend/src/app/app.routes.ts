import {Routes} from "@angular/router";
import {HomeComponent} from "./home/home.component";
import {NewTaskListComponent} from "./newtask/new-task-list/new-task-list.component";
import {TaskDetailComponent} from "./newtask/task-detail/task-detail.component";
import {WorkflowListComponent} from "./workflow/workflow-list/workflow-list.component";
import {WorkflowComponent} from "./workflow/workflow.component";
import {TaskComponent} from "./task/task.component";
import {SseComponent} from "./course/sse/sse.component";
import {LoginComponent} from "./auth/login/login.component";
import {RegisterComponent} from "./auth/register/register.component";
import {VerifyEmailComponent} from "./auth/verify-email/verify-email.component";
import {ForgotPasswordComponent} from "./auth/forgot-password/forgot-password.component";
import {ResetPasswordComponent} from "./auth/reset-password/reset-password.component";
import {ChangePasswordComponent} from "./auth/change-password/change-password.component";
import {UserProfileComponent} from "./auth/user-profile/user-profile.component";
import {adminGuard} from "./auth/admin.guard";
import {AdminUsersComponent} from "./admin/admin-users/admin-users.component";
import {SubmitAbstractComponent} from "./submission/submit-abstract/submit-abstract.component";
import {SubmissionListComponent} from "./submission/submission-list/submission-list.component";
import {ScreeningDashboardComponent} from "./screening/screening-dashboard.component";

export const routes: Routes = [
    {path: '', component: HomeComponent},
    {path: 'home', component: HomeComponent},
    {path: 'new-task-list', component: NewTaskListComponent},
    {path: 'tasks/:id', component: TaskDetailComponent},
    {path: 'workflow-list', component: WorkflowListComponent},
    {path: 'workflow/:tenantId/:id/:version', component: WorkflowComponent},
    {path: 'sse', component: SseComponent},
    {path: 'login', component: LoginComponent},
    {path: 'register', component: RegisterComponent},
    {path: 'verify-email', component: VerifyEmailComponent},
    {path: 'forgot-password', component: ForgotPasswordComponent},
    {path: 'reset-password', component: ResetPasswordComponent},
    {path: 'change-password', component: ChangePasswordComponent},
    {path: 'profile', component: UserProfileComponent},
    {path: 'admin/users', component: AdminUsersComponent, canActivate: [adminGuard]},
    {path: 'submissions', component: SubmissionListComponent},
    {path: 'submit-abstract', component: SubmitAbstractComponent},
    {path: 'screening', component: ScreeningDashboardComponent},
];
