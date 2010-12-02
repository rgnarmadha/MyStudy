<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<%@ page import="org.json.JSONObject" %>
	<%@ page import="org.json.JSONArray" %>
	<%@ page import="cass.User" %>	
	<%@ page import="cass.Base" %>	
   	<%@ page import="java.util.ArrayList" %>	
    <%@ page import="java.util.List" %>	
        
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!-- 
/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
-->
<html>

<head>
   <!-- See http://developer.yahoo.com/yui/grids/ for info on the grid layout -->
   <title>Sakai Event Explorer</title>
   <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

   <!-- See http://developer.yahoo.com/yui/ for info on the reset, font and base css -->
   <link rel="stylesheet" href="http://yui.yahooapis.com/2.7.0/build/reset-fonts-grids/reset-fonts-grids.css" type="text/css">
   <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.7.0/build/base/base-min.css"> 

   <!-- Load the Timeline library after reseting the fonts, etc -->
   <script src="http://static.simile.mit.edu/timeline/api-2.3.0/timeline-api.js?bundle=true" type="text/javascript"></script>
 
   <link rel="stylesheet" href="local_example.css" type="text/css">

   <!-- Since we don't have our own server, we do something tricky and load our data here as if it were a library file -->
   <script src="local_data.js" type="text/javascript"></script>
  
<%! JSONObject j;%> 
<%!List<String> hzdl,hzdh,hzl;%>
<%! String bui, current_date;%>
<%! int interval_pixels, start_year, end_year;%>
<%!String userid,dat,evnt,lowTime,highTime; %>

<%!
String ar[]=new String[4];
String val[]=new String[4];
int k;
%>

<%
userid= request.getParameter("userid");
dat=request.getParameter("date");
evnt=request.getParameter("event");
lowTime=request.getParameter("lowTime");
highTime=request.getParameter("highTime");


k=0;
for(int i=0;i<ar.length;i++)
{
	ar[i]=null;
	val[i]=null;
}
if(!(userid==null))
{
	ar[k]="Userid";
	val[k]=userid;
	k++;
}
if(!(dat==null))
{
	ar[k]="Dateid";
	val[k]=dat;
	k++;
}
if(!(evnt==null))
{
	ar[k]="Event";
	val[k]=evnt;
	k++;
}
if(!(lowTime==null)||!(highTime==null))
{
if(lowTime.equals(""))
{
	lowTime="00:00:00";
}
else if(highTime.equals(""))
{
	highTime="00:00:00";
}
ar[k]="timerange";
val[k]=""+lowTime+"-"+highTime+"";
k++;
}
%>

<%


User u=new User(ar,val,k);
j=u.jUser;

