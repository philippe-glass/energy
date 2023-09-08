import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpParams  } from '@angular/common/http';
import { ConstantsService } from '../common/services/constants.service';
import { NgSelectModule } from '@ng-select/ng-select';
import { C } from 'angular-bootstrap-md/lib/free/utils/keyboard-navigation';
import { Observable, interval, Subscription, timer } from 'rxjs';
import { fnum2,fnum3, precise_round, formatTime, formatDate, formatTime2, timeYYYYMMDDHMtoDate, formatTimeWindow
  , getDefaultInitTime, getDefaultHour, getDefaulInitDay, getDefaultTargetTime, getDefaultTargetDay
  , getDefaultTime, getDefaultTime2, timeHMtoDate, toogleDisplay, getDefaultTimeZone
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
  default_date = formatDate(new Date);
  default_timezone = getDefaultTimeZone();

  // Filters
  filter = {"consumerDeviceCategories":[] ,"producerDeviceCategories":[] ,"neighborNodeNames":[]
      ,"hideExpiredAgents":false, "multiNodes":false
    }
  // Form of consumer/producer creation
  agentInputForm = {
      "beginDate" :new Date(),"beginTime" :getDefaultTime() ,"endDate" :new Date() ,"endTime" :getDefaultTime()
      ,"power" :0  ,"duration" :0  ,"delayToleranceMinutes" :0 , "delayToleranceRatio":0  ,"priorityLevel" :"Low"   ,"deviceName" :""  ,"deviceCategory" :{"value":null}
      ,"agentType":{"value":null},  "environmentalImpact": 3
    };

  tab_agents = {
    "beginTime":{}
    ,"endTime":{}
    ,"duration":{}
    ,"power":{}
    ,"deviceName":{}
    ,"deviceLocation":{}
    ,"deviceCategory":{}
    ,"sensorNumber":{}
    ,"delayToleranceRatio":{}
  }

  listAgentType = [];
  listPriorityLevel = [];
  listDeviceCategoryProducer = [];
  listDeviceCategoryConsumer = [];
  defaultNodeLocation = {}
  mapNeighborNodes = {};
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
    console.log("refreshNodeContent : filter locations = ", this.filter.neighborNodeNames)
    let filterParams = new HttpParams();
    Object.entries(this.filter).forEach(([key, value]) => {
      filterParams = filterParams.append(key,""+value);
    });
    console.log("refreshNodeContent filterParams = ", filterParams);
    var serviceName = "retrieveNodeContent";
    if(this.filter.multiNodes) {
      serviceName = "retrieveAllNodesContent";
    }
    var serviceUrl = this._constant.baseAppUrl+'energy/' + serviceName;
    console.log("refreshNodeContent call service ", serviceUrl);
    this.httpClient.get(serviceUrl, { params: filterParams }).
      subscribe((res :any[])=> {
        console.log("refreshNodeContent : receive response", res);
        this.nodeContent=res;
        this.listYesNo = this.nodeContent['listYesNo'];
        this.listPriorityLevel = this.nodeContent['listPriorityLevel'];
        this.listDeviceCategoryProducer = this.nodeContent['listDeviceCategoryProducer'];
        this.listDeviceCategoryConsumer = this.nodeContent['listDeviceCategoryConsumer'];
        this.listAgentType = this.nodeContent['listAgentType'];
        this.defaultNodeLocation = this.nodeContent['nodeConfig'];
        this.mapNeighborNodes = this.nodeContent['mapNeighborNodes'];
        console.log("refreshNodeContent : mapNeighborNodes = ", this.mapNeighborNodes);
        console.log('refreshNodeContent : listDeviceCategoryProducer:', this.listDeviceCategoryProducer, serviceUrl);
        console.log("this.httpClient.get : nodeContent = ", this.nodeContent, this.nodeContent['consumers']);
        for (const consumer of this.nodeContent['consumers']) {
            var agent = consumer.agentName;
            //console.log("consumer", consumer, agent);
            this.tab_agents.beginTime[agent] = getDefaultTime();
            this.tab_agents.endTime[agent] = getDefaultTime2(60);
            this.tab_agents.power[agent] = consumer.power > 0 ? consumer.power : consumer.disabledPower;
            this.tab_agents.duration[agent] = 0;
            this.tab_agents.delayToleranceRatio[agent] = consumer.delayToleranceRatio;
            // console.log("agent.delayToleranceRatio", consumer, consumer.delayToleranceRatio);
        }
        for (const producer of this.nodeContent['producers']) {
            var agent = producer.agentName;
            var current_time = getDefaultTime();
            console.log("#producer", agent,  producer.endDate, producer.endDate.substring(19,25), this.default_timezone);
            this.tab_agents.beginTime[agent] = getDefaultTime();
            console.log("endtime="+ this.tab_agents.beginTime[agent]);
            this.tab_agents.endTime[agent] = producer.endDate.substring(10,16);// getDefaultTime2(60);
            //this.tab_agents.endTimeZone[agent] = producer.endDate.substring(19,25);
            this.tab_agents.power[agent] = producer.power > 0 ? producer.power : producer.disabledPower;
            this.tab_agents.duration[agent] = 0;
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
    if(!consumer.local) {
      return "";
    } else if(missingPower <= 0.001) {
      return "";
    } else if(missingPower < available ) {
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
  fnum3(num, displayZero=false) {
    return fnum3(num, displayZero);
  }

addAgent() {
  this.agentInputForm.beginDate = timeHMtoDate(this.agentInputForm.beginTime);
  this.agentInputForm.endDate = timeHMtoDate(this.agentInputForm.endTime);
  this.agentInputForm.environmentalImpact = 2;
  if(this.agentInputForm.deviceCategory.value == 'EXTERNAL_ENG') {
    this.agentInputForm.environmentalImpact = 3;
  }
  console.log("addAgent", this.agentInputForm.beginTime, this.agentInputForm.beginDate);
  console.log("addAgent : agentInputForm=",  this.agentInputForm );
  if(this.agentInputForm.power>0 && this.agentInputForm.endTime !='') {
    this.httpClient.post(this._constant.baseAppUrl+'energy/addAgent',  this.agentInputForm
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


generate_json_date(sTime1) {
  var sTime2 = sTime1.trimLeft();
  //console.log("generate_json_date #" + sTime1 + "#" + sTime2 )
  if (sTime2.length == 5) {
    sTime2 = sTime2 + ":00";
  }
  var result = this.default_date + " " + sTime2  + this.default_timezone;
  console.log("generate_json_date : entry = ", sTime1, " result = ", result);
  return result;
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

 // agent.beginDate = timeHMtoDate(this.tab_agents.beginTime[agentName]);
  agent.beginDate = this.generate_json_date(this.tab_agents.beginTime[agentName]);
  //var sEndTime = this.tab_agents.endTime[agentName];
  agent.endDate = this.generate_json_date(this.tab_agents.endTime[agentName]);
  //alert("FOO");
  console.log("save_agent : agent.beginDate = ", agent.beginDate , " agent.endDate = ", agent.endDate);
  agent.power = this.tab_agents.power[agentName];
  console.log("save_agent : agent.power = ", agent.power);
  agent.delayToleranceRatio = this.tab_agents.delayToleranceRatio[agentName];
  if(agent.delayToleranceRatio==null) {
    agent.delayToleranceRatio = 0;
  }

  var agentType = agent.agentType;
  if (typeof agentType === 'string') {
    agent.agentType = {"value":agentType}
  }
  if(agent.power>0)  {
    console.log("save_agent : targetAction = ", this.targetAction, "endDate=", agent.endDate, "agentType = ", agent.agentType);
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

setAgentEndTime(event) {
    console.log("set_consumer_endTime", event, event.target.power);
    this.agentInputForm.beginDate = timeHMtoDate(this.agentInputForm.beginTime);
    this.agentInputForm.endDate = this.agentInputForm.beginDate;
    this.agentInputForm.endDate.setTime(this.agentInputForm.beginDate.getTime() + 60 * this.agentInputForm.duration * 1000 );
    this.agentInputForm.endTime = formatTime(this.agentInputForm.endDate);
    this.agentInputForm.delayToleranceMinutes = this.agentInputForm.duration;
    console.log("set_consumer_endTime", this.agentInputForm.beginDate.getTime(), this.agentInputForm.beginTime, this.agentInputForm.duration, this.agentInputForm.endTime);
  }

  toogleDisplay(spanId) {
    return toogleDisplay(spanId,  'display_yes',  'display_none');
  }

  getNodeLocationLabel(nodeLocation) {
    var prefix = "neighbor"
    if(this.defaultNodeLocation["name"] == nodeLocation.name) {
      prefix = "home";
    }
    return prefix+" " + nodeLocation.name;
  }

  changeAgentType(agentType) {
    console.log("changeAgentType : agentType = ", agentType);
    this.agentInputForm["deviceCategory"]["value"] = null;
    console.log(this.agentInputForm["deviceCategory"]);
    var isProducer = (agentType=='PRODUCER');
    var isComsumer = (agentType=='CONSUMER');
    var oListDeviceCategoryConsumer = document.getElementById("span_listDeviceCategoryConsumer");
    if(oListDeviceCategoryConsumer != null) {
      oListDeviceCategoryConsumer.className = isComsumer ? "display_yes" : "display_none";
    }
    var oListDeviceCategoryProducer = document.getElementById("span_llistDeviceCategoryProducer");
    if(oListDeviceCategoryProducer != null) {
      oListDeviceCategoryProducer.className = isProducer ? "display_yes" : "display_none";
    }
    var oTolerance = document.getElementById("span_delayToleranceMinutes");
    if(oTolerance != null) {
      oTolerance.className = isComsumer ? "display_yes" : "display_none";
    }
    var oPriority = document.getElementById("span_priority");
    if(oPriority != null) {
      oPriority.className = isComsumer ? "display_yes" : "display_none";
    }
  }
}
