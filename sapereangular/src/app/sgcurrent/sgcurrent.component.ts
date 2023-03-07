import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpParams  } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import { NgSelectModule } from '@ng-select/ng-select';
import { C } from 'angular-bootstrap-md/lib/free/utils/keyboard-navigation';
import { Observable, interval, Subscription, timer } from 'rxjs';
import { fnum2,fnum3, precise_round, formatTime, formatDate, formatTime2, timeYYYYMMDDHMtoDate, formatTimeWindow
  , getDefaultInitTime, getDefaultHour, getDefaulInitDay, getDefaultTargetTime, getDefaultTargetDay
  , getDefaultTime, getDefaultTime2, timeHMtoDate, toogleDisplay
 } from '../common/util.js';


@Component({
  selector: 'app-energy',
  templateUrl: './sgcurrent.component.html',
  styleUrls: ['./sgcurrent.component.scss']
})
export class SGCurrentComponent implements OnInit {
  private subscription: Subscription;
  everySecond: Observable<number> = timer(0, 1000);

  activateAutoRefresh = false;

  nodeContent = {};
  nodeTotalHistory = [];

  // Filters
  filter_consumerDeviceCategories = [];
  filter_producerDeviceCategories = [];
  filter_hideExpiredAgents = "NO";
  // Form of consumer creation
  c_beginDate = new Date();
  c_beginTime = getDefaultTime();
  c_endDate = new Date();
  c_endTime = getDefaultTime();
  c_power = 0;
  c_duration = 0;
  c_delayToleranceMinutes = 0;
  c_priorityLevel = "Low";
  c_deviceName = "";
  c_deviceCategory = "";
  //listPriorityLevel = [];

  // Form of producer creation
  p_beginDate = new Date();
  p_beginTime = getDefaultTime();
  p_endDate = new Date();
  p_endTime = getDefaultTime();
  p_power = 0;
  p_duration = 0;
  p_deviceName = "";
  p_deviceCategory = "";

  tab_beginTime = {};
  tab_endTime = {};
  tab_duration = {};
  tab_power = {};
  tab_deviceName = {};
  tab_deviceLocation = {};
  tab_deviceCategory = {};
  tab_sensorNumber = {};
  tab_delayToleranceRatio = {};

  listPriorityLevel = [];
  listDeviceCategoryProducer = [];
  listDeviceCategoryConsumer = [];
  listYesNo = [];
  maxDisplayTime = new Date();

  targetAction = ""; // restart agent or modify agent

  constructor(private httpClient: HttpClient
      ,private _constant: ConstantsService
     // ,private updateSubscription: Subscription

      ) {
    this.refreshNodeContent();
  }

  refreshNodeContent() {
    console.log("refreshNodeContent : filter_hideExpiredAgents = ", this.filter_hideExpiredAgents);
    let filterParams = new HttpParams()
      .set('consumerDeviceCategories', ""+this.filter_consumerDeviceCategories)
      .set('producerDeviceCategories', ""+this.filter_producerDeviceCategories)
      .set('hideExpiredAgents', (this.filter_hideExpiredAgents=='YES')? 'true' : 'false');
    ;
    console.log("refreshNodeContent filterParams = ", filterParams);
    this.httpClient.get(this._constant.baseAppUrl+'energy/retrieveNodeContent', { params: filterParams }).
      subscribe((res :any[])=> {
        this.nodeContent=res;
        this.listYesNo = this.nodeContent['listYesNo'];
        this.listPriorityLevel = this.nodeContent['listPriorityLevel'];
        this.listDeviceCategoryProducer = this.nodeContent['listDeviceCategoryProducer'];
        this.listDeviceCategoryConsumer = this.nodeContent['listDeviceCategoryConsumer'];
        console.log('listDeviceCategoryProducer:', this.listDeviceCategoryProducer);
        console.log(this._constant.baseAppUrl+'energy/retrieveNodeContent');
        console.log("this.httpClient.get : nodeContent = ", this.nodeContent, this.nodeContent['consumers']);
        for (const consumer of this.nodeContent['consumers']) {
            var agent = consumer.agentName;
            //console.log("consumer", consumer, agent);
            this.tab_beginTime[agent] = getDefaultTime();
            this.tab_endTime[agent] = getDefaultTime2(60);
            this.tab_power[agent] = consumer.power > 0 ? consumer.power : consumer.disabledPower;
            this.tab_duration[agent] = 0;
            this.tab_delayToleranceRatio[agent] = consumer.delayToleranceRatio;
            // console.log("agent.delayToleranceRatio", consumer, consumer.delayToleranceRatio);
        }
        for (const producer of this.nodeContent['producers']) {
            var agent = producer.agentName;
            //console.log("producer", producer, agent);
            this.tab_beginTime[agent] = getDefaultTime();
            this.tab_endTime[agent] = getDefaultTime2(60);
            this.tab_power[agent] = producer.power > 0 ? producer.power : producer.disabledPower;
            this.tab_duration[agent] = 0;
      }
      this.maxDisplayTime.setHours(0);
      this.maxDisplayTime.setMinutes(0);
      this.maxDisplayTime.setSeconds(0);
      this.maxDisplayTime.setMilliseconds(0);
      this.maxDisplayTime.setTime(this.maxDisplayTime.getTime() + 60 * 24*60 * 1000 );
      console.log("maxDisplayTime", this.maxDisplayTime);
    });
  }

