package service.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
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

/**
 * 輸出XML
 *
 */
public class AED extends HttpServlet {

    base.MdsDbExtend _mdsDB=base.ConnectionProvider.getMdsDb();
    private org.apache.log4j.Logger logger=org.apache.log4j.Logger.getLogger("AED");
    boolean Truncate=true;

    public void doGet(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        doPost(arg0, arg1);
    }

    /* 主要呼叫這支 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        String strUrl="";
        String CID=base.EnvCtx.getValueAsString("City");
        String strReturn="", strData01="", CityName_0="", CityName_1="";
        ArrayList Data02=new ArrayList();
        ArrayList Data01=new ArrayList();
        PrintWriter out=null;

        this._mdsDB=base.ConnectionProvider.getMdsDb();
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-type","text/html;charset=UTF-8");
            out=response.getWriter();

            // Get URL
            Data01=this._mdsDB.getDataArrayList(this._mdsDB.exeQuery("SELECT SET_NAME,SET_VALUE FROM DIS_CONFIG WHERE SET_NAME='AED_URL'"));
            for(int i=0;i<Data01.size();i++){
                Data02=(ArrayList) Data01.get(i);
                if(Data02.get(0).equals("AED_URL"))
                    strUrl=(Data02.get(1)==null?"":Data02.get(1).toString());
            }

            if(CID.matches("00") || CID.isEmpty()) {
                CID="00";
                logger.info("  === (消防署)檔案下載開始 ===");
            } else {
                Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery("SELECT CID,CNAME FROM COUNTRY2 WHERE CID='"+ CID +"'"));
                for(int i=0;i<Data01.size();i++){
                    strData01="";
                    Data02=(ArrayList) Data01.get(i);
                    strData01=(Data02.get(0)==null)?"":(String)Data02.get(0);
                    if(strData01.equals(CID))
                        CityName_0=(Data02.get(1)==null)?"":(String)Data02.get(1);
                }
                if(CityName_0.contains("台")) CityName_1=CityName_0.replace("台","臺");
                if(CityName_0.contains("臺")) CityName_1=CityName_0.replace("臺","台");
                logger.info("  === ("+ CityName_0 +")檔案下載開始 ===");
            }
            String Jsonres=send(strUrl);

            /* 匯入資料庫 */
            logger.info("    3.資料匯入資料庫");
            InsertToDB(Jsonres, CID, CityName_0, CityName_1);

        } catch (Exception e) {
            logger.info(e.getMessage());
        }finally{
            //回傳相關訊息
            /************************************************************************/
            logger.info("程式執行完成 ");
            logger.info("-------------------------");
            out.print(strReturn);
            out.flush();
            out.close();
            this._mdsDB.disconnect();
            /************************************************************************/
        }
    }

    private String send(String strURL) {

        boolean flag=true;
        URL Url;
        String Jsonobj="";
        String strFileNamePath="";
        HttpsURLConnection conn=null;
        File newFile=null;
        // 建立本機端下載目錄
        String savePath=createDir();

        try {
            Url=new URL(strURL);
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
                conn=(HttpsURLConnection)Url.openConnection();
                InputStreamReader in=null;
                if(conn.getResponseCode() == 200)
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
            }
        } catch (MalformedURLException e) {
             logger.info(e.getMessage());
             flag=false;
        } catch (IOException e) {
             logger.info(e.getMessage());
             flag=false;
        } catch (Exception e) {
             logger.info(e.getMessage());
             flag=false;
        } finally {
             if(newFile.exists() && newFile.length()>0)
                 Jsonobj=strFileNamePath;
             if(flag)
                 logger.info("       ==> 下載成功");
             else
                 logger.info("       ==> 下載失敗");
             conn.disconnect();
        }
        return Jsonobj;
      }

    private String createDir() {

        File newFile;
        String patternString=".*:/.*";
        String sysPath=getServletContext().getRealPath("/");
        String savePath=base.EnvCtx.getValueAsString("Monuments.dowload.dir").replaceAll("\\\\","/")
                       +File.separator+"AED";
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
            }
        }
        return newFile.getAbsolutePath();
    }

      private void InsertToDB(String FN, String CID, String CityName_0, String CityName_1) throws Exception {

          int fcount=0, dbcount=0, singleCharInt;
          char singleChar;
          boolean start=false;
          String line="";
          StringBuffer strObject= new StringBuffer();
          InputStreamReader isr=null;

          try {
              isr=new InputStreamReader(new FileInputStream(new File(FN)), "UTF-8");
              while((singleCharInt=isr.read()) != -1) {
                  singleChar=(char) singleCharInt;
                  if(singleChar=='{') start=true; //資料字串包: 開始flag
                  if(start) { // 資料字串包: 開始
                      if(singleChar!='{' && singleChar!='}')
                          strObject.append(singleChar);
                      // 如字元是 }, 則一筆資料結束
                      if(singleChar=='}') {
                          start=false;
                          line=unicodeToString(strObject.toString());
                          line=line.replaceAll("\":", "=");
                          //篩選縣市
                          if(CID=="00") {
                              fcount++;
                              if(DataInputDB(line)) dbcount++;
                          } else {
                              if(line.contains("場所縣市=\""+ CityName_0) || line.contains("場所縣市=\""+ CityName_1)) {
                                  fcount++;
                                  if(DataInputDB(line)) dbcount++;
                              }
                          }
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
          String X="", Y="", strPair, strCol="";
          String[] XY, records, values, fileColumns, tableColumns, insertValue;
          //檔案欄位名稱
          String strColumns_CHN="場所名稱,場所縣市,場所區域,場所地址,場所LAT,場所LNG,"
                               +"場所分類,場所類型,其他類型,場所描述,AED放置地點,AED地點描述,"
                               +"地點LAT,地點LNG,全年全天開放大眾使用,周一至周五起,周一至周五迄,"
                               +"周六起,周六迄,周日起,周日迄,開放使用時間備註,開放時間緊急連絡電話";
          //table欄位名稱
          String strColumns_ENG="SPACE_NAME,SPACE_COUNTY,SPACE_DISTRICT,SPACE_ADDR,"
                               +"SPACE_LAT,SPACE_LNG,SPACE_KIND,SPACE_TYPE,SPACE_OTHER_TYPE,"
                               +"SPACE_DESC,AED_LOC,AED_LOC_DESC,AED_LAT,AED_LNG,AED_OPEN_EVERYDAY,"
                               +"MON_FRI_OPEN,MON_FRI_CLOSE,SAT_OPEN,SAT_CLOSE,SUN_OPEN,SUN_CLOSE,"
                               +"OPENTIME_NOTE,OPENTIME_TEL";

          try {
              // 資料清除
              if(Truncate) {
                  this._mdsDB.exeUpdate("TRUNCATE TABLE GIS_OPENDATA_AED");
                  this._mdsDB.commit();
                  Truncate=false;
              }

              records=line.split("\",\"");
              fileColumns=strColumns_CHN.split(",");
              tableColumns=strColumns_ENG.split(",");
              insertValue=new String[fileColumns.length];

              //比對欄位名稱, 給正確 insert 資料值
              for(int i=0; i<records.length; i++) {
                  strPair=records[i].replaceAll("\"", "");
                  if(strPair.substring(strPair.length()-1).matches("=")) strPair+=" ";
                      values=strPair.split("=");

                  for(int y=0; y<values.length; y++) {
                      if(values[0].toUpperCase().matches("地點LNG"))
                          X=(values[1].trim().length()==0)? "":values[1].trim();
                      if(values[0].toUpperCase().matches("地點LAT"))
                          Y=(values[1].trim().length()==0)? "":values[1].trim();
                      for(int o=0; o<fileColumns.length; o++) {
                           if(values[0].toUpperCase().matches(fileColumns[o])) {
                               fileColumns[o]=tableColumns[o];
                               insertValue[o]=values[1].trim();
                           }
                      }
                  }
              }

              line="";
              for(String value: insertValue) {
                  if(value==null) value="";
                  line+=(line.length()==0)? "'"+value+"'":",'"+value+"'";
              }

              for(String column: fileColumns) {
                strCol+=(strCol.length()==0)? column:","+column+"";
              }
              strCol=strCol+",TWD97_X,TWD97_Y,GEOMETRY";

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
             this._mdsDB.exeUpdate("INSERT INTO GIS_OPENDATA_AED("+ strCol + ") VALUES(" + line +")");
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

          ArrayList Data01=new ArrayList();
          ArrayList Data02;
          String seq="";

          Data01=this._mdsDB.getDataArrayList(
                 this._mdsDB.exeQuery("SELECT CASE WHEN MAX(SEQ) IS NULL THEN 1 ELSE MAX(SEQ)+1 END FROM DTS_RECORD"));

          for(int i=0;i<Data01.size();i++){
              Data02=new ArrayList();
              Data02=(ArrayList) Data01.get(i);
              seq=(Data02.get(0)==null)?"0":Data02.get(0).toString();
          }

          //寫入資料交換紀錄表DTS_RECORD
          this._mdsDB.exeUpdate("INSERT INTO DTS_RECORD (CITY,SEQ,KIND,UPDATETIME,RCOUNT,UCOUNT,LDATE,LUSER) "
                                +"VALUES ('"+CID+"',"+seq+",'AED','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"'"
                                +",'"+RCOUNT+"','"+UCOUNT+"','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','DT')");
      }

    private String Cal_lonlat_To_twd97(String data)
        {
        if(data.equals("NoShape")){
          return "0,0";
        }

        String londata=data.substring(0,data.indexOf(",")-1);
        String latdata=data.substring(data.indexOf(",")+1);
        double lon=Double.parseDouble(londata);
        double lat=Double.parseDouble(latdata);
        double a=6378137.0;
        double b=6356752.314245;
        double lon0=121 * Math.PI / 180;
        double k0=0.9999;
        double x;
        double y;
        int dx=250000;
        String TWD97="";

        lon=(lon/180) * Math.PI;
        lat=(lat/180) * Math.PI;

        //---------------------------------------------------------
        double e=Math.pow((1 - Math.pow(b,2) / Math.pow(a,2)), 0.5);
        double e2=Math.pow(e,2)/(1-Math.pow(e,2));
        double n=( a - b ) / ( a + b );
        double nu=a / Math.pow((1-(Math.pow(e,2)) * (Math.pow(Math.sin(lat), 2) ) ) , 0.5);
        double p=lon - lon0;
        double A=a * (1 - n + (5/4) * (Math.pow(n,2) - Math.pow(n,3)) + (81/64) * (Math.pow(n, 4)  - Math.pow(n, 5)));
        double B=(3 * a * n/2.0) * (1 - n + (7/8.0)*(Math.pow(n,2) - Math.pow(n,3)) + (55/64.0)*(Math.pow(n,4) - Math.pow(n,5)));
        double C=(15 * a * (Math.pow(n,2))/16.0)*(1 - n + (3/4.0)*(Math.pow(n,2) - Math.pow(n,3)));
        double D=(35 * a * (Math.pow(n,3))/48.0)*(1 - n + (11/16.0)*(Math.pow(n,2) - Math.pow(n,3)));
        double E=(315 * a * (Math.pow(n,4))/51.0)*(1 - n);

        double S=A * lat - B * Math.sin(2 * lat) +C * Math.sin(4 * lat) - D * Math.sin(6 * lat) + E * Math.sin(8 * lat);

        //計算Y值
        double K1=S*k0;
        double K2=k0*nu*Math.sin(2*lat)/4.0;
        double K3=(k0*nu*Math.sin(lat)*(Math.pow(Math.cos(lat),3))/24.0) * (5 - Math.pow(Math.tan(lat),2) + 9*e2*Math.pow((Math.cos(lat)),2) + 4*(Math.pow(e2,2))*(Math.pow(Math.cos(lat),4)));
        y=K1 + K2*(Math.pow(p,2)) + K3*(Math.pow(p,4));

        //計算X值
        double K4=k0*nu*Math.cos(lat);
        double K5=(k0*nu*(Math.pow(Math.cos(lat),3))/6.0) * (1 - Math.pow(Math.tan(lat),2) + e2*(Math.pow(Math.cos(lat),2)));
        x=K4 * p + K5 * (Math.pow(p, 3)) + dx;

        TWD97=x+ "," + y;
        return TWD97;
    }

    /*
     * unicode轉中文
     */
    public static String unicodeToString(String str) {

        Pattern pattern=Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher=pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch=(char) Integer.parseInt(matcher.group(2), 16);
            str=str.replace(matcher.group(1), ch+"" );
        }
        return str;
    }
}