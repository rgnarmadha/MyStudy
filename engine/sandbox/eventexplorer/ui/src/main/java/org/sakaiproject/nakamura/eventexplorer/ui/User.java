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
 * This is the usable class for retrieving data
 * from Cassandra and providing it to JSP.
 */

package org.sakaiproject.nakamura.eventexplorer.ui;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.json.JSONObject;

import java.util.List;


public class User extends Base 
{
	public static JSONObject jUser=new JSONObject();
	public static List<String> hzlJSP;
	public static List<String> hzdlJSP;
	public static List<String> hzdhJSP;
	public static String buiJSP;
	public static int init_pixel;
	public static String current_date;
	public static int start_yr;
	public static int end_yr;
    public User(String ar[],String val[],int len) 
    {

    	boolean isUser=false;
//    	String ar[]=new String[1];
//    	String val[]=new String[1];
  //  	ar[0]="Dateid";
  //  	ar[0]="Timerange";
//    	ar[0]="Userid";
  //   	ar[0]="Event";
    	
  //  	val[0]="Jun 18 2010";
  //  	val[0]="02:59:25-02:59:27";
  //  	val[0]="[org.sakaiproject.nakamura.search.SearchServlet] Dependency Manager: Service SearchPropertyProvider registered, activate component";
//    	val[0]="User1";
    	
    	
//    	int len=ar.length;
    	String user="";
    	for(int i=0;i<len;i++)
    	{
    		if(ar[i].equalsIgnoreCase("Userid"))
    		{
    			isUser=true;
    			user=val[i];
    		}
    	}
    	
    
      String arr[]={"Dateid","Event","Timerange"};
    	String prop[]=new String[3];
    	String value[]=new String[3];
    	int k=0;
    	
    	for(int i=0;i<arr.length;i++)
    	{
    		for(int j=0;j<len;j++)
    		{
    			if(arr[i].equalsIgnoreCase(ar[j]))
    			{
    				prop[k]=ar[j];
    				value[k]=val[j];
    				k++;
    			}
    		}
    	}
    	
        Cassandra.Client client = setupConnection();

    	if(isUser==true)
    	{
    		System.out.println("Selecting the event messages from "+user+"/n");
    		selectSingleUserWithAllMessages(client,user,ar,val,len);
    	}
    	else
    	{
    		System.out.println("Selecting the event message from all users.\n");
    		selectAllUsersWithAllMessages(client,ar,val,len);
    	}
        jUser=j;									//Used in the JSP
        hzdhJSP=hzdh;
        hzdlJSP=hzdl;
        hzlJSP=hzl;
        buiJSP=BUI;
        init_pixel=init_pixels;
        current_date=cur_date;
        start_yr=start_year;
        end_yr=end_year;
        
        closeConnection();

    }

    private static void selectSingleUserWithAllMessages(Cassandra.Client client, String authorKey,String ar[],String val[], int len) {
        try {
            SlicePredicate slicePredicate = new SlicePredicate();
            SliceRange sliceRange = new SliceRange();
            sliceRange.setStart(new byte[] {});
            sliceRange.setFinish(new byte[] {});
            sliceRange.setCount(10000);
            
            slicePredicate.setSlice_range(sliceRange);

            ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY_USERS);
            List<ColumnOrSuperColumn> result = client.get_slice(KEYSPACE, authorKey, columnParent, slicePredicate, ConsistencyLevel.ONE);

            print(authorKey, result, ar, val,len);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    private static void selectAllUsersWithAllMessages(Cassandra.Client client, String ar[],String val[],int len) {
        try {
            KeyRange keyRange = new KeyRange(3);
            keyRange.setStart_key("");
            keyRange.setEnd_key("");

            SliceRange sliceRange = new SliceRange();
            sliceRange.setStart(new byte[] {});
            sliceRange.setFinish(new byte[] {});
            sliceRange.setCount(10000);

            SlicePredicate slicePredicate = new SlicePredicate();
            slicePredicate.setSlice_range(sliceRange);

            ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY_USERS);
            List<KeySlice> keySlices = client.get_range_slices(KEYSPACE, columnParent, slicePredicate, keyRange, ConsistencyLevel.ONE);

            for (KeySlice keySlice : keySlices) {
                print(keySlice.getKey(), keySlice.getColumns(), ar, val, len);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


}