  displayFirstChars(str, maxCharsNb)  {
    if(str==null) {
      return null;
    } else if(str.length > maxCharsNb) {
      return str.substring(0,maxCharsNb) + "..."
    } else {
      return str;
    }
  }

  disaplyTime(dateStr) {
    var date = new Date(dateStr);
    //console.log("disaplyHHMM", dateStr, date);
    if(date > this.maxDisplayTime) {
      return "";
    }
  return formatTime2(date);
}

  getClassTRagent(agent) {
    if(agent.hasExpired) {
      return 'tr_expired';
    } else if(!agent.isInSpace) {
      return 'tr_not_inspace';
    } else if(agent.isDisabled) {
      return 'tr_disabled';
    } else {
      return '';
      //return 'tr_ok';
    }
    //class='expired_{{consumer.hasExpired}}'
  }

  getAlertNotInSapce(agent) {
    if(agent.hasExpired) {
      return "";
    } else if(agent.isInSpace){
      return "";
    } else {
      return " [!!NOT IN SPACE!!]"
    }
  }

  getTxtClassConsumer(consumer) {
    if(consumer.hasExpired || consumer.isDisabled)  {
      return "";
    } else if(consumer.missingPower > 0.001) {
        return 'txt_warning_high';
    } else {
      return 'txt_ok';
    }
  }

  getClassMissing(consumer, available) {
    var missingPower = consumer.missingPower
    if(missingPower <= 0.001) {
      return '';
    } else if(missingPower <available ) {
      if(missingPower<0.001) {
        console.log("getClassMissing", missingPower, available);
      }
      var warningDurationSec = consumer.warningDurationSec;
      console.log("getClassMissing warningDurationSec = ", warningDurationSec);
      if(warningDurationSec>=40) {
        return "warning_duration_catastrophic";
      } else if(warningDurationSec>=20) {
        return "warning_duration_veryhigh";
      } else if(warningDurationSec>=10) {
        return "warning_duration_high";
      } else if(warningDurationSec>=5) {
        return "warning_duration_medium";
      } else {
        return "warning_duration_weak";
      }
    } else {
      return '';
    }
  }

  getOffersClass(consumer) {
      if(consumer.offersTotal > 0
         && (consumer.offersTotal >= consumer.missingPower - 0.0001)) {
          return "offers_ok";
      }
      return "";
  }

  getClassMissingRequests(nodeTotal) {
    return this.getClassMissing(nodeTotal.minRequestMissing, nodeTotal.available);
  }


  getClassEndDate(sDate) {
    var date = new Date(sDate);
    //console.log("getClassEndDate", date);
    var current = (new Date()).getTime();
    var time = date.getTime();
    var remain = time - current;
    if(remain<0) {
      return '';
    } else if(remain < 1000*60) {
      return "txt_warning_high";
    } else if(remain < 5*1000*60) {
      return "txt_warning_medium";
    } else {
      return "";
    }
  }

