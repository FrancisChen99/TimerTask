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


public class DisasterToxic extends HttpServlet {

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
           Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery("SELECT SET_NAME, SET_VALUE FROM DIS_CONFIG WHERE SET_NAME='DToxicUrl'"));

           for(int i=0;i<Data01.size();i++){
               strData01="";
               Data02=new ArrayList();
               Data02=(ArrayList) Data01.get(i);
               strData01=(Data02.get(0)==null)?"":(String)Data02.get(0);
               if(strData01.equals("DToxicUrl"))
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

           logger.info("=== 毒化物" + "(" + RstrCity_0 + ") ===");
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
                       +File.separator+"Toxic";
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
        String strColumns="DISASTER_ID,AT_CNAME,AT_ENAME,AT_UNAME,TOXIC_NO,TNAME,QTY,STORE,CLASS,CONTAINER";

        try {
            // 資料清除
            if(Truncate) {
                this._mdsDB.exeUpdate("TRUNCATE TABLE DISASTER_TOXIC");
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
                if(value.contains("'")) line+=",q'["+value+"]'";
                else line+=(line.length()==0)? "'"+value+"'":",'"+value+"'";
           }
            
		   //全形轉半形
           for(char c:line.toCharArray()){
               if((int)c >= 65281 && (int)c <= 65374)
                   line=line.replace(c, (char)(((int)c)-65248));
           }
           // 資料匯入 TABLE
           this._mdsDB.exeUpdate("INSERT INTO DISASTER_TOXIC("+ strColumns + ") VALUES(" + line +")");
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
                              +"VALUES ('"+CID+"',"+seq+",'DisasterToxic','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"'"
                              +",'"+RCOUNT+"','"+UCOUNT+"','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','DT')");
    }
}
