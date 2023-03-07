import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  name: String = "";
  result: String = "";
  property: String = "";
  value: String = "";
  waiting: String = "";
  timeLeft: number = 20;
  interval;

  startTimer() {
    this.timeLeft=10;
    this.interval = setInterval(() => {
      if (this.timeLeft > 0 && this.result == "") {
        this.httpClient.get(this._constant.baseAppUrl+'getResult?query=' + this.name, { responseType: 'text' })
          .subscribe(res => {
            this.result = res;
          })
        this.timeLeft--;
      }
      if (this.result != "") {
        clearInterval(this.interval);
      }
      if (this.timeLeft ==0) {
        clearInterval(this.interval);
      }
      console.log(this.timeLeft)
    }, 1000)
  }

  pauseTimer() {
    clearInterval(this.interval);
  }

  constructor(private httpClient: HttpClient, private _constant: ConstantsService) { }

  ngOnInit() {
  }

  addQuery() {
    this.httpClient.post(this._constant.baseAppUrl+'addQuery',
      { "name": this.name, "prop": this.property.split(','), "values": this.value.split(','), "waiting": this.waiting.split(',') })
      .subscribe(res => {
        console.log(res);
        this.result="";
        this.startTimer();
      })
    console.log(this.name);

  }

  positiveReward() {
    this.httpClient.get(this._constant.baseAppUrl+'reward?name=' + this.name + '&reward=10', { responseType: 'text' }).
      subscribe(res => {
        this.result="";
        console.log(res);
      })
  }

  negativeReward() {
    this.httpClient.get(this._constant.baseAppUrl+'reward?name=' + this.name + '&reward=-10', { responseType: 'text' }).
      subscribe(res => {
        this.result="";
        console.log(res);
      })
  }


}