  fnum2(num, displayZero=false) {
    return fnum2(num, displayZero);
  }

  addConsumer(){
    this.c_beginDate = timeHMtoDate(this.c_beginTime);
    this.c_endDate = timeHMtoDate(this.c_endTime);
    console.log("addConsumer", this.c_beginDate ,"c_priorityLevel", this.c_priorityLevel);
    this.httpClient.post(this._constant.baseAppUrl+'energy/addAgent',
          { "agentType":"Consumer", "beginDate":this.c_beginDate, "endDate":this.c_endDate, "priorityLevel":this.c_priorityLevel
            , "deviceName":this.c_deviceName, "deviceCategory":{"value":this.c_deviceCategory}, "environmentalImpact": 3
            , "power":this.c_power, "delayToleranceMinutes": this.c_delayToleranceMinutes, "delayToleranceRatio" : 0 }
           , { responseType: 'text' }).
    subscribe(res => {
      console.log("addConsumer : result = ", res);
      this.reload();
    })
  }


  addProducer() {
    this.p_beginDate = timeHMtoDate(this.p_beginTime);
    this.p_endDate = timeHMtoDate(this.p_endTime);
    var envImpact = 2;
    if(this.p_deviceCategory=='EXTERNAL_ENG') {
      envImpact = 3;
    }
    console.log("addProducer", this.p_beginTime, this.p_beginDate );
    if(this.p_power>0 && this.p_endTime !='') {
      this.httpClient.post(this._constant.baseAppUrl+'energy/addAgent',
      { "agentType":"Producer", "beginDate":this.p_beginDate, "endDate":this.p_endDate, "power":this.p_power
          , "deviceName":this.p_deviceName, "deviceCategory":{"value":this.p_deviceCategory}, "environmentalImpact": envImpact }
      , { responseType: 'text' }).
      subscribe(res => {
        console.log("addProducer : res = ", res);
        this.reload();
      })
    }
  }


reload() {
    console.log("--- refresh page");
    location.reload()
}



modify_agent(agent, _targetAction) {
  this.targetAction = _targetAction;
  var agentName = agent.agentName;
  console.log("modify_agent", agent, agentName, " targetAction = ", this.targetAction);
  var oBeginTime = document.getElementById( "beginTime_" + agentName);
  console.log("modify_agent : oBeginTime", oBeginTime);
  oBeginTime.classList.remove("hide");
  var oEndTime = document.getElementById( "endTime_" + agentName);
  oEndTime.classList.remove("hide");
  var oDuration = document.getElementById( "duration_" + agentName);
  //oDuration.classList.remove("hide");
  var oPower = document.getElementById( "power_" + agentName);
  console.log("modify_agent : oPower = ", oPower);
  oPower.classList.remove("hide");
  var oToleance =  document.getElementById( "delayToleranceRatio_" + agentName);
  if(oToleance!=null) {
    oToleance.classList.remove("hide");
  }
  var oTxtBeginTime = document.getElementById( "txt_beginTime_" + agentName);
  oTxtBeginTime.innerHTML="";
  var oTxtEndTime = document.getElementById( "txt_endTime_" + agentName);
  oTxtEndTime.innerHTML="";
  var oTxtPower = document.getElementById( "txt_power_" + agentName);
  oTxtPower.innerHTML="";
  var oTxtTolerance = document.getElementById( "txt_delayToleranceRatio_" + agentName);
  if(oTxtTolerance!=null) {
    oTxtTolerance.innerHTML="";
  }


  // Buttons
  var oRestart = document.getElementById("restart_"+ agentName );
  oRestart.classList.add("hide");
  var oModify = document.getElementById("modify_"+ agentName );
  if(oModify!=null) {
    oModify.classList.add("hide");
  }
  var oStop = document.getElementById("stop_"+ agentName );
  oStop.classList.add("hide");
  var oSave = document.getElementById("save_"+ agentName ); 
  oSave.classList.remove("hide");



}


save_agent(agent) {
  var agentName = agent.agentName;
  console.log("save_agent", agent, agentName);
  var oBeginTime = document.getElementById( "beginTime_" + agentName);
  oBeginTime.classList.add("hide");
  var oEndTime = document.getElementById( "endTime_" + agentName);
  oEndTime.classList.add("hide");
  var oDuration = document.getElementById( "duration_" + agentName);
  if(oDuration!=null) {
    oDuration.classList.add("hide");
  }
  var oPower = document.getElementById( "power_" + agentName);
  oPower.classList.add("hide");
  var oToleance =  document.getElementById( "delayToleranceRatio_" + agentName);
  if(oToleance!=null) {
    oToleance.classList.add("hide");
  }
  // Buttons
  var oRestart = document.getElementById("restart_"+ agentName );
  oRestart.classList.remove("hide");
  var oModify = document.getElementById("modify_"+ agentName );
  oModify.classList.remove("hide");
  var oSave = document.getElementById("save_"+ agentName );
  oSave.classList.add("hide");

  agent.beginDate = timeHMtoDate(this.tab_beginTime[agentName]);
  agent.endDate = timeHMtoDate(this.tab_endTime[agentName]);
  //console.log("modify_agent beginDate", agent.beginDate, "endDate",  agent.endDate);
  agent.power = this.tab_power[agentName];
  console.log("save_agent : agent.power = ", agent.power);
  agent.delayToleranceRatio = this.tab_delayToleranceRatio[agentName];
  if(agent.delayToleranceRatio==null) {
    agent.delayToleranceRatio = 0;
  }

  if(agent.power>0)  {
    console.log("save_agent : targetAction = ", this.targetAction);
    if(this.targetAction == 'restartAgent') {
      this.httpClient.post(this._constant.baseAppUrl+'energy/restartAgent', agent , { responseType: 'text' }).
        subscribe(res => {
          console.log("restartAgent : result = ", res);
          this.reload();
        });
      } else if(this.targetAction == 'modifyAgent') {
        this.httpClient.post(this._constant.baseAppUrl+'energy/modifyAgent', agent , { responseType: 'text' }).
        subscribe(res => {
          console.log("modifyAgent : result = ", res);
          this.reload();
        });
      }
    }
}


applyFilter() {
  //this.reload();
  this.refreshNodeContent();
}


stop_agent(agent) {
  var agentName = agent.agentName;
  console.log("stop_agent", agent, agentName);
  this.httpClient.post(this._constant.baseAppUrl+'energy/stopAgent', agent , { responseType: 'text' }).
    subscribe(res => {
      console.log("stop_agent : result = ", res);
      this.reload();
    });
}


  ngOnInit() {
    console.log("ngOnInit : subscription  = ", this.subscription);
    if(this.activateAutoRefresh) {
      this.subscription = this.everySecond.subscribe((seconds) => {
        this.refreshNodeContent();
      })
    }
  }

  set_c_endTime(event) {
    console.log("set_c_endTime", event, event.target.power);
    this.c_beginDate = timeHMtoDate(this.c_beginTime);
    this.c_endDate = this.c_beginDate;
    this.c_endDate.setTime(this.c_beginDate.getTime() + 60 * this.c_duration * 1000 );
    this.c_endTime = formatTime(this.c_endDate);
    this.c_delayToleranceMinutes = this.c_duration;
    console.log("set_c_endTime", this.c_beginDate.getTime(), this.c_beginTime, this.c_duration, this.c_endTime);
  }

  set_p_endTime(event) {
    console.log("set_p_endTime", event, event.target.power);
    this.p_beginDate = timeHMtoDate(this.p_beginTime);
    this.p_endDate = this.c_beginDate;
    this.p_endDate.setTime(this.p_beginDate.getTime() + 60 * this.p_duration * 1000 );
    this.p_endTime = formatTime(this.p_endDate);
    console.log("set_p_endTime", this.c_beginDate.getTime(), this.p_beginTime, this.p_duration, this.p_endTime);
  }


  toogleDisplay(spanId) {
    return toogleDisplay(spanId,  'display_yes',  'display_none');
  }
}
