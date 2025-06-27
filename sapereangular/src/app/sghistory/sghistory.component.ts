import { Component, OnInit, OnChanges, ViewChild, ElementRef, Input, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

import { ConstantsService } from '../common/services/constants.service';
import * as shape from 'd3-shape';
import * as d3 from 'd3';
import { cpuUsage } from 'process';
import { of } from 'rxjs';
import {NgbModal, ModalDismissReasons} from '@ng-bootstrap/ng-bootstrap';
import { fnum2,fnum3, precise_round, formatTime, formatDate, formatTime2, timeYYYYMMDDHMtoDate, formatTimeWindow
  , getDefaultInitTime, getDefaultHour, getDefaulInitDay, getDefaultTargetTime, getDefaultTargetDay
  , getDefaultTime, getDefaultTime2, timeHMtoDate, toogleDisplay, format2D
 } from '../common/util.js';
import { isDefined } from '@angular/compiler/src/util';

@Component({
  selector: 'app-sghistory',
  templateUrl: './sghistory.component.html',
  styleUrls: ['./sghistory.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class SGHistoryComponent implements OnInit {
  @ViewChild('myModal') myModal;
  private chartData = {};
  //private minDate = new Date();
  //private maxDate = new Date();
  //private deltaTime = 0;
  private nodeTotalHistory = [];
  private nodeTotalHistoryDisplay = [];
  private total = {};
  private chartContainer: ElementRef;
  private totalWidth = 0;
  private totalHeight = 0;
  private margin: any = { top: 50, bottom: 50, left: 50, right: 20};
  private width = 0;
  private height = 0;
  private chart: any;
  private xScale: any;
  private yScale: any;
  private colors: any;
  private xAxis: any;
  private yAxis: any;
  private xAxisPos = 0;
  private yMinPos = 0;
  private nbTicks = 100;
  private maxDisplayTime = new Date();
  private captionYPos = [];
  private listAgents = [];
  private filter = {"agentName":"", "processingTimeToleranceSec":0};
  private chartVariables = ["missing",  "requested", "produced", "provided", "consumed", "available", "minMissingRequest", "max"];
  private chartActivation = {};
  //private chartActivation = {"requested":true, "missing":true, "produced":true, "provided":true, "available":true, "consumed":true};

  private displayAdditionalPower = false;

  constructor(private httpClient: HttpClient,private _constant: ConstantsService, public _chartElem: ElementRef) {
    this.chartContainer = _chartElem;
    this.refreshHistory(true);
    }

  refreshHistory(isFirstLoad) {
    //this.chartContainer.destroy();
    //this.chartContainer.nativeElement.remove();
    var serviceUrl = this._constant.baseAppUrl+'energy/nodeTotalHistory'
    let filterParams = new HttpParams();
    Object.entries(this.filter).forEach(([key, value]) => {
      filterParams = filterParams.append(key,""+value);
    });
    console.log("refreshHistory : filterParams = ", filterParams);
    this.httpClient.get(serviceUrl, { params: filterParams }).
      subscribe((res :any[])=> {
        this.nodeTotalHistory=res;
        console.log("this.httpClient.get : nodeTotalHistory = ", this.nodeTotalHistory );
        //this.dataMax = [];
        this.chartData = {};
        for(var i = 0; i < this.chartVariables.length; i++) {
          var variable = this.chartVariables[i];
          this.chartData[variable] = [];
          if(!isDefined(this.chartActivation[variable])) {
            this.chartActivation[variable] = (variable == "consumed")? false: true;
          }
        }
        var listAgentNames = []
        if(this.filter.agentName == "") {
          this.listAgents = [];
        }
        //this.minDate = null;
        //this.maxWarningDuration = 0;
        this.total = {"cumulativeWarningDuration":0
           // , "warningWS":0, "producedWS":0, "missingWS":0, "requestedWS":0
            , "warningWH":0, "producedWH":0, "missingWH":0, "requestedWH":0, "storageUsedForProdWH":0, "storageUsedForNeed":0
            , "maxWarningDurationDate":null, "warningPercent":0, "maxWarningDuration":0
            , "minDate" : null, "maxDate" : null, "deltaTime":0
          };
        var currentWarningDuration = 0;
        var lastDate = null;
        var lastMaxWarningDuration = 0;
        var listWarningDuration = [];
        var date = null;
        this.nodeTotalHistoryDisplay = [];
        this.displayAdditionalPower = false;
        for(var i = 0; i < this.nodeTotalHistory.length; i++) {
            var obj = this.nodeTotalHistory[i];
            if(obj.storageUsed > 0) {
              this.displayAdditionalPower = true;
            }
            var nbEvents = obj.linkedEvents.length;
            if(nbEvents>0) {
              var rowspan = nbEvents;
              for(var idxEvent = 0; idxEvent < obj.linkedEvents.length; idxEvent++) {
                  var event =  obj.linkedEvents[idxEvent];
                  //console.log("step1111 event = ", event);
                  //var nextRow = copyNodeTotal(obj);
                  if(this.filter.agentName == "") {
                    //console.log("agentFilter=", this.filter.agentName)
                    var agentName = event.issuer;
                    if(!listAgentNames.includes(agentName)) {
                      listAgentNames.push(agentName);
                      var deviceName = event.issuerProperties.deviceProperties.name;
                      console.log("agentName = ", agentName, "listAgentNames = ", listAgentNames, ", deviceName = " + deviceName);
                      var agentLabel = agentName + " (" + deviceName + ")";
                      var agentItem = {"value":agentName, "label":agentLabel};
                      this.listAgents.push(agentItem);
                    }
                  }
                  if(event.additionalPower > 0)  {
                    this.displayAdditionalPower = true;
                  }
                  var nextRow = JSON.parse(JSON.stringify(obj));
                  nextRow['linkedEvents'] = null;
                  nextRow['event'] = event;
                  nextRow['rowspan'] = rowspan;
                  nextRow['idxEvent'] = idxEvent;
                  this.nodeTotalHistoryDisplay.push(nextRow);
                  rowspan = 0;
              }
            } else {
                var nextRow = JSON.parse(JSON.stringify(obj));
                nextRow['linkedEvents'] = null;
                nextRow['rowspan'] = 1;
                nextRow['idxEvent'] = 0;
                this.nodeTotalHistoryDisplay.push(nextRow);
            }
        }
        console.log("nodeTotalHistoryDisplay", this.nodeTotalHistoryDisplay);

        for(var i = 0; i < this.nodeTotalHistory.length; i++) {
          var obj = this.nodeTotalHistory[i];
          if(i < 10) {
            //console.log("item",  obj.minRequestMissing);
          }
          //var item = {"date":obj.date, "power": obj.requested};
          lastDate = date;
          date = new Date(obj.date);
          /*
          if(this.total["minDate"]==null) {
            this.total["minDate"] = date;
          }*/
          //console.log("offers", obj.offers);
          //console.log("date=", date);
          var time = formatTime2(new Date(obj.date));
          var max = Math.max(obj.requested, obj.produced, obj.available);
          this.chartData['missing'].push([date, obj.missing]);
          this.chartData['requested'].push([date, obj.requested]);
          this.chartData['produced'].push([date, obj.produced]);
          this.chartData['available'].push([date, obj.available]);
          this.chartData['provided'].push([date, obj.provided]);
          this.chartData['consumed'].push([date, obj.consumed]);
          var minRequestMissing = 0;
          if(obj.available > obj.minRequestMissing && obj.minRequestMissing > 0) {
            minRequestMissing = obj.minRequestMissing;
          }
          if(this.filter.agentName != "" && obj.maxWarningDuration > 0 ) {
            minRequestMissing = obj.minRequestMissing;
          }
          this.chartData['minMissingRequest'].push([date, minRequestMissing]);
          this.chartData['max'].push([date, max]);
          if(this.total["minDate"] == null || date.getTime() < this.total["minDate"].getTime()) {
            this.total["minDate"] = date;
          }
          if(this.total["maxDate"] == null || date.getTime() >  this.total["maxDate"].getTime()) {
            this.total["maxDate"] = date;
          }
          if(obj.maxWarningDuration > this.total['maxWarningDuration']) {
            this.total['maxWarningDuration'] = obj.maxWarningDuration;
            this.total['maxWarningDurationDate'] = date;
          }
          //console.log("TTTTTTTTTESST");
          if(lastDate != null && (date.getTime() > lastDate.getTime())) {
            var deltaSec =  (date.getTime() - lastDate.getTime())/1000;
            /*
            this.total['producedWS']+= deltaSec * obj.produced;
            this.total['missingWS']+= deltaSec * obj.missing;
            this.total['requestedWS']+= deltaSec * obj.requested;
            this.total['warningWS']+= deltaSec * obj.sumWarningPower;
            */
            this.total['producedWH']+= deltaSec * obj.produced / 3600;
            this.total['missingWH']+= deltaSec * obj.missing / 3600;
            this.total['requestedWH']+= deltaSec * obj.requested / 3600;
            this.total['warningWH']+= deltaSec * obj.sumWarningPower / 3600;
            this.total['storageUsedForNeed']+= deltaSec * obj.storageUsedForNeed / 3600;
            this.total['storageUsedForProdWH']+= deltaSec * obj.storageUsedForProd / 3600;
            console.log("storageUsedForProd = ", obj.storageUsedForProd);
            //console.log("deltaMS", deltaSec, lastMaxWarningDuration, obj.maxWarningDuration);
            if(obj.maxWarningDuration >= lastMaxWarningDuration + deltaSec) {
              //currentWarningDuration = obj.maxWarningDuration;
              currentWarningDuration+=deltaSec;
            } else {
              if(currentWarningDuration > 0) {
                listWarningDuration.push(currentWarningDuration);
              }
              currentWarningDuration = 0;
              //currentWarningDuration = obj.maxWarningDuration;
            }
            //this.cumulativeWarningDuration+= obj.maxWarningDuration;
          }
          lastMaxWarningDuration =  obj.maxWarningDuration;
          //this.maxDate = date;
        }
        if(currentWarningDuration > 0) {
          listWarningDuration.push(currentWarningDuration);
        }
        console.log("listWarningDuration", listWarningDuration);
        this.total['cumulativeWarningDuration'] = 0;
        for(var i = 0; i < listWarningDuration.length; i++) {
          this.total['cumulativeWarningDuration']+=listWarningDuration[i];
        }
        console.log("minDate", this.total["minDate"], "maxDate", this.total["maxDate"], this.total["maxDate"].getTime(), "maxWarningDuration", this.total['maxWarningDuration']);
        if(this.total["minDate"] != null) {
          this.total["deltaTime"] = this.total["maxDate"].getTime() - this.total["minDate"].getTime();
          this.total['warningPercent'] = 100*1000*this.total['cumulativeWarningDuration'] / this.total["deltaTime"];
        }
        var deltaTimeTotalSec = this.total["deltaTime"]/1000;
        var deltaTimeSec = deltaTimeTotalSec % 60;
        var deltaTimeMin = (deltaTimeSec/ 60) % 60;
        //var testDate = timeHMtoDate();
        console.log("deltaTime = " + this.total["deltaTime"]);
        //console.log("this.dataMax", this.dataMax);
        console.log("this.chartContainer", this.chartContainer);
        if (isFirstLoad) {
          this.createChart();
        } else {
          this.emptyChart();
        }
        if (this.chartData['max']) {
          this.updateChart();
        }
      });
      // Set max display time
      this.maxDisplayTime.setHours(0);
      this.maxDisplayTime.setMinutes(0);
      this.maxDisplayTime.setSeconds(0);
      this.maxDisplayTime.setMilliseconds(0);
      this.maxDisplayTime.setTime(this.maxDisplayTime.getTime() + 60 * 24*60 * 1000 );
      console.log("maxDisplayTime", this.maxDisplayTime);
  }

  changeAgentFilter(event) {
    var isAgentFilterSet = (event != "");
    console.log("changeAgentFilter", event, "isAgentFilterSet", isAgentFilterSet);
    this.chartActivation["consumed"] = isAgentFilterSet;
  }

  applyFilter() {
      console.log("applyFilter : agentFilter = ", this.filter.agentName);
      this.refreshHistory(false);
    }

  getTxtClassConsumer(consumer) {
    if(consumer.hasExpired)  {
      return "";
    } else if(consumer.missingPower > 0) {
        return 'txt_warning_high';
    } else {
      return 'txt_ok';
    }
  }

  getClassMissing(missing, available) {
    if(missing == 0) {
      return '';
    } else  if(missing <available ) {
      return "warning_high";
    } else {
      return ''; //'warning_medium';
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


  getClassMissingRequests( nodeTotal) {
    return this.getClassMissing(nodeTotal.minRequestMissing, nodeTotal.available);
  }


  getClassContractDoublons(nodeTotal) {
    if(nodeTotal.contractDoublons)  {
      return "warning_high";
    }
    return "";
  }


  fnum2(num) {
    return fnum2(num);
  }

  fnum3(num) {
    return fnum3(num, false);
  }

  fnum2_minus_plus(num) {
    var preffix = (num>0 ? '+' : '');
    return preffix + this.fnum2(num);
  }

  fnum3_minus_plus(num) {
    var preffix = (num>0 ? '+' : '');
    return preffix + this.fnum3(num);
  }

  reload() {
    console.log("--- refresh page");
    location.reload()
  }


  ngOnInit() {
  }

  ngOnChanges() {
    if (this.chart) {
      this.updateChart();
    }
  }


  setDimensions() {
    this.totalWidth = Math.max(1800,5*this.total["deltaTime"]/1000);
    console.log("setDimensions : totalWidth = " , this.totalWidth);
    var maxTotalWdth = 5*1000;
    if(this.totalWidth > maxTotalWdth && true) {
      this.totalWidth = maxTotalWdth;
      console.log("setDimensions : totalWidth[2] = " , this.totalWidth);
    }
    this.totalHeight = 700;
    this.nbTicks =  Math.max(10,this.totalWidth/100);
    this.width = this.totalWidth - this.margin.left - this.margin.right;
    this.height = this.totalHeight - this.margin.top - this.margin.bottom;
    this.yMinPos = this.height;
  }


  createChart() {
    console.log("createChart2 deltaTime = ", this.total["deltaTime"]);
    let element = this.chartContainer.nativeElement;
    this.setDimensions();
    console.log("createChart", element, element.offsetWidth, element.offsetHeight);

    // append the svg object to the body of the page
    let svg = d3.select("#chart")
    .append("svg")
      .attr("width", this.totalWidth)
      .attr("height", this.totalHeight)
      /*
    .append("g")
      .attr("transform",
            "translate(" + this.margin.left + "," + this.margin.top + ")")
    */
    // chart plot area
    this.chart = svg.append('g')
      .attr('class', 'bars')
      .attr('transform', `translate(${this.margin.left}, ${this.margin.top})`);

    // define X & Y domains
    var dataMax : Array<any>;
    dataMax = this.chartData['max'];
    //var dataMax = this.dataMax;
    console.log("dataMax = ", dataMax);
    //console.log("dataMax2= ", dataMax2);
    //let xDomain = this.dataMax.map(d => d[0]);

    //let xDomain = [this.minDate, this.maxDate];
    //console.log("xDomain", xDomain, this.minDate, this.maxDate);

    let yDomain = [0, d3.max(dataMax, d => d[1])];

    // create scales
    //this.xScale = d3.scaleBand().padding(0.1).domain(xDomain).rangeRound([0, this.width]);
    this.xScale = d3.scaleTime()
      .domain( [this.total["minDate"], this.total["maxDate"] ])
      .range([0, this.width]);

    //console.log("xScale minDate: ", this.xScale(this.minDate));
    //console.log("xScale maxDate: ", this.xScale(this.maxDate));


    this.yScale = d3.scaleLinear().domain(yDomain).range([this.yMinPos, 0]);

    // bar colors
    this.colors = d3.scaleLinear().domain([0, dataMax.length]).range(<any[]>['red', 'blue']);

    this.xAxisPos = this.margin.top +0.1*this.margin.bottom + this.height;

    // x & y axis
    this.xAxis = svg.append('g')
      .attr('class', 'axis axis-x')
      .attr('transform', `translate(${this.margin.left}, ${this.xAxisPos})`)
      .call(d3.axisBottom(this.xScale)
        .ticks(this.nbTicks)
        .tickFormat(d3.timeFormat("%H:%M:%S")))
      ;

    this.yAxis = svg.append('g')
      .attr('class', 'axis axis-y')
      .attr('transform', `translate(${this.margin.left}, ${this.margin.top})`)
      .call(d3.axisLeft(this.yScale));
  }

  emptyChart() {
    d3.select("svg").selectAll("rect").remove();
    d3.select("svg").selectAll("line").remove();
    d3.select("svg").selectAll("text").remove();
  }

  updateChart() {
    var dataMax : Array<any>;
    dataMax = this.chartData['max'];
    //console.log("updateChart begin_", this.dataMax, this.dataMissing , this.xScale);
    console.log("updateChart begin_", dataMax, this.chartData['missing'] , this.xScale);
    console.log("test1", this.xScale(this.total["minDate"]),  this.xScale(this.total["maxDate"]));

    // update scales & axis
    this.yScale.domain([0, d3.max(dataMax, d => d[1])]);
    this.colors.domain([0, dataMax.length]);
    this.yAxis.transition().call(d3.axisLeft(this.yScale));

    //let updateBar = this.chart.selectAll('.bar').data(this.dataMax);
    let updateBar = this.chart.selectAll('.bar').data(this.chartData['max']);
    updateBar.exit().remove();

    let updateLine = this.chart.selectAll('.line').data(this.chartData['max']);
    updateLine.exit().remove();

    // remove exiting bars

    console.log("updateChart step1");
    //this.displayLine2(this.dataMinMissingRequest, "black", "Min Missing");
    //this.displayBar2(this.dataMinMissingRequest, "rgb(252, 133, 133)", "Min Missing");
    this.displayBar2(this.chartData["minMissingRequest"], "rgb(252, 133, 133)", "Min Missing");
    if(this.chartActivation["consumed"]) {
        this.displayBar2(this.chartData["consumed"], "rgb(138,247,253)", "Consumed");
    }
    if(this.chartActivation["requested"]) {
      this.displayLine2(this.chartData["requested"], "rgba(9, 53, 175, 0.712)", "Requested");
    }
    if(this.chartActivation["missing"]) {
      this.displayLine2(this.chartData['missing'] , "rgb(252, 0, 0)", "Missing");
    }
    if(this.chartActivation["available"]) {
      this.displayLine2(this.chartData["available"], "rgb(76, 248, 142)", "Available");
    }
    if(this.chartActivation["produced"]) {
      this.displayLine2(this.chartData["produced"], "darkgreen", "Produced");
    }
    //this.displayLine(update, this.dataConsumed, "grey");
    // Display max warning duration
    let svg = d3.select("svg");
  }

  displayCaption(data, lineColor, caption) {
    var datIndex = Math.round(data.length/5);
    var item = data[datIndex];
    var posX = this.xScale(item[0]);
    var posY = this.yScale(item[1]) + 30;
    console.log("captionYPos", this.captionYPos);
    if(this.captionYPos.indexOf(Math.round(posY/5))>=0) {
      console.log("Pos of " + caption + " Already used");
      posY = posY + 10;
    }
    let svg = d3.select("svg");
    svg.append('text')
      .attr("fill", lineColor)
      .style("font-size", "12px")
      .attr("x",posX)
      .attr("y", posY)
      .text(caption)
    ;
    this.captionYPos.push(Math.round(posY/5));
  }


  displayLine2(data, lineColor, caption) {
    console.log("displayLine2", caption);
    for(var i = 0; i < data.length; i++) {
      if(i>0) {
        let current = data[i];
        let last = data[i-1];
        let x1 = this.xScale(last[0]);
        let x2 = this.xScale(current[0]);
        let y1 = this.yScale(last[1]);
        let y2 = this.yScale(current[1]);
        //console.log("displayLine2", i, x1, x2, y1, y2);
        this.chart .append('line')
        .attr("x1", x1)
        .attr("y1", y1)
        .attr("x2", x2)
        .attr("y2", y1)
        .attr("stroke", lineColor)
        ;
        this.chart .append('line')
        .attr("x1", x2)
        .attr("y1", y1)
        .attr("x2", x2)
        .attr("y2", y2)
        .attr("stroke", lineColor)
        ;
      }
    }this.displayCaption(data, lineColor, caption);
  }




  displayBar2(data, lineColor, caption) {
    console.log("displayBar2", caption);
    for(var i = 0; i < data.length; i++) {
      if(i>0) {
        let current = data[i];
        let last = data[i-1];
        let x1 = this.xScale(last[0]);
        let x2 = this.xScale(current[0]);
        let y1 = this.yScale(last[1]);
        let y2 = this.yScale(current[1]);
        //console.log("displayBar2", y1, this.yScale(0) );
        this.chart .append('rect')
        .attr("x", x1)
        .attr("y", y1)
        .attr("width", x2-x1)
        .attr("height", - y1 + this.yScale(0))
        .attr("fill", lineColor)
        ;
      }
    }this.displayCaption(data, lineColor, caption);
  }


  displayOffers2(histoId) {
      console.log("displayOffers2", histoId);
      var divObj = document.getElementById('offers_' + histoId);
      //divObj.style.display='yes';
      if(divObj.className=='display_none') {
        divObj.className  = 'display_yes';
      } else {
        divObj.className  = 'display_none';
      }
  }

  displayAllOffers(toDisplay) {
    var listSpanOffers = document.getElementsByTagName('span');
    for (var i = 0; i < listSpanOffers.length; i++) {
      var spanOffers = listSpanOffers[i];
      console.log("displayAllOffers", i, spanOffers, spanOffers.id, spanOffers.id.substring(0,7));
      if(spanOffers.id.substring(0,7) == 'offers_') {
        spanOffers.className = toDisplay? 'display_yes' : 'display_none';
      }
    }
  }

  formatDuration(durationMS) {
    var durationSec = durationMS/1000;
    var ss = durationSec % 60;
    var mm = Math.floor((durationSec / 60) % 60);
    var hh = Math.floor((durationSec / 3600) % 24);
    //console.log("formatDuration ", ss, mm, hh);
    if(mm==0 && hh ==0) {
      return (ss + " sec.");
    }
    var result =  format2D(ss);
    if(hh>0 || mm>0 ) {
      result =  format2D(mm) + ":" + result;
    }
    if(hh>0) {
      result =  format2D(hh) + ":" + result;
    }
    return result;
  }


getClassBorderTop(nodeTotal) {
  // console.log("nodeTotal.idxEvent = ", nodeTotal.idxEvent);
  if(nodeTotal.idxEvent == 0) {
    return "next_rowbloc";
  } else {
    return "";
  }
}

  getTextMoveOver(event)  {
    var result = "";
    if(event.warningType!=null) {
      result = ""+ event.warningType + "";
    }
    if(event.comment != "") {
      result = result + " : " + event.comment;
    }
    // TODO : add comment
    return result;
  }

  getClassEventType(event) {
    if(typeof event == 'undefined' || event==null) {
      return "no_event";
    }
    var warningClassification = event.warningClassification;
    return warningClassification;
  }

  getClassWarningRequest(reqContent) {
    var warningLevel = reqContent.warningDurationSec;
    if(warningLevel >= 10) {
      return "txt_warning_veryhigh";
    }
    return "txt_warning_high";
  }

  offerToStr(nextOffer)  {
    var provider = nextOffer.issuer;
    var consumer = nextOffer.request.issuer;
    var sOffer = "(" + nextOffer.id + ") " + provider + "->" + consumer
      + (nextOffer.isComplementary?" [COMPLEMENTARY] ": "")
      + " W=" + fnum3(nextOffer.power)
      + " [" +  this.disaplyTime(nextOffer.creationTime) + " to " +  this.disaplyTime(nextOffer.deadline) + "]";
    if(nextOffer.acquitted>0) {
      sOffer = sOffer + " acquitted";
    } else {
      sOffer = sOffer + "### NOT ACQUITTED ###";
    }
    if(nextOffer.used>0) {
      sOffer = sOffer + " used at " + this.disaplyTime(nextOffer.usedTime);
    }
    if(nextOffer.accepted>0) {
      sOffer = sOffer + " accepted at " + this.disaplyTime(nextOffer.acceptanceTime);
    }
    if(nextOffer.contractEventId>0) {
      sOffer = sOffer + " contract at " + this.disaplyTime(nextOffer.contractTime);
    } else if (nextOffer.logCancel != '') {
      sOffer = sOffer + " canceled : " + nextOffer.logCancel;
    }
    if(nextOffer.log) {
      sOffer = sOffer + " log:" + nextOffer.log;
    }
    return sOffer;
  }

  getClassOffer(nextOffer) {
    if(nextOffer.accepted > 0) {
      if(nextOffer.contractEventId>0) {
        return "offer_contract_validated";
      }
      return "offer_accepted";
    } else if(nextOffer.acquitted==0) {
      return "txt_warning_high";
    }
  }
}