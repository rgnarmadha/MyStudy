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
/*
 * This is a data retriever class for the Event Explorer.
 * This connects to the local Cassandra instance and 
 * retrieves data from Cassandra and converts them into
 * JSON objects for Simile timeline usage
 */

package org.sakaiproject.nakamura.eventexplorer.ui;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Base {

    protected static final String KEYSPACE = "SAKAI";
    protected static final String COLUMN_FAMILY_USERS = "Users";
    protected static final String ENCODING = "UTF8";
    protected static TTransport tr = null;
    
    public static JSONObject j;
    public static JSONArray ja;    
    public static JSONObject j1;
	
    public static long high_t=0L;
    public static long low_t=2000000000000L;
    public static List<Date> dates=new ArrayList<Date>();
    
    public static String units[]=new String[5];
    public static int level[]=new int[5];
    

    public static String BUI="";
    static int init_lvl=1;
    static Date init_dr[]=new Date[2];
    static int max_events=6;
    
    static int k=0;
    public static List<String> hzdl;
    public static List<String> hzdh;
    public static List<String> hzl;
    public static List<Date> hzd;
    public static int max;
    public static int init_pixels;
    public static String cur_date;
    public static int start_year;
    public static int end_year;
    
    public Base()
    {
    }
    
    protected static Cassandra.Client setupConnection() 			//Connection start-up block
    {
        try 
        {
            tr = new TSocket("localhost", 9160);
            TProtocol proto = new TBinaryProtocol(tr);
            Cassandra.Client client = new Cassandra.Client(proto);
            tr.open();
            return client;
        } 
        catch (TTransportException exception) 
        {
            exception.printStackTrace();
        }
        return null;
    }

    protected static void closeConnection()							//Connection terminator 
    {
        try 
        {
            tr.flush();
            tr.close();
        } 
        catch (TTransportException exception) 
        {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
	protected static void print(String key, List<ColumnOrSuperColumn> result, String ar[], String val[], int len) 
    {
    	units[0]="YEAR";
        units[1]="MONTH";
        units[2]="WEEK";
        units[3]="DAY";
        units[4]="HOUR";
        
    	level[0]=1;
        level[1]=2;
        level[2]=3;
        level[3]=4;
        level[4]=5;
	
        try 
        {
        
        	/*Initialize the JSON objects so that for every new
        	 * query to Cassandra from the JSP, fresh objects are created
        	 * and new data is processed. 
        	 */
        	
        	hzdl=new ArrayList<String>();
        	hzdh=new ArrayList<String>();
        	hzl=new ArrayList<String>();
        	hzd=new ArrayList<Date>();
        	max=0;
        	init_pixels=200;
        	cur_date="";
        	start_year=2009;
        	end_year=2011;
        	j=new JSONObject();										  
        	ja=new JSONArray();
        	long timestamp=0;
            System.out.println("Key: '" + key + "'");
                       
            
            for (ColumnOrSuperColumn c : result) 
            {
            	
            	/*
            	 * Traversing all the columns of the supercolumn
            	 * one-by-one. Each supercolumn represents an event
            	 * message and its sub-columns represents the 
            	 * properties of the message.
            	 */
            	
               	if (c.getColumn() != null) 
            	{   
               		

                    String name = new String(c.getColumn().getName(), ENCODING);
                    String value = new String(c.getColumn().getValue(), ENCODING);
                    timestamp = c.getColumn().getTimestamp();
                    System.out.println("  name: '" + name + "', value: '" + value + "', timestamp: " + timestamp);
                }
            	else if (c.getSuper_column() != null)
            	{
                	
            		
                	String desc="";
                	String mesg="";

                	j1=new JSONObject();

            	    SuperColumn superColumn = c.getSuper_column();
                    String supername=new String(superColumn.getName(), ENCODING);
                	j1.put("title", supername);

                    for (Column column : superColumn.getColumns()) 
                    {
                    	
                    	String name = new String(column.getName(),ENCODING);
                        String value = new String(column.getValue(), ENCODING);
                        
                       	if(name.equalsIgnoreCase("timestamp"))
                       	{
                       		timestamp=Long.parseLong(value);
                     
                       		if(timestamp>high_t)
                       		{
                       			high_t=timestamp;
                       		}
                       		
                       		if(timestamp<low_t)
                       		{
                       			low_t=timestamp;
                       		}
                     
                       	}
                       	
                       	if(name.equalsIgnoreCase("Message"))
                       	{
                       		mesg=value;
                       	}
                       	String nam=name.toUpperCase();
                       	
                      		desc=desc+""+nam+" : "+value+"-      -";
                       	

//                     	System.out.println("        name: '" + name + "', value: '" + value + "', timestamp: " + timestamp);
                    }   	
            
                    boolean flag=true;
            
                    Date d=new Date(timestamp);			
                    String date=d.toString();						//Fri Jun 18 02:58:50 IST 2010
                	
                    
                    String str[];
                    str=date.split(" ");
                    String start=""+str[1]+" "+str[2]+" "+str[5]+" "+str[3]+" GMT";				//Jun 18 2010 02:58:50 GMT

                    String curr_date=""+str[1]+" "+str[2]+" "+str[5];					//Jun 18 2010
                    String curr_time=""+str[3];										//02:58:50
            
                    for(int i=0;i<len;i++)
                    {
                    	if(ar[i].equalsIgnoreCase("Dateid"))
                    	{
                    		if(!curr_date.equalsIgnoreCase(val[i]))
                    		{
                    			flag=false;
                    		}
                    	}
                    	if(ar[i].equalsIgnoreCase("Event"))
                    	{
                    		if(!mesg.equalsIgnoreCase(val[i]))
                    		{
                    			flag=false;
                    		}
                    	}
                    	if(ar[i].equalsIgnoreCase("TimeRange"))
                    	{
                    		String time_ranges[]=val[i].split("-");
                    		String frstComparison=compareTime(curr_time,time_ranges[0]);
                    		String scndComparison=compareTime(time_ranges[1],curr_time);
                    		
                    		if(!(frstComparison.equalsIgnoreCase("greater")&&scndComparison.equalsIgnoreCase("greater"))&&!(frstComparison.equalsIgnoreCase("equals"))&&!(scndComparison.equalsIgnoreCase("equals")))
                    		{
                    			flag=false;
                    		}
                    	}
                    }
                    
                    if(flag==true)
                    {
                    	dates.add(d);
                    	j1.put("description", desc);
                    	j1.put("start", start);
                    	ja.put(j1);
                    	System.out.println(start);
                    	
                    }
                    
            	}   // end supercolumn
               	
            }	// end result - collection of all supercolumns from a key
        	
            Date low_range=new Date(low_t);
            Date high_range=new Date(high_t);
            BUI= calcBUI(low_range, high_range);
            
            int i=0;
            init_dr=getDateRange(dates);
            
            for(;i<units.length;i++)
            {
            	if(units[i].equalsIgnoreCase(BUI))
            	{
            		init_lvl=level[i];
            	}
            }
            createHotZones(init_lvl, dates, init_dr);
            
            cur_date=getDateString(init_dr[0]);
            start_year=init_dr[0].getYear()+1900-1;
            end_year=init_dr[1].getYear()+1900+1;
            j.put("events", ja);
        	String out=j.toString();
        	System.out.println(out);
        	System.out.println(low_range);
        	System.out.println(high_range);
        	System.out.println(BUI);
        	System.out.println(init_lvl);
        	System.out.println(init_dr[0]+" - "+init_dr[1]);
        	System.out.println(cur_date);
        	System.out.println(start_year);
        	System.out.println(end_year);
        	
        	for(int in=0;in<hzdh.size();in++)
        	{
        		System.out.println(hzdh.get(in));
        		System.out.println(hzdl.get(in));
        		System.out.println(hzd.get(in));
        		System.out.println(hzl.get(in));
        		
        	}
        	if(hzd.size()>1)
            {
            	max=getMax(init_lvl, hzd);
            }
        	for(int in=0;in<max;in++)
            {
            	init_pixels=init_pixels+100;
            }
            
        	System.out.println(max);
        	System.out.println(init_pixels);
                } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    public static String compareTime(String s1, String s2)
    {
    	String st1[]=s1.split(":");
    	String st2[]=s2.split(":");
    	
    	int a[]=new int[st1.length];
    	int b[]=new int[st2.length];
    	
    	for(int i=0;i<st1.length;i++)
    	{
    		a[i]=Integer.parseInt(st1[i]);
    	}
    	for(int i=0;i<st2.length;i++)
    	{
    		b[i]=Integer.parseInt(st2[i]);
    	}
    	
    	if(a[0]>b[0])
    	{
    		return "greater";
    	}
    	else if(a[0]<b[0])
    	{
    		
    		return "lower";
    	}
    	else if(a[0]==b[0])
    	{
    		if(a[1]>b[1])
        	{
        		return "greater";
        	}
        	else if(a[1]<b[1])
        	{
        		return "lower";
        	}
        	else if(a[1]==b[1])
        	{
        		if(a[2]>b[2])
            	{
            		return "greater";
            	}
            	else if(a[2]<b[2])
            	{
            		return "lower";
            	}
            	else if(a[2]==b[2])
            	{
            		return "equal";
            	}
        	}
    	}
    	return "not valid input";
    }
    @SuppressWarnings("deprecation")
	public static String calcBUI(Date d1, Date d2)
    {
    	if((Math.abs(d1.getDate()-d2.getDate())==0)&&(Math.abs(getWeek(d1)-getWeek(d2))==0)&&(Math.abs(d1.getMonth()-d2.getMonth())==0)&&(Math.abs(((d1.getYear()-d2.getYear())))==0))
    	{
    		return "HOUR";
    	}    	
    	else if((Math.abs(getWeek(d1)-getWeek(d2))==0)&&(Math.abs(d1.getMonth()-d2.getMonth())==0)&&(Math.abs(((d1.getYear()-d2.getYear())))==0))
    	{
    		return "DAY";
    	}
    	else if((Math.abs(d1.getMonth()-d2.getMonth())==0)&&(Math.abs(((d1.getYear()-d2.getYear())))==0))
    	{
    		return "WEEK";
    	}
    	else if(Math.abs(((d1.getYear()-d2.getYear())))==0)
    	{
    		return "MONTH";
    	}
    	if(Math.abs((d1.getYear()-d2.getYear()))>5)
    	{
    		return "YEAR";
    	}
    	
    	return "MONTH";
    	
    }
    @SuppressWarnings("deprecation")
	public static int getWeek(Date d)
    {
    	if(d.getDate()<=7)
    	{
    		return 1;
    	}
    	else if(d.getDate()>7&&d.getDate()<=14)
    	{
    		return 2;
    	}
    	else if(d.getDate()>14&&d.getDate()<=21)
    	{
    		return 3;
    	}
    	else if(d.getDate()>21&&d.getDate()<=31)
    	{
    		return 4;
    	}
    	return 0;
    }
    public static Date[] getDateRange(List<Date> d)
    {
    	Date high=new Date();
    	Date low=new Date();

    	
    	long ht=0L;
    	long lt=2000000000000L;
    	
    	for(Date da:d)
    	{
    		long t=da.getTime();
    		if(t>ht)
    		{
    			ht=t;
    			high=da;
    		}
    		if(t<lt)
    		{
    			lt=t;
    			low=da;
    		}
    	}

    	Date da[]=new Date[2];
    	da[0]=low;
    	da[1]=high;
    	return da;
    }
    
    public static String getDateString(Date d)
    {
    	String s=d.toString();
    	String st[]=s.split(" ");
    	String ds=st[1]+" "+st[2]+" "+st[5]+" "+st[3]+" GMT";
    	return ds;
    }
    public static String getLevelString(int n)
    {
    	String s="";
    	if(n==1)
    	{
    		s=s+"YEAR";
    	}
    	if(n==2)
    	{
    		s=s+"MONTH";
    	}
    	if(n==3)
    	{
    		s=s+"WEEK";
    	}
    	if(n==4)
    	{
    		s=s+"DAY";
    	}
    	if(n==5)
    	{
    		s=s+"HOUR";
    	}
    	return s;
    }
    
    @SuppressWarnings("deprecation")
	public static String getKey(Date d,int level)
    {
    	String s="";
    	if(level==1)
    	{
    		s=""+d.getYear();
    	}
    	if(level==2)
    	{
    		s=""+d.getMonth();
    	}
    	if(level==3)
    	{
    		s=""+getWeek(d);
    	}
    	if(level==4)
    	{
    		s=""+d.getDate();
    	}
    	if(level==5)
    	{
    		s=""+d.getHours();
    	}
    	 return s;
    }


    public static void createHotZones(int l, List<Date> d, Date[] dr)
    {
    	String key;
    	Date val;
    	
    	Map<String, List<Date>> map=new HashMap<String, List<Date>>();
    	
    	for(Date dat : d)
    	{
    		key=getKey(dat, l);
    		val=dat;
    		if(!(map.containsKey(key)))
    		{
    			List<Date> lst=new ArrayList<Date>();
    			lst.add(val);
    			map.put(key, lst);
    		}
    		else
    		{
    			List<Date> tmp=map.get(key);
    			tmp.add(val);
    			map.put(key, tmp);
    			
    		}
    	}
    	
    	List<List<Date>> dense=new ArrayList<List<Date>>();
    	List<List<Date>> sparse=new ArrayList<List<Date>>();
    	
    	Collection<List<Date>> c=map.values();
    	
    	for(List<Date> lists:c)
    	{
    		if(lists.size()>max_events)
    		{
    			dense.add(lists);
    		}
    		else if(lists.size()<=max_events&&lists.size()>0)
    		{
    			sparse.add(lists);
    		}
    		
    	}
    	
    	if(sparse.size()!=0)
    	{
    		if(l!=init_lvl)
    		{
    			if(sparse.size()<(map.size()/2))
    			{
    				for(List<Date> sp:sparse)
    				{
    					Date spr[]=getDateRange(sp);
    					hzdl.add(getDateString(spr[0]));
    					hzdh.add(getDateString(spr[1]));
    					hzl.add(getLevelString(l));
    					hzd.add(spr[0]);
    				}
    			}
    			else
    			{
    				hzdl.add(getDateString(dr[0]));
    				hzdl.add(getDateString(dr[1]));
    				hzl.add(getLevelString(l));
    				hzd.add(dr[0]);
    				k++;
    			}
    		}
    			
    	}
    	if(dense.size()!=0)
    	{
    		if(l!=init_lvl&&l==5)
    		{
    			for(List<Date> de:dense)
    			{
    				
    				Date der[]=getDateRange(de);
    				hzdl.add(getDateString(der[0]));
    				hzdh.add(getDateString(der[1]));
    				hzl.add(getLevelString(l));
    				hzd.add(der[0]);
    				k++;
    			}
    		}
    		else
    		{
    			for(List<Date> de:dense)
    			{
    				Date der[]=getDateRange(de);
    				createHotZones(l+1, de, der);
    			
    			}
    		}
    	}
    	
    }
    
    public static int getMax(int lvl, List<Date> d)
    {
    	int max=0;
    	int count=0;
    	long k;
    	long range=getRange(lvl);
    	System.out.println(range);
    	System.out.println(d.size());
    	for(int i=0;i<d.size()-1;i++)
    	{
    		count=0;
    		for(int j=i+1;j<d.size();j++)
    		{
    			k=Math.abs(d.get(i).getTime()-d.get(j).getTime());
    			System.out.println(k);
    			if(k<range)
    			{
    				count++;
    				if(count>max)
    				{
    					max=count;
    				}
    				k++;
    			}
    		}
    	}
    	return max;
    }
    
    public static long getRange(int lvl)
    {
    	long t=0L;
    	if(lvl==1)
    	{
    		t=1000*60*60*24*365L;
    	}
    	if(lvl==2)
    	{
    		t=1000*60*60*24*30L;
    	}
    	if(lvl==3)
    	{
    		t=1000*60*60*24*7L;
    	}
    	if(lvl==4)
    	{
    		t=1000*60*60*24*1L;
    	}
    	if(lvl==5)
    	{
    		t=1000*60*60*1L;
    	}
    	return t;
    }
    
}



/*
 * IMP: No two hotzones in each level will overlap, because they are distinct according to the unique keys of the level,
 * which represents individual interval unit. Each date hotzone date-range will be included in the key and no keys overlap.
 * Hence no hotzones overlap.
 * 
 * IMPORTANT:Some messages do not have a timestamp field. In this case, the timestamp would be taken to be 0, and hence 
 * date-range would include lowest date as Jan 1 1970 and highest date as current local date due to code structure which 
 * would cause a big problem for hotzone creation. 
 * This problem is temporarily solved by placing position of timestamp recording field.
 */
