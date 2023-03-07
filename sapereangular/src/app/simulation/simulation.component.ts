import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-simulation',
  templateUrl: './simulation.component.html',
  styleUrls: ['./simulation.component.scss']
})
export class SimulationComponent implements OnInit {

  number="";
  set="";
  ngOnInit() {
  }
  constructor(private httpClient: HttpClient,private _constant: ConstantsService) {}

  generate(){
    this.httpClient.post('/app/config/generateSim',
    { "number": this.number, "set":this.set}, { responseType: 'text' }).
    subscribe(res => {
      console.log(res);
    })

  }

}
