package service.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class DisasterBasic extends HttpServlet {

    /**
     *
     */
    //private static final long serialVersionUID=1L;
    //private static final int BUFFER_SIZE=4096;
    base.MdsDbExtend _mdsDB=base.ConnectionProvider.getMdsDb();
    private org.apache.log4j.Logger logger=org.apache.log4j.Logger.getLogger("Chemical");
    private ArrayList Data01, Data02;
    boolean Truncate=true;

    /**/
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**/
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter out=null;
        String strUrl=null;
        String strReturn="";
        String strCityId=null;
        String Today, LDate, strData01, Jsonres;
        String RstrCity_0, RstrCity_1="", sql_script="";

        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-type","text/html;charset=UTF-8");
            out=response.getWriter();

            Data01=new ArrayList();
            _mdsDB=base.ConnectionProvider.getMdsDb();

           // 取參數
           Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery("SELECT SET_NAME, SET_VALUE FROM DIS_CONFIG WHERE SET_NAME='DBasicUrl'"));

           for(int i=0;i<Data01.size();i++){
               strData01="";
               Data02=new ArrayList();
               Data02=(ArrayList) Data01.get(i);
               strData01=(Data02.get(0)==null)?"":(String)Data02.get(0);
               if(strData01.equals("DBasicUrl"))
                   strUrl=(Data02.get(1)==null)?"":(String)Data02.get(1);
           }
           
           RstrCity_0=strUrl.substring(strUrl.lastIndexOf('=')+1, strUrl.length());
           if(RstrCity_0.trim().length()==0) {
               logger.info("=== City 參數為空值, 無法下載檔案 ===");
               return;
           } else if(RstrCity_0.equalsIgnoreCase("all")) {
        	   strCityId="00";
           } else {
        	   if(RstrCity_0.contains("台")) RstrCity_1=RstrCity_0.replace("台","臺");
               if(RstrCity_0.contains("臺")) RstrCity_1=RstrCity_0.replace("臺","台");

               sql_script="SELECT CNAME,CID FROM COUNTRY2 WHERE CNAME='"+ RstrCity_0 +"'";
               Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery(sql_script));
            
               if(Data01.size()==0) {
                  sql_script="SELECT CNAME,CID FROM COUNTRY2 WHERE CNAME='"+ RstrCity_1 +"'";
            	  Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery(sql_script));
               }
        	   
        	   for(int i=0;i<Data01.size();i++){
        		   strData01="";
        	       Data02=new ArrayList();
                   Data02=(ArrayList) Data01.get(i);
                   strData01=(Data02.get(0)==null)?"":(String)Data02.get(0);
        	       if(strData01.equals(RstrCity_0) || strData01.equals(RstrCity_1))
                       strCityId=(Data02.get(1)==null)?"":(String)Data02.get(1);
        	   }
               // 中文碼轉 UTF-8
               strUrl=strUrl.replace(RstrCity_0, java.net.URLEncoder.encode(RstrCity_0,"UTF8"));
               strUrl=strUrl.replace(RstrCity_1, java.net.URLEncoder.encode(RstrCity_1,"UTF8"));
           }

           logger.info("=== 防災基本資料表" + "(" + RstrCity_0 + ") ===");
           Today=com.mds.mdsDateTime.getNowDateTime("yyyyMMdd");
           LDate=com.mds.mdsDateTime.getDateDiff_ForDay("yyyyMMdd", Today, -2);
           LDate=com.mds.mdsDateTime.changeYYYYtoYY(LDate, 0, 4);

           // URL 檔案下載
           Jsonres=send(strUrl);

           logger.info("    3. 讀檔匯入資料庫");
           InsertToDB(Jsonres, strCityId);

        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {
            //回傳相關訊息
            /************************************************************************/
            logger.info("-------------------------");
            out.print(strReturn);
            out.flush();
            out.close();
            Data01.clear();
            Data02.clear();
            Truncate=true;
            this._mdsDB.disconnect();
            /************************************************************************/
        }
    }

    private String send(String strURL) {

        boolean flag=true;
        URL Url;
        String Jsonobj="";
        String strFileNamePath="";
        HttpsURLConnection conn = null;
        File newFile;

        String patternString=".*:/.*";
        String sysPath=getServletContext().getRealPath("/");
        String savePath=base.EnvCtx.getValueAsString("Disaster.download.dir").replaceAll("\\\\","/")
                       +File.separator+"Basic";
        Pattern pattern=Pattern.compile(patternString);
        Matcher matcher=pattern.matcher(savePath);

        if(!matcher.matches()) {
            savePath=sysPath + File.separator + savePath;
            newFile=new File(savePath);
        } else {
            newFile=new File(savePath);
        }

        if(!new File(newFile.getAbsolutePath()).exists()) {
            try {
                new File(newFile.getAbsolutePath()).mkdirs();
            } catch (SecurityException se) {
                logger.info("    無法建立檔案路徑");
                logger.info(se.getMessage());
                flag=false;
            }
        }

        try {
            Url = new URL(strURL);
            if("https".equalsIgnoreCase(Url.getProtocol())){
                SslTrust.ignoreSsl();
            }

            strFileNamePath=savePath+File.separator
                           +com.mds.mdsDateTime.getNowDateTime("yyyyMMdd")+".txt";
            newFile=new File(strFileNamePath);

            if(newFile.exists()){
                logger.info("    1. 本機端刪除今日日期檔案");
                newFile.delete();
            } else {
            	logger.info("    1. 本機端建立今日日期檔案");
            }

            if(!newFile.exists()) {
                conn = (HttpsURLConnection)Url.openConnection();
                InputStreamReader in=null;
                if (conn.getResponseCode() == 200)
                  in=new InputStreamReader(conn.getInputStream(),"UTF-8");
                else {
                  conn.disconnect();
                  logger.info("    * URL Connection 錯誤");
                  throw new Exception("URL Connection Error");
                }
                logger.info("    2. 下載資料");
                FileWriter fw=new FileWriter(strFileNamePath);
                int data=0;
                while (data != -1) {
                    data=in.read();
                    fw.write(data);
                    fw.flush();
                }
                fw.close();
                in.close();
                conn.disconnect();
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
            flag=false;
        } catch (Exception e) {
            logger.info(e.getMessage());
            flag=false;
        } finally {
            conn.disconnect();
            if(newFile.exists() && newFile.length()>0)
                Jsonobj=strFileNamePath;
            if(flag)
                logger.info("       ==> 下載成功");
            else
                logger.info("       ==> 下載失敗");
        }
        return Jsonobj;
    }

    private void InsertToDB(String FN, String CID) throws Exception {

        int fcount=0, dbcount=0, singleCharInt;
        char singleChar;
        boolean start=false;
        String line="";
        StringBuffer strObject= new StringBuffer();
        InputStreamReader isr=null;

        try {
            isr=new InputStreamReader(new FileInputStream(new File(FN)),"BIG5");
            while((singleCharInt=isr.read()) != -1) {
                singleChar=(char) singleCharInt;
                if(singleChar=='{') start=true; //資料字串包: 開始flag
                if(start) { // 資料字串包: 開始
                    if(singleChar!='{' && singleChar!='}')
                        strObject.append(singleChar);
                    // 如字元是 }, 則一筆資料結束
                    if(singleChar=='}') {
                        start=false;
                        line=strObject.toString().replaceAll("\":", "=");
                        fcount++;
                        if(DataInputDB(line)) dbcount++;
                        strObject.setLength(0);
                    }
                }
            }
        } catch (IOException e) {
            logger.info("     ==> 讀檔失敗: "+ FN);
        } finally {
            // 記錄下載筆數
            if(fcount<1) {
                logger.info("      沒有資料");
                logger.info("筆數 :  0" );
                writeRecord(0, 0, CID);
            } else {
                logger.info("      筆數 : " + dbcount);
                writeRecord(fcount, dbcount, CID);
            }
        }
    }
    
    protected boolean DataInputDB(String line) throws SQLException {

        boolean success=true;
        String X="", Y="", strPair;
        String[] XY, records, values, columns, insertValue;
        String strColumns="DISASTER_ID,VERIFY_DATE,VERIFY_ACTION,PLACE_NO,P_COMP_NAME,CITYCODE,"
        		         +"CITYNAME,P_COMP_ADDR,GIS_X,GIS_Y,ORG,ORGMSG";

        try {
            // 資料清除
            if(Truncate) {
                this._mdsDB.exeUpdate("TRUNCATE TABLE DISASTER_BASIC");
                this._mdsDB.commit();
                Truncate=false;
            }

            records=line.split("\",\"");
            columns=strColumns.split(",");
            insertValue=new String[columns.length];

            //比對欄位名稱, 給正確 insert 資料值
            for(int i=0; i<records.length; i++) {
                strPair=records[i].replaceAll("\"", "");
                if(strPair.substring(strPair.length()-1).matches("=")) strPair+=" ";
                values=strPair.split("=");
                values[1]=strPair.substring(strPair.indexOf("=")+1, strPair.length());
                if(values.length>0) {
                    if(values[0].toUpperCase().matches("GISX"))
                        X=(values[1].trim().length()==0)? "":values[1].trim();
                    if(values[0].toUpperCase().matches("GISY"))
                        Y=(values[1].trim().length()==0)? "":values[1].trim();
                    for(int o=0; o<columns.length; o++) {
                        if(values[0].toUpperCase().matches(columns[o].replaceAll("\\_", ""))) {
                            insertValue[o]=values[1].trim();
                            break;
                        }
                    }
                }
            }
            
            line="";
            for(String value: insertValue) {
                if(value==null) value="";
                line+=(line.length()==0)? "'"+value+"'":",'"+value+"'";
            }
            strColumns=strColumns+",X,Y,GEOMETRY";
            
            // 座標
            if(X.length()>0 && Y.length()>0) {
                XY=this.Cal_lonlat_To_twd97(X+","+Y).split(",");
                line+=","+XY[0]+","+XY[1]+",getpoint("+XY[0]+","+XY[1]+")";
           } else {
                line+=",'','',''";
           }
            
		   //全形轉半形
           for(char c:line.toCharArray()){
               if((int)c >= 65281 && (int)c <= 65374)
                   line=line.replace(c, (char)(((int)c)-65248));
           }
           // 資料匯入 TABLE
           this._mdsDB.exeUpdate("INSERT INTO DISASTER_BASIC("+ strColumns + ") VALUES(" + line +")");
        } catch (SQLException statement) {
              success=false;
              logger.info(statement.getMessage());
        } finally {
             if(success) {
                 this._mdsDB.commit();
             } else {
                 this._mdsDB.rollback();
                 logger.info("      ==> 匯入失敗");
                 logger.info("          "+line);
             }
        }
        return success;
    }

    private void writeRecord(int RCOUNT, int UCOUNT, String CID) throws Exception {

        Data01=new ArrayList();
        Data01=this._mdsDB.getDataArrayList(
               this._mdsDB.exeQuery("SELECT CASE WHEN MAX(SEQ) IS NULL THEN 1 ELSE MAX(SEQ)+1 END FROM DTS_RECORD"));
        String seq="";
        for(int i=0;i<Data01.size();i++){
            Data02=new ArrayList();
            Data02=(ArrayList) Data01.get(i);
            seq=(Data02.get(0)==null)?"0":Data02.get(0).toString();
        }
        //寫入資料交換紀錄表DTS_RECORD
        this._mdsDB.exeUpdate("INSERT INTO DTS_RECORD (CITY,SEQ,KIND,UPDATETIME,RCOUNT,UCOUNT,LDATE,LUSER) "
                              +"VALUES ('"+CID+"',"+seq+",'DisasterBasic','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"'"
                              +",'"+RCOUNT+"','"+UCOUNT+"','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','DT')");
    }

	private String Cal_lonlat_To_twd97(String data)
    {   
		if(data.equals("0,0")){
			//logger.info("座標0,0滾.............");
			return "0,0";
		}
		//logger.info("data========================"+data);
		String londata = data.substring(0,data.indexOf(",")-1);
		String latdata = data.substring(data.indexOf(",")+1);
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
