package service.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.*; 
import java.nio.charset.Charset;

import com.mds.mdsDateTime;
/**
 * 輸出XML
 * 
 */
public class DTS070112 extends HttpServlet {

	//private static final long serialVersionUID = 1L;
	//private final String USER_AGENT = "Mozilla/5.0";
	base.MdsDbExtend _mdsDB = base.ConnectionProvider.getMdsDb();

	private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("TTask");
	private String strMailServer = "";
	private String strMailTo = "";
	private String strMailFlag = "";
	
	
	//private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	public void doGet(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {

		doPost(arg0, arg1);
	}

	/* 主要呼叫這支 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {
		java.sql.ResultSet rs = null;// 回傳 DB 內容物
		
		java.util.ArrayList Data01 = new  java.util.ArrayList();
		java.util.ArrayList Data02;
		

		String log_title = "=====可燃性高壓氣體種類及數量介接浩展資訊服務.";
		logger.info(log_title);	

		String strUrl = "";
		String strReturn = "";//若是驗證通過準備回傳 "0"給呼叫的服務 
		java.io.PrintWriter out = null;
		String strFlag = "";
		String strData01 = "";
		String strColumnName = "";
		String strCityID = "";
		
		try {
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("content-type","text/html;charset=UTF-8"); 
			//先找是否要跟署要資料及URL
			_mdsDB = base.ConnectionProvider.getMdsDb();
			Data01 = _mdsDB.getDataArrayList(_mdsDB.exeQuery
					("select SET_NAME,SET_VALUE  from DIS_CONFIG where set_name in('SCA12Flag','SCA12Url','HznCID','SCA12String')  order by SET_NAME"));
			for(int i=0;i<Data01.size();i++){
				strData01 = "";
				Data02 = new  java.util.ArrayList();
				Data02 = (java.util.ArrayList)Data01.get(i);
				strData01 = (Data02.get(0)==null)?"":(String)Data02.get(0);
				if(strData01.equals("SCA12Flag"))
					strFlag = (Data02.get(1)==null)?"":(String)Data02.get(1);
				else if(strData01.equals("SCA12Url"))
					strUrl = (Data02.get(1)==null)?"":(String)Data02.get(1);
				else if(strData01.equals("HznCID"))
					strCityID = (Data02.get(1)==null)?"":(String)Data02.get(1);
				else if(strData01.equals("SCA12String"))
					strColumnName = (Data02.get(1)==null)?"":(String)Data02.get(1);
			}
			if(!strFlag.equals("Y")){
				logger.info(log_title+"=====================================("+strCityID+")==該縣市設定檔預設為不做資料交換=====================================");
				return;
			}
			out = response.getWriter();
			
/*			Data01 = _mdsDB.getDataArrayList(_mdsDB.exeQuery
 * 			("select SET_NAME,SET_VALUE  from DIS_CONFIG where set_name like '%SingleSignOn_For_119%' order by SET_NAME"));
			for(int i=0;i<Data01.size();i++){
				Data02 = new  java.util.ArrayList();
				Data02 = (java.util.ArrayList)Data01.get(i);
				strData01 = (Data02.get(0)==null)?"":(String)Data02.get(0);
				if(strData01.equals("SingleSignOn_For_119_MAILSERVER"))
					strMailServer = (Data02.get(1)==null)?"":(String)Data02.get(1);
				else if(strData01.equals("SingleSignOn_For_119_MAILTO"))
					strMailTo = (Data02.get(1)==null)?"":(String)Data02.get(1);
				else if(strData01.equals("SingleSignOn_For_119_MAILFLAG"))
					strMailFlag = (Data02.get(1)==null)?"":(String)Data02.get(1);
			}*/


			//String Jsonres = getServletContext().getRealPath("/")+"download\\Hydrant\\"+com.mds.mdsDateTime.getNowDateTime("yyyyMMdd")+".txt";
			//---------------------------------------------------------------------------
//			String LDate = com.mds.mdsDateTime.getNowDateTime("yyyyMMdd");
//			LDate = com.mds.mdsDateTime.getDateDiff_ForDay("yyyyMMdd", LDate, -2);
//			LDate  = com.mds.mdsDateTime.changeYYYYtoYY(LDate, 0, 4);
//			logger.info(log_title+"URL======="+strUrl+"&LDate="+LDate+"&CID="+strCityID);
//			String Jsonres = send(strUrl+"&LDate="+LDate+"&CID="+strCityID);
//			//String Jsonres = "";
//			String strData = "";
//			logger.info(log_title+"Jsonres======="+Jsonres);
//			File newFile = new File(Jsonres);
//			java.io.FileReader fr = null;
//			BufferedReader br= null;
//			if(newFile.exists()){//表示回傳資料完成
//				fr=new java.io.FileReader(Jsonres);
//				br=new BufferedReader(fr);
//				String line;
//				while ((line=br.readLine()) != null) {
//					strData +=line;
//				}
//				fr.close();
//				br.close();
//			}
			//--------------------------------------------------------------------------
logger.info(log_title+"URL======="+strUrl+"&CID="+strCityID);
			String Jsonres = send(strUrl+"&CID="+strCityID);
			//String Jsonres = "";
			String strData = "";
			File newFile = new File(Jsonres);
			java.io.FileReader fr = null;
			BufferedReader br= null;
			if(newFile.exists()){//表示回傳資料完成
				fr=new java.io.FileReader(Jsonres);
				br=new BufferedReader(fr);
				String line;
				while ((line=br.readLine()) != null) {
					strData +=line;
				}
				fr.close();
				br.close();
			}
			//--------------------------------------------------------------------------
            

			if(strData.length()<1 || strData.indexOf("查無資料")!=-1){
				logger.info("=====================沒有"+log_title+"資料=====================");
				Data01 = _mdsDB.getDataArrayList(_mdsDB.exeQuery("select case when max(seq) is null then 1 else max(seq)+1 end  from DTS_RECORD"));
				String seq = "";
				for(int i=0;i<Data01.size();i++){
					Data02 = new java.util.ArrayList();
					Data02 = (java.util.ArrayList)Data01.get(i);
					seq = (Data02.get(0)==null)?"0":Data02.get(0).toString();
				}
//寫入資料交換紀錄表DTS_RECORD
				this._mdsDB.exeUpdate("INSERT INTO DTS_RECORD (CITY,SEQ,KIND,UPDATETIME,RCOUNT,UCOUNT,LDATE,LUSER) VALUES ('"+strCityID+"',"+seq+",'NFASCA12','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','0','0','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','DT')");
				this._mdsDB.disconnect();
				return;
			}else{
				strData = strData.substring(strData.indexOf("[")+1,strData.lastIndexOf("]"));//去掉頭尾的[跟 ]
				strData = strData.substring(strData.indexOf("{")+1,strData.lastIndexOf("}"));//去掉頭尾的{跟 }
			}
				//開始拆資料以},{作區隔
				String tycgData01[] = strData.split("\\},\\{");
				String tycgData02[];
				String tycgData03[];
				String tycgData04[];
				String strtycgData01 = "";
				String strtycgData02 = "";
				

				//java.util.Map DataHMap = new HashMap();
				//java.util.Map DataMMap = new HashMap();
				//System.out.println("tycgData01.length===="+tycgData01.length);
				StringBuffer sql01 = new StringBuffer();
				StringBuffer sql02 = new StringBuffer();
				String sql = "";
				String strLocid = "";
				String strId = "";
				String AIRCOUNT = "";
				String CODEDESC = "";
				//寫入資料庫,先全部刪除後新增
				int a = 0;
				for(int i=0;i<tycgData01.length;i++){
				//for(int i=0;i<11;i++){
					tycgData01[i] = tycgData01[i].replaceAll("\"", "");
					tycgData02 = tycgData01[i].split(",");
					//DataHMap = new java.util.HashMap();
					sql01.setLength(0);
					sql02.setLength(0);
					sql = "";
					strLocid = "";
					strId = "";
					AIRCOUNT = "";
					CODEDESC = "";
					
					for(int j=0;j<tycgData02.length;j++){
					//for(int j=0;j<11;j++){
						tycgData03 = tycgData02[j].split(":");
						strtycgData01 = "";
						strtycgData02 = "";
						strtycgData01 = (tycgData03.length>1)?tycgData03[0]:"";
						strtycgData02 = (tycgData03.length>1)?tycgData03[1]:"";
						strtycgData01 = (strtycgData01==null || strtycgData01.equals("null"))?"":strtycgData01;
						strtycgData02 = (strtycgData02==null || strtycgData02.equals("null"))?"":strtycgData02;
						strtycgData01 = strtycgData01.toUpperCase();
						if(strColumnName.indexOf(","+strtycgData01+",")!=-1){
							if(strtycgData01.equals("LOCTEL")){
								sql01.append("LOCPHONE,");          sql02.append("'"+strtycgData02+"',");
							}else if(strtycgData01.equals("NAME")){
								sql01.append("NAME,");          sql02.append("'"+strtycgData02+"',");
							}else if(strtycgData01.equals("ID")){
								strId = strtycgData02 ;
								sql01.append("ID,");          sql02.append("'"+strtycgData02+"',");
							}else if(strtycgData01.equals("LOCID")){
								strLocid = strtycgData02;
								sql01.append(strtycgData01+",");          sql02.append("'"+strtycgData02+"',");
							}else if(strtycgData01.equals("AIRCOUNT")){
								AIRCOUNT = strtycgData02;
							}else if(strtycgData01.equals("CODEDESC")){
								CODEDESC = strtycgData02;
							}else if(!strtycgData01.equals("")){
								sql01.append(strtycgData01+",");          sql02.append("'"+strtycgData02+"',");
							}	
						}
												
					}		
					sql01.append("MEMO,");      sql02.append("'"+ AIRCOUNT + CODEDESC +"',");
					sql01.append("LDATE,");     sql02.append("'"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"',");
					sql01.append("LUSER");      sql02.append("'浩展'");
					
					
					
					sql = "INSERT INTO NFASCA12 ("+sql01.toString()+") VALUES ("+sql02.toString()+")";

					try{
						this._mdsDB.beginTran();
						//ID先刪除後新增
						this._mdsDB.exeUpdate("DELETE FROM NFASCA12 WHERE ID='"+strId+"'");
						a += this._mdsDB.exeUpdate(sql);
						this._mdsDB.commit();
					}catch(java.sql.SQLException e1){
						logger.info(this.getErrorInfoFromException(e1));						
						this._mdsDB.rollback();
					}		
				}
				logger.info("總筆數============"+(tycgData01.length)+"筆,成功寫入"+a+"筆");
				
				//先取得序號 DTS_RECORD  seq
				Data01 = _mdsDB.getDataArrayList(_mdsDB.exeQuery
						("select case when max(seq) is null then 1 else max(seq)+1 end  from DTS_RECORD"));
				String seq = "";
				for(int i=0;i<Data01.size();i++){
					Data02 = new  java.util.ArrayList();
					Data02 = (java.util.ArrayList)Data01.get(i);
					seq = (Data02.get(0)==null)?"0":Data02.get(0).toString();
				}
				//寫入資料交換紀錄表DTS_RECORD
				this._mdsDB.exeUpdate("INSERT INTO DTS_RECORD (CITY,SEQ,KIND,UPDATETIME,RCOUNT,UCOUNT,LDATE,LUSER) VALUES ('"+strCityID+"',"+seq+",'NFASCA12','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','"+tycgData01.length+"','"+a+"','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','DT')");

		} catch (Exception e) {
			logger.info(log_title + "====="+ getErrorInfoFromException(e));
			
			e.printStackTrace();
		}finally{
			//回傳相關訊息
			/************************************************************************/
			this._mdsDB.disconnect();
			out.print(strReturn);
			out.flush();
			out.close();
			/************************************************************************/
			
		}
	}


	public static String getErrorInfoFromException(Exception e) {
		try {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			e.printStackTrace(pw);
			return "\r\n" + sw.toString() + "\r\n";
		} catch (Exception e2) {
			return "bad getErrorInfoFromException";
		}
	}
	
	 
	
	private String send (String strurl) throws Exception {
		String Jsonobj = "";

		try {
			//資料交換系統的服務提供介面
			//stPost.setParameter("name1",new String("中文".getBytes(),stPost.getRequestCharSet()));
//System.out.println("strurl==========="+strurl);
			/*URL url = new URL(strurl);
			java.io.InputStream is=url.openStream();
			String strFileNamePath = getServletContext().getRealPath("/")+"download\\Hydrant\\"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+".txt";
			File newFile = new File(strFileNamePath);
			FileOutputStream fos=new FileOutputStream(newFile);
			while(true){
				int i=is.read();
				if(i==-1)
					break;
				fos.write(i);
			}
			is.close();
			fos.close();
			
			if(newFile.exists()){
				Jsonobj = strFileNamePath;
			}*/
			URL url = new URL(strurl);

			URLConnection conn = url.openConnection();

			conn.connect();
logger.info("開始產生檔案");
			String strFileNamePath = getServletContext().getRealPath("/")+"download\\SCA12\\"+com.mds.mdsDateTime.getNowDateTime("yyyyMMdd")+".txt";
			File newFile = new File(strFileNamePath);
			if(newFile.exists()){
logger.info("檔案存在,先刪除檔案");
				newFile.delete();
			}
			if(!newFile.exists()){
				FileWriter fw = new FileWriter(strFileNamePath);

				InputStreamReader in = new InputStreamReader(conn.getInputStream(),"UTF-8");

				int data = in.read();

				while (data != -1) {
					data = in.read();
					fw.write(data);
					fw.flush();
				}
				if(newFile.exists()){
					Jsonobj = strFileNamePath;
				}
			}
		} finally {
			// Release current connection to the connection pool once you are done
			logger.info("檔案產生完畢");
		}

		return Jsonobj;
	}
	
	
	public static String SendMail(String Flag,String mailserver,String mailto,String ErrorMessage) {
		StringBuffer mailText = new StringBuffer();
        mailText.append("消防局水源資料介接火預平台服務:\n\n  ");
        mailText.append("******************************************************************************************************\n");
        mailText.append(ErrorMessage);
        mailText.append("******************************************************************************************************");

//        String[] t_to = new String[] { "spernaza@mds.com.tw", "123"};
		//send email---
        try{
        	String[] mailtoto = mailto.split(",");
            String[] t_to = new String[mailtoto.length*2];// { "spernaza@mds.com.tw", "123"};
            for(int i=0;i<mailtoto.length;i++){
            	t_to[(2*i)] = mailtoto[i];
            	t_to[(2*i)+1] = "收件者";
            }
        	if(Flag.equals("Y")){
        		com.mds.mdsMAIL.sendMail(
        				mailserver,  //郵件伺服器
                    "service@mail.tyfd.gov.tw",   //寄件者
                    t_to,        //收件者
                    new String[] {},     //副本
                    new String[] {},    //蜜餞副本
                    "119人員登入錯誤訊息",//主旨
                    mailText.toString(),   //郵件內容
                    "", //簽名
                    new String[] { "" });   //附加檔案
        	}
            
		} catch (Exception e2) {
			return "";
		}
        return "";
	}
	private String Cal_lonlat_To_twd97(String data)
    {   
		if(data.equals("0,0")){
logger.info("座標0,0滾.............");
			return "0,0";
		}
logger.info("data========================"+data);
		String londata = data.substring(0,data.indexOf(",")-1);
		String latdata = data.substring(data.indexOf(",")+1);
		//londata = (londata.length()>8)?londata.substring(0,8):londata;
		//latdata = (latdata.length()>7)?latdata.substring(0,8):latdata;
//System.out.println("londata====="+londata);
//System.out.println("latdata====="+latdata);
		double lon = Double.parseDouble(londata);
		double lat = Double.parseDouble(latdata);
	    double a = 6378137.0;
	    double b = 6356752.314245;
	    double lon0 = 121 * Math.PI / 180;
	    double k0 = 0.9999;
	    double x;
	    double y;
	    int dx = 250000;
        String TWD97 = "";

        lon = (lon/180) * Math.PI;
        lat = (lat/180) * Math.PI;
        
        //---------------------------------------------------------
        double e = Math.pow((1 - Math.pow(b,2) / Math.pow(a,2)), 0.5);
        double e2 = Math.pow(e,2)/(1-Math.pow(e,2)); 
        double n = ( a - b ) / ( a + b );
        double nu = a / Math.pow((1-(Math.pow(e,2)) * (Math.pow(Math.sin(lat), 2) ) ) , 0.5);
        double p = lon - lon0;
        double A = a * (1 - n + (5/4) * (Math.pow(n,2) - Math.pow(n,3)) + (81/64) * (Math.pow(n, 4)  - Math.pow(n, 5)));
        double B = (3 * a * n/2.0) * (1 - n + (7/8.0)*(Math.pow(n,2) - Math.pow(n,3)) + (55/64.0)*(Math.pow(n,4) - Math.pow(n,5)));
        double C = (15 * a * (Math.pow(n,2))/16.0)*(1 - n + (3/4.0)*(Math.pow(n,2) - Math.pow(n,3)));
        double D = (35 * a * (Math.pow(n,3))/48.0)*(1 - n + (11/16.0)*(Math.pow(n,2) - Math.pow(n,3)));
        double E = (315 * a * (Math.pow(n,4))/51.0)*(1 - n);

        double S = A * lat - B * Math.sin(2 * lat) +C * Math.sin(4 * lat) - D * Math.sin(6 * lat) + E * Math.sin(8 * lat);
 
        //計算Y值
        double K1 = S*k0;
        double K2 = k0*nu*Math.sin(2*lat)/4.0;
        double K3 = (k0*nu*Math.sin(lat)*(Math.pow(Math.cos(lat),3))/24.0) * (5 - Math.pow(Math.tan(lat),2) + 9*e2*Math.pow((Math.cos(lat)),2) + 4*(Math.pow(e2,2))*(Math.pow(Math.cos(lat),4)));
        y = K1 + K2*(Math.pow(p,2)) + K3*(Math.pow(p,4));
 
        //計算X值
        double K4 = k0*nu*Math.cos(lat);
        double K5 = (k0*nu*(Math.pow(Math.cos(lat),3))/6.0) * (1 - Math.pow(Math.tan(lat),2) + e2*(Math.pow(Math.cos(lat),2)));
        x = K4 * p + K5 * (Math.pow(p, 3)) + dx;

        TWD97 = x+ "," + y;
        return TWD97;
    }
}