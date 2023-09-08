import { Component, OnInit, OnChanges, ViewChild, ElementRef, Input, ViewEncapsulation } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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

@Component({
  selector: 'app-sghistory',
  templateUrl: './sghistory.component.html',
  styleUrls: ['./sghistory.component.scss'],
  encapsulation: ViewEncapsulation.None
})

export class SGHistoryComponent implements OnInit {
  @Input() private dataMax : Array<any>;
  @Input() private dataRequested : Array<any>;
  @Input() private dataProduced : Array<any>;
  @Input() private dataProvided : Array<any>;
  @Input() private dataConsumed : Array<any>;
  @Input() private dataAvailable : Array<any>;
  @Input() private dataMissing : Array<any>;
  @Input() private dataMinMissingRequest : Array<any>;
  @Input() private chartData: Array<any>;
  @ViewChild('myModal') myModal;


  private minDate = new Date();
  private maxDate = new Date();
  private deltaTime = 0;
  private nodeTotalHistory = [];
  private nodeTotalHistoryDisplay = [];
  private maxWarningDuration = 0;
  private cumulativeWarningDuration = 0;
  private cumulativeWarningEnergy = 0;
  private cumulativeProducedEnergy = 0;
  private cumulativeDuration = "";
  private maxWarningDurationDate = null;
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
  maxDisplayTime = new Date();
  private captionYPos = [];

  constructor(private httpClient: HttpClient,private _constant: ConstantsService, public _chartElem: ElementRef) {
    this.httpClient.get(this._constant.baseAppUrl+'energy/nodeTotalHistory').
      subscribe((res :any[])=> {
        this.nodeTotalHistory=res;
        console.log("this.httpClient.get : nodeTotalHistory = ", this.nodeTotalHistory );
        this.dataMax = [];
        this.dataMissing = [];
        this.dataRequested = [];
        this.dataProduced = [];
        this.dataProvided = [];
        this.dataConsumed = [];
        this.dataAvailable = [];
        this.dataMinMissingRequest = [];
        this.minDate = null;
        this.maxWarningDuration = 0;
        this.cumulativeWarningDuration = 0;
        this.cumulativeWarningEnergy = 0;
        this.cumulativeProducedEnergy = 0;
        var currentWarningDuration = 0;
        var lastDate = null;
        var lastMaxWarningDuration = 0;
        var listWarningDuration = [];
        var date = null;
        this.nodeTotalHistoryDisplay = [];
        for(var i = 0; i < this.nodeTotalHistory.length; i++) {
            var obj = this.nodeTotalHistory[i];
            var nbEvents = obj.linkedEvents.length;
            if(nbEvents>0) {
              var rowspan = nbEvents;
              for(var idxEvent = 0; idxEvent < obj.linkedEvents.length; idxEvent++) {
                  var event =  obj.linkedEvents[idxEvent];
                  //console.log("step1111 event = ", event, event.type);
                  //var nextRow = copyNodeTotal(obj);
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
          if(this.minDate==null) {
            this.minDate = date;
          }
          //console.log("offers", obj.offers);
          //console.log("date=", date);
          var time = formatTime2(new Date(obj.date));
          var max = Math.max(obj.requested, obj.produced, obj.available);
          this.dataMissing.push([date, obj.missing]);
          this.dataRequested.push([date, obj.requested]);
          this.dataProduced.push([date, obj.produced]);
          this.dataProvided.push([date, obj.provided]);
          this.dataAvailable.push([date, obj.available]);
          this.dataConsumed.push([date, obj.consumed]);
          var minRequestMissing = 0;
          if(obj.available > obj.minRequestMissing && obj.minRequestMissing > 0) {
            minRequestMissing = obj.minRequestMissing;
          }
          this.dataMinMissingRequest.push([date, minRequestMissing]);
          this.dataMax.push([date, max]);
          if(date.getTime() < this.minDate.getTime()) {
            this.minDate = date;
          }
          if(date.getTime() >  this.minDate.getTime()) {
            this.maxDate = date;
          }
          if(obj.maxWarningDuration > this.maxWarningDuration) {
            this.maxWarningDuration = obj.maxWarningDuration;
            this.maxWarningDurationDate = date;
          }
          if(lastDate != null && (date.getTime() > lastDate.getTime())) {
            var deltaSec =  (date.getTime() - lastDate.getTime())/1000;
            this.cumulativeProducedEnergy+= deltaSec * obj.produced;
            this.cumulativeWarningEnergy+= deltaSec * obj.sumWarningPower;
            //console.log(this.cumulativeWarningEnergy, this.cumulativeProducedEnergy);
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
        this.cumulativeWarningDuration = 0;
        for(var i = 0; i < listWarningDuration.length; i++) {
          this.cumulativeWarningDuration+=listWarningDuration[i];
        }
        console.log("minDate", this.minDate, "maxDate", this.maxDate, this.maxDate.getTime(), "maxWarningDuration", this.maxWarningDuration);
        this.deltaTime = this.maxDate.getTime() - this.minDate.getTime();
        var deltaTimeTotalSec = this.deltaTime/1000;
        var deltaTimeSec = deltaTimeTotalSec % 60;
        var deltaTimeMin = (deltaTimeSec/ 60) % 60;
        //var testDate = timeHMtoDate();
        console.log("deltaTime = " + this.deltaTime);

        console.log("this.dataMax", this.dataMax);
        this.chartContainer = _chartElem;
        console.log("this.chartContainer", this.chartContainer);
        this.createChart();
        if (this.dataMax) {
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
    this.totalWidth = Math.max(1800,5*this.deltaTime/1000);
    this.totalHeight = 700;
    this.nbTicks =  Math.max(10,this.totalWidth/100);
    this.width = this.totalWidth - this.margin.left - this.margin.right;
    this.height = this.totalHeight - this.margin.top - this.margin.bottom;
    this.yMinPos = this.height;
  }
  createChart() {
    console.log("createChart2 deltaTime = ", this.deltaTime);
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
    let xDomain = this.dataMax.map(d => d[0]);

    //let xDomain = [this.minDate, this.maxDate];
    //console.log("xDomain", xDomain, this.minDate, this.maxDate);

    let yDomain = [0, d3.max(this.dataMax, d => d[1])];

    // create scales
    //this.xScale = d3.scaleBand().padding(0.1).domain(xDomain).rangeRound([0, this.width]);
    this.xScale = d3.scaleTime()
      .domain( [this.minDate, this.maxDate])
      .range([0, this.width]);

    //console.log("xScale minDate: ", this.xScale(this.minDate));
    //console.log("xScale maxDate: ", this.xScale(this.maxDate));


    this.yScale = d3.scaleLinear().domain(yDomain).range([this.yMinPos, 0]);

    // bar colors
    this.colors = d3.scaleLinear().domain([0, this.dataMax.length]).range(<any[]>['red', 'blue']);

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


  updateChart() {
    console.log("updateChart begin_", this.dataMax, this.dataMissing , this.xScale);
    console.log("test1", this.xScale(this.minDate),  this.xScale(this.maxDate));

    // update scales & axis
    this.yScale.domain([0, d3.max(this.dataMax, d => d[1])]);
    this.colors.domain([0, this.dataMax.length]);
    this.yAxis.transition().call(d3.axisLeft(this.yScale));

    let updateBar = this.chart.selectAll('.bar').data(this.dataMax);
    updateBar.exit().remove();

    let updateLine = this.chart.selectAll('.line').data(this.dataMax);
    updateLine.exit().remove();

    // remove exiting bars

    console.log("updateChart step1");
    //this.displayLine2(this.dataMinMissingRequest, "black", "Min Missing");
    this.displayBar2(this.dataMinMissingRequest, "rgb(252, 133, 133)", "Min Missing");

    this.displayLine2(this.dataRequested, "rgba(9, 53, 175, 0.712)", "Requested");
    this.displayLine2(this.dataMissing, "rgb(252, 0, 0)", "Missing");
    this.displayLine2(this.dataAvailable, "rgb(76, 248, 142)", "Available");
    this.displayLine2(this.dataProduced, "darkgreen", "Produced");
    //this.displayLine(update, this.dataConsumed, "grey");
    // Display max warning duration
    let warningPercent = 100*1000*this.cumulativeWarningDuration / this.deltaTime;
    let svg = d3.select("svg");
    svg.append('text')
      .attr("fill", "black")
      .style("font-size", "12px")
      .attr("x",10)
      .attr("y", 10)
      .text(""
        + " Cumulative warning duration : " + this.formatDuration(1000*this.cumulativeWarningDuration) + "  / " + this.formatDuration(this.deltaTime)
        + "  (" + this.fnum2(warningPercent) + " %)"
        + " max = " + this.maxWarningDuration + " sec at " + this.disaplyTime(this.maxWarningDurationDate))
    ;
    svg.append('text')
      .attr("fill", "black")
      .style("font-size", "12px")
      .attr("x",10)
      .attr("y", 30)
      .text(""
          + " Cumulative warning energy " + this.fnum2(this.cumulativeWarningEnergy/3600 ) + " WH"
          + " (" + this.fnum2(100* this.cumulativeWarningEnergy / this.cumulativeProducedEnergy) + " % )"
          + "  Cumulative produced : " + this.fnum2(this.cumulativeProducedEnergy/ 3600000) + " KWH  "
         )
    ;
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
    console.log("formatDuration ", ss, mm, hh);
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
    var sOffer = "(" + nextOffer.id + ") " + nextOffer.issuer + "->" + nextOffer.request.issuer
     + (nextOffer.isComplementary?" [COMPLEMENTARY] ": "")
      + " W=" + nextOffer.power
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