
export function precise_round(num,decimals) {
  var sign = num >= 0 ? 1 : -1;
  return (Math.round((num*Math.pow(10,decimals)) + (sign*0.001)) / Math.pow(10,decimals)).toFixed(decimals);
}


function OLD_aux_format(rounded) {
    if(rounded.length>6) {
        var intPart = rounded.substring(0,  rounded.indexOf("."));
        var floatPart = rounded.substring( rounded.indexOf("."));
        //console.log("fnum2", rounded, rounded.indexOf("."), intPart);
        if(intPart.length>3) {
          var bigPart = intPart.substring(0,intPart.length-3);
          var smallPart = intPart.substring(intPart.length-3);
          //console.log(rounded,intPart, bigPart,  smallPart, floatPart );
          return bigPart + " " + smallPart + floatPart;
        }
      } else {
        return "" + rounded;
      }
}

export function addThousandSeparators(num, separator) {
  num = num.toString();
  var pattern = /(-?\d+)(\d{3})/;
  while (pattern.test(num)) {
      num = num.replace(pattern, "$1" + separator + "$2");
  }
  return num;
}

export function fnum(num, decimalNb, displayZero=false ) {
  if (!displayZero && Math.abs(num) <= 0.0001) {
    return "";
  } else {
    var rounded =  precise_round(num,decimalNb);
    var result1 = rounded.replace(".", ",");  // Use ',' separator for decimal part
    return addThousandSeparators(result1, " ");
  }
}

export function fnum_minus_plus(num, decimalNb, displayZero=false) {
  var preffix = (num>0 ? '+' : '');
  return preffix + fnum(num, decimalNb, displayZero);
}


export function fnum0(num, displayZero=false ) {
  return fnum(num, 0, displayZero);
}

export function fnum2(num, displayZero=false ) {
  return fnum(num, 2, displayZero);
}



export function fnum3(num, displayZero=false ) {
  return fnum(num, 3, displayZero);
}


export function formatDate(date) {
    var day = date.getDate();
    var month = 1+date.getMonth();
    var year = date.getFullYear();
    var result = year + "-" + format2D(month)  + "-" + format2D(day);
    return result;
  }

  export function formatTime( date) {
    var hh = date.getHours();
    var mm = date.getMinutes();
    var result = format2D(hh)  + ":" + format2D(mm);
    //console.log("getFormatedDate result = ", result, current.getMonth());
    return result;
  }

  export function format2D(number) {
    return ""+ ((number<10?'0':'') + number);
  }

  export function formatTime2( date) {
    var hh = date.getHours();
    var mm = date.getMinutes();
    var ss = date.getSeconds();
    var result =  format2D(hh) + ":" + format2D(mm) + ":" + format2D(ss);
    //console.log("getFormatedDate result = ", result, current.getMonth());
    return result;
  }

  export function getDefaulInitDay() {
    var defaultTime = new Date();
    return formatDate(defaultTime);
  }

  export function getDefaultInitTime() {
    var defaultTime = new Date();
    defaultTime.setTime(defaultTime.getTime());
    return formatTime(defaultTime);
  }

  export function getDefaultHour() {
    var defaultTime = new Date();
    defaultTime.setTime(defaultTime.getTime());
    return defaultTime.getHours();
  }

  export function getDefaultTargetDay() {
    var defaultTime = new Date();
    return formatDate(defaultTime);
  }

  export function getDefaultTargetTime() {
    var defaultTime = new Date();
    defaultTime.setTime(defaultTime.getTime() + (1*60*60*1000));
    return formatTime(defaultTime);
  }

  export function timeYYYYMMDDHMtoDate(datePart, timePart) {
      var result = new Date();
      var dateArray = datePart.split("-");
      var year = parseInt(dateArray[0]);
      var month = parseInt(dateArray[1]) - 1;
      var day = parseInt(dateArray[2]);
      result.setFullYear(year);
      result.setMonth(month);
      result.setDate(day);
      var timeArray = timePart.split(":");
      var hh = parseInt(timeArray[0]);
      var mm = parseInt(timeArray[1]);
      console.log("timeYYYYMMDDHMtoDate", timePart, hh, mm);
      result.setHours(hh);
      result.setMinutes(mm);
      result.setSeconds(0);
      return result;
  }

  export function formatTimeWindow(nodeTransitionMatrices) {
    var timeWindow = nodeTransitionMatrices.timeWindow;
    //console.log("formatTimeWindow : timeWindow = ", timeWindow);
    var startDate = new Date(timeWindow.startDate);
    var endDate =  new Date(timeWindow.endDate);
    //console.log("formatTimeWindow : startHour = ", timeWindow.startHour);
    var sTimeWindow = formatTime(startDate) + "-" + formatTime(endDate);
    sTimeWindow = timeWindow.startHour + "-" + timeWindow.endHour;
    return sTimeWindow;
  }

  export function getDefaultTime() {
    var current = new Date();
    return formatTime(current);
  }

  export function getDefaultTime2(shiftMinutes) {
    var current = new Date();
    current.setTime(current.getTime() + (shiftMinutes*60*1000));
    return formatTime(current);
  }

  export function timeHMtoDate(beginTime) {
    var result = new Date();
    var time = beginTime.split(":");
    var hh = parseInt(time[0]);
    var mm = parseInt(time[1]);
    console.log("setBeginDate", time, hh, mm);
    result.setHours(hh);
    result.setMinutes(mm);
    result.setSeconds(0);
    return result;
}

export function toogleDisplay(spanId, classDisplay, classHide) {
  console.log("toogleDisplay", spanId, classDisplay, classHide);
  var divObj = document.getElementById(spanId);
  if(divObj.className==classHide) {
    divObj.className  = classDisplay;
  } else {
    divObj.className  = classHide;
  }
}