import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import { AddserviceComponent } from './addservice/addservice.component';
import { LoginComponent } from './login/login.component';
import { LocalservicesComponent } from './localservices/localservices.component';
import { OldConfigComponent } from './oldconfig/oldconfig.component';
import { ConfigComponent } from './config/config.component';
import { LsasComponent } from './lsas/lsas.component';
import { SimulationComponent } from './simulation/simulation.component';
import { GraphComponent } from './graph/graph.component';
import { SGCurrentComponent } from './sgcurrent/sgcurrent.component';
import { SGHistoryComponent } from './sghistory/sghistory.component';
import { SGLearningComponent } from './sglearning/sglearning.component';
import { ForcastingComponent } from './forcasting/forcasting.component';



const routes: Routes = [
    { path: '', component: LoginComponent },
    { path: 'about', component: AboutComponent },
    { path: 'addservices', component: AddserviceComponent },
    { path: 'home', component: HomeComponent },
    { path: 'localservices', component: LocalservicesComponent },
    { path: 'logout', component: LoginComponent},
    { path: 'lsas', component: LsasComponent},
    { path: 'oldconfig', component: OldConfigComponent},
    { path: 'config', component: ConfigComponent},
    { path: 'simulation', component: SimulationComponent},
    { path: 'sgcurrent', component: SGCurrentComponent},
    { path: 'sghistory', component : SGHistoryComponent},
    { path: 'sglearning', component : SGLearningComponent},
    { path: 'forcasting', component : ForcastingComponent},
    { path: 'graph', component: GraphComponent},
    // otherwise redirect to home
    { path: '**', redirectTo: '' }];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
