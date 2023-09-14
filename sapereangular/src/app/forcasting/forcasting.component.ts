import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import { fnum2,fnum3, fnum5, precise_round, formatTime, formatDate, formatTime2, timeYYYYMMDDHMtoDate, formatTimeWindow
  , getDefaultInitTime, getDefaultHour, getDefaulInitDay, getDefaultTargetTime, getDefaultTargetDay
  , getDefaultTime, getDefaultTime2, timeHMtoDate, toogleDisplay, format2D, fnum
 } from '../common/util.js';
@Component({
  selector: 'app-lsas',
  templateUrl: './forcasting.component.html',
  styleUrls: ['./forcasting.component.scss']
})
export class ForcastingComponent implements OnInit {
  private forcastingRequest = {"samplingNb":10, "timestamp": null, "time" :getDefaultTime(), "year":"0", "month":"0", "day":"0"}
  private forcastingResult = {};
  private forcastingRef = {};
  private nbValues = 0;
  private sInterval = {"min":"", "max":""};
  private inputError = "";

  constructor(private httpClient: HttpClient,private _constant: ConstantsService) {
    var a_date = new Date();
    a_date.setFullYear(2021)
    this.forcastingRequest.year = ""+a_date.getFullYear();
    this.forcastingRequest.month = format2D(1+a_date.getMonth());
    this.forcastingRequest.day =  format2D(a_date.getDate());
    console.log("forcasting page constructor");
    this.httpClient.get(this._constant.baseAppUrl+'energy/getEndUserForcastingRef').
    subscribe(result => {
      this.forcastingRef = result;
      console.log("forcastingRef = ", this.forcastingRef);
      this.forcastingRequest.time = this.forcastingRef["defaultTime"];
      var interval = this.forcastingRef["datesInterval"];
      var smin = (interval["beginDate"]).substring(0,16);
      var smax = (interval["endDate"]).substring(0,16);
      this.sInterval = {"min": smin, "max" : smax};
      this.forcastingRef["minDate"] = new Date(interval["beginDate"]);
      this.forcastingRef["maxDate"] = new Date(interval["endDate"]);
      console.log(this.sInterval, this.forcastingRef["minDate"], this.forcastingRef["maxDate"]);
    })
  }

  callForcasting() {
    // call getForcasting
    console.log("callForcasting", this.forcastingRequest.time);
    var date = this.forcastingRequest.year + "-" + this.forcastingRequest.month + "-" + this.forcastingRequest.day;
    var s_timestamp = date + " " + this.forcastingRequest.time + ":00+0000"
    var timestamp = new Date(s_timestamp);
    this.inputError = "";
    if(timestamp < this.forcastingRef["minDate"]) {
      this.inputError = "Input error : the date entered is before the min date " + this.forcastingRef["datesInterval"]["beginDate"];
    } else if (timestamp > this.forcastingRef["maxDate"]) {
      this.inputError = "Input error : the date entered is after the max date " + this.forcastingRef["datesInterval"]["endDate"];
    } else {
      var test2 = {
        "timestamp": "2021-10-27 21:15:00+0000",
        "samplingNb": "4"
        }
      console.log("timestamp = ", s_timestamp);

      this.forcastingRequest.timestamp = s_timestamp;// s_timestamp;
      this.httpClient.post(this._constant.baseAppUrl+'energy/getForcasting'
      , this.forcastingRequest // test2
      , { responseType: 'json' }).
      subscribe(result => {
        console.log(result);
        this.forcastingResult = result;
        this.nbValues = (this.forcastingResult["predicetedValues"]).length;
        var nbTimestamps = (this.forcastingResult["timestamps"]).length;
        this.forcastingResult["horizon"] = "";
        this.forcastingResult["errorMSE"] = 0;
        this.forcastingResult["errorRMSE"] = 0;
        var mse1 = 0;
        if (this.nbValues == 4) {
          for (var valIdx = 0; valIdx < this.nbValues; valIdx++) {
            var delta = this.forcastingResult["predicetedValues"][valIdx] - this.forcastingResult["realValues"][valIdx];
            console.log(delta);
            mse1+= delta*delta;
          }
          var mse = mse1/this.nbValues;
          this.forcastingResult["errorMSE"] = mse;
          this.forcastingResult["errorRMSE"] = Math.sqrt( mse);
        }
        if(nbTimestamps > 0) {
          this.forcastingResult["horizon"] = this.forcastingResult["timestamps"][(nbTimestamps-1)];
        }
        console.log("forcastingResult = ", this.forcastingResult, this.forcastingResult["timestamps"], nbTimestamps, this.forcastingResult["horizon"]);
      });
    }
  }

  ngOnInit() {
  }

  fnum3(num, displayZero=false) {
    return fnum3(num, displayZero);
  }

  fnum5(num, displayZero=false) {
    var result1 = fnum5(num, displayZero);
    return result1.replace(" ", "");
  }
}