hzdl=u.hzdlJSP;
hzdh=u.hzdhJSP;
hzl=u.hzlJSP;
bui=u.buiJSP;
current_date=u.current_date;
interval_pixels=u.init_pixel;
start_year=u.start_yr;
end_year=u.end_yr;
%>

   <script>  
		var zon=new Array();		//The array for storing all hotzones
		var bui;					//Basic Unit of Interval
		bui=Timeline.DateTime.YEAR;
		
		var bu="<%=bui%>"
		var intervalPixel=<%=interval_pixels%>				//The interval of pixels
		var cur_date="<%=current_date%>"					//The starting date for timeline view
		var st_yr=<%=start_year%>							//The starting year of timeline
		var end_yr=<%=end_year%>							//The ending year of timeline
		
		//Calculating the Basic Unit of Interval based on parameter recieved from User class
		if(bu=="YEAR")
		{
		bui=Timeline.DateTime.YEAR;
		}
		if(bu=="MONTH")
		{
		bui=Timeline.DateTime.MONTH;
		}
		if(bu=="WEEK")
		{
		bui=Timeline.DateTime.WEEK;
		}
		if(bu=="DAY")
		{
		bui=Timeline.DateTime.DAY;
		}
		if(bu=="HOUR")
		{
		bui=Timeline.DateTime.HOUR;
		}
		
		//lowd array stores all the 'start' dates for the hotzones
		var lowd = new Array(
		<%
		for(int i = 0; i < hzdl.size(); i++) 
		{
			out.print("\""+hzdl.get(i)+"\"");
			if(i+1 < hzdl.size()) 
			{
				out.print(",");
			}
		}
		%>);
		
		//highd array stores all the 'end' dates for the hotzones
		var highd = new Array(
		<%
		for(int i = 0; i < hzdh.size(); i++) 
		{
			out.print("\""+hzdh.get(i)+"\"");
			if(i+1 < hzdh.size()) 
			{
				out.print(",");
			}
		}
		%>);

		//lvl array stores all the interval units for the hotzones
	var lvl = new Array(
		<%
		for(int i = 0; i < hzl.size(); i++) 
		{
			out.print("\""+hzl.get(i)+"\"");
			if(i+1 < hzl.size()) 
			{
				out.print(",");
			}
		}
		%>);
		var mg = 15;					//mg is the magnifying amount for hotzones
		
		
		//Storing all the hotzones with the start, end, magnifier, and unit interval in zon array
		var i;
		for(i=0;i<lvl.length;i++)
		{
		
		var units;
		var str=lvl[i]

		if(str=="YEAR")
		{
		units=Timeline.DateTime.YEAR;
		}
		else if(str=="MONTH")
		{
		units=Timeline.DateTime.MONTH;
		}
		else if(str=="WEEK")
		{
		units=Timeline.DateTime.WEEK;
		}
		else if(str=="DAY")
		{
		units=Timeline.DateTime.DAY;
		}
		else if(str=="HOUR")
		{
		units=Timeline.DateTime.HOUR;
		}

		var v={start:    lowd[i],
                end:      highd[i],
                magnify:  mg,
                unit:     units
		
		}
		zon[i]=v;
		
		}
   
        var tl;
        function onLoad() {
            var tl_el = document.getElementById("tl");
            var eventSource1 = new Timeline.DefaultEventSource();
            
            var theme1 = Timeline.ClassicTheme.create();
            theme1.autoWidth = true; // Set the Timeline's "width" automatically.
                                     // Set autoWidth on the Timeline's first band's theme,
                                     // will affect all bands.
			var test=<%=j%>
            theme1.timeline_start = new Date(Date.UTC(st_yr, 0, 1));			//creating the start date using the start year taken from user class
            theme1.timeline_stop  = new Date(Date.UTC(end_yr, 0, 1));			//creating the end date using the end year taken from user class
            
			//test_t stores the hotzone information for timeline band
			var hzInfo={
				zones: zon,
        		eventSource:    eventSource1,
				date:           cur_date,
				width:          45, 
				intervalUnit:   bui, 
				intervalPixels: intervalPixel,
				theme:          theme1,
				layout:         'original'
			}
			
            var bandInfos = [
		Timeline.createHotZoneBandInfo(hzInfo)					//taking the input for creating band from var hzInfo
            ];
                                                            
            // create the Timeline
            tl = Timeline.create(tl_el, bandInfos, Timeline.HORIZONTAL);
            
            var url = '.'; // The base url for image, icon and background image
                           // references in the data
            eventSource1.loadJSON(test, url); // The data was stored into the 
                                                       // timeline_data variable.
            tl.layout(); // display the Timeline
        }
        
        var resizeTimerID = null;
        function onResize() {
            if (resizeTimerID == null) {
                resizeTimerID = window.setTimeout(function() {
                    resizeTimerID = null;
                    tl.layout();
                }, 500);
            }
        }
   </script>

</head>
<body onload="onLoad();" onresize="onResize();">
<div id="doc3" class="yui-t7">
   <div id="hd" role="banner">
	      <h1>Sakai Event Explorer View page</h1>
		  <p>Welcome to the Event Explorer. Here you can view all the events of any user in your network in a time-based order.
		  </p>
     <p>This page calls the User class which retrieves data from Cassandra, creates JSON objects of the data and feeds
	  it to the Simile JSP for display.</p>
     <p>The Timeline will grow automatically to fit the events. </p>
   </div>
   <div id="bd" role="main">
	   <div class="yui-g">
	     <div id='tl'></div>
	     <p>To move the Timeline: use the mouse scroll wheel, the arrow keys or grab and drag the Timeline.</p>
		 
		 </div>
	 </div>
</div>

   <div id="ft" role="contentinfo">
     <p>Thanks to the <a href=''>Simile Timeline project</a> Timeline version <span id='tl_ver'></span></p>
     <script>Timeline.writeVersion('tl_ver')</script> 
   </div>
</div>

</body>
</html>
