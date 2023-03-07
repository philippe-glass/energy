import { BrowserModule } from '@angular/platform-browser';
import { NgModule, NO_ERRORS_SCHEMA, Injectable } from '@angular/core';
import { MDBBootstrapModule } from 'angular-bootstrap-md';
import { HttpClientModule, HttpInterceptor, HttpRequest, HttpHandler, HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { FormsModule } from '@angular/forms';
import { HomeComponent } from './home/home.component';
import { GraphComponent } from './graph/graph.component';
import { AboutComponent } from './about/about.component';
import { AddserviceComponent } from './addservice/addservice.component';
import { ConstantsService } from './common/services/constants.service';
import { LoginComponent } from './login/login.component';
import { HeaderComponent } from './header/header.component';
import { LocalservicesComponent } from './localservices/localservices.component';
import { ConfigComponent } from './config/config.component';
import { LsasComponent } from './lsas/lsas.component';
import { SimulationComponent } from './simulation/simulation.component';
import { SGCurrentComponent } from './sgcurrent/sgcurrent.component';
import { SGHistoryComponent } from './sghistory/sghistory.component';
import { SGLearningComponent } from './sglearning/sglearning.component';



@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    AboutComponent,
    AddserviceComponent,
    LoginComponent,
    HeaderComponent,
    LocalservicesComponent,
    ConfigComponent,
    LsasComponent,
    SimulationComponent,
    SGCurrentComponent,
    SGHistoryComponent,
    SGLearningComponent,
    GraphComponent
      ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    BrowserAnimationsModule,
    HttpClientModule,
    NgxGraphModule,
    MDBBootstrapModule.forRoot()
  ],
  schemas: [ NO_ERRORS_SCHEMA ],
  providers: [ConstantsService],
  bootstrap: [AppComponent]
})
export class AppModule { }

