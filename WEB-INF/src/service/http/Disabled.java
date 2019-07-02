package service.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.Properties;
import java.util.Scanner;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Servlet implementation class DownloadFileFromFTP
 */
public class Disabled extends HttpServlet {

    private org.apache.log4j.Logger logger=org.apache.log4j.Logger.getLogger("Disabled");
    private base.MdsDbExtend _mdsDB=base.ConnectionProvider.getMdsDb();
    private Channel channel=null;
    private Session sshSession=null;
    boolean Truncate=true;

   /**
    * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

   /**
     *  從 FTP 下載弱勢團體檔案至署
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        File newFile;
        String strReturn="";
        String FileName="";
        String savePath;
        String[] CID=getServletConfig().getInitParameter("CID-Mapping").split(",");
        PrintWriter out=null;
        
        this._mdsDB=base.ConnectionProvider.getMdsDb();
        logger.info("=== (社家署)弱勢團體資料交換 ===");

        try {
             request.setCharacterEncoding("UTF-8");
             response.setCharacterEncoding("UTF-8");
             response.setHeader("content-type","text/html;charset=UTF-8");
             out=response.getWriter();

             String TWDate=com.mds.mdsDateTime.getNowDateTime("yyyyMMdd");
             TWDate=com.mds.mdsDateTime.changeYYYYtoYY(TWDate, 0, 4);
             FileName="CRIP" + TWDate + ".txt";

             /*
              ** 建立下載檔案資料夾
              ** 判斷是否絕對路徑
              ** 不是, 則建立目錄於專案目錄下
              */
             savePath=createDir();

             newFile=new File(savePath+File.separator+FileName);

             if(CID.length==1) {
                 String[] code={"00","消防署"};
                 logger.info("  === (消防署)檔案下載服務開始 ===");
                 // 檔案下載
                 if(SFTP(newFile, FileName))
                     InsertToDB(code,savePath,FileName);
             } else {
                 logger.info("  === ("+ CID[1] +")檔案下載服務開始 ===");
                 InsertToDB(CID,savePath,FileName);
             }
        } catch (Exception e) {
             return;
        } finally {
             /************************************************************************/
             logger.info("程式執行完成");
             logger.info("-------------------------");
             out.print(strReturn);
             out.flush();
             out.close();
             channel.disconnect();
             sshSession.disconnect();
             this._mdsDB.disconnect();
             /************************************************************************/
        }
   }

   private String createDir() {

       final String patternString=".*:/.*";
       final String sysPath=getServletContext().getRealPath("/");
       String savePath=base.EnvCtx.getValueAsString("Disabled.download.dir").replaceAll("\\\\","/");
       File newFile;

       Pattern pattern=Pattern.compile(patternString);
       Matcher matcher=pattern.matcher(savePath);
       if(!matcher.matches()) {
           savePath=sysPath + File.separator + savePath + File.separator + "Disabled";
           newFile=new File(savePath);
       } else {
           savePath=savePath + File.separator + "Disabled";
           newFile=new File(savePath);
       }

       if(!new File(newFile.getAbsolutePath()).exists()) {
           try {
               new File(newFile.getAbsolutePath()).mkdirs();
           } catch (SecurityException se){
               logger.info("    檔案路徑無法建立");
               logger.info(se.getMessage());
           }
       }
       return savePath;
   }

   private boolean SFTP(File newFile, String FileName) {

       final String ftpIp=base.EnvCtx.getValueAsString("Disabled.Sftp.ip");
       final String ftpId=base.EnvCtx.getValueAsString("Disabled.Sftp.id");
       final String ftpPwd=base.EnvCtx.getValueAsString("Disabled.Sftp.pwd");
       final String ftpRemotePath=base.EnvCtx.getValueAsString("Disabled.Sftp.dir");
       boolean flag=true;
       ChannelSftp sftp=null;

        if(newFile.exists()) {
           newFile.delete();
           logger.info("    1.刪除檔案 " + FileName);
       } else {
           logger.info("    1.建立檔案 " + FileName);
       }

       if(!newFile.exists()){
           logger.info("    2.下載檔案 " + FileName);
           /* SFTP 檔案下載 */
           try {
               sftp=SftpConnect(ftpId, ftpIp, ftpPwd);
               sftp.cd(ftpRemotePath);
               sftp.get(FileName, newFile.getAbsolutePath());
               // 確認檔案存在
               if(newFile.exists() && newFile.length()>0)
                 logger.info("      ==> 下載完成");
               else
                 logger.info("      ==> 無檔案");
           } catch (SftpException e) {
               logger.info("      ==> " + e.getMessage());
               flag=false;
           } finally {
               sftp.disconnect();
           }
       }
       return flag;
   }

   private ChannelSftp SftpConnect(String ftpId, String ftpIp, String ftpPwd) {

        try {
             JSch jsch=new JSch();
             jsch.getSession(ftpId, ftpIp);
             sshSession=jsch.getSession(ftpId, ftpIp);
             sshSession.setPassword(ftpPwd);
             Properties sshConfig=new Properties();
             sshConfig.put("StrictHostKeyChecking", "no");
             sshSession.setConfig(sshConfig);
             sshSession.connect();
             channel=sshSession.openChannel("sftp");
             channel.connect();
        } catch (Exception e) {
             logger.info("      ==> 連線失敗");
             channel.disconnect();
             sshSession.disconnect();
        }
        return (ChannelSftp) channel;
    }

    private void InsertToDB(String[] CID, String filePath, String FN) {

        int fcount=0, dbcount=0;
        String line=null, cID;
        String code="@"+CID[0]+"x";
        String path=filePath + File.separator;
        ArrayList<String> aCID=new ArrayList();
        BufferedReader br=null;
        InputStreamReader isr=null;
        
        if(CID[0].matches("00")) logger.info("    3.資料匯入資料庫");
        else logger.info("    1.資料匯入資料庫");
        /* 資料檔解密 */
        if(!CID[0].matches("00")) {
        	if(findString(path+FN, code)) { // 是否解密過
                decrypt(path+FN,path+"tmp"+FN,code.length());
                logger.info("      ==> 檔案完成解密");
            } else if(findString(path+FN, "@[0-9]{10}x$")) {
        	    logger.info("      ==> 檔案無法解密");
        	    return;
            } else {
            	logger.info("      ==> 檔案不需解密");
            }
        }
      
        try {
        	if(CID[0].matches("00")) isr=new InputStreamReader(new FileInputStream(new File(path+FN)),"UTF8");
        	else isr=new InputStreamReader(new FileInputStream(new File(path+FN)));
            br=new BufferedReader(isr);
            
            while ((line=br.readLine()) != null) {
                fcount++;
                if(line.trim().length()>0)
                    if(DataInputDB(line)) dbcount++;
                if(CID[0].matches("00")) saveFileByCountyID(path, FN, line, aCID);
            }
        } catch (IOException e) {
             logger.info("     ==> 讀檔失敗: "+ FN);
        } finally {
             if(CID[0].matches("00")) {
                 getEncrypt(aCID);
                 cID=CID[0];
             } else {
                 cID=get3digitCID(CID[1]);
             }
             // 記錄筆數
             if(fcount<1) {
                 logger.info("      沒有資料");
                 logger.info("筆數 :  0");
                 writeRecord(0, 0, cID);
             } else {
                 logger.info("      筆數 : " + dbcount);
                 writeRecord(fcount, dbcount, cID);
             }
        }
    }

    private void saveFileByCountyID(String Path, String FN, String line, ArrayList<String> aCID) {

        BufferedWriter writer;
        String[] details=line.split(",");
        String saveCountyPath=Path+details[0]+File.separator;
        String data=Path+","+details[0]+","+FN;
        File newFile=new File(saveCountyPath);

        /* 存檔依縣市代碼 */
        if (!newFile.exists()) newFile.mkdirs();

        try {
        	newFile=new File(saveCountyPath+"tmp"+FN);
            if(newFile.exists() && !aCID.contains(data)) newFile.delete();
            writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile.getAbsolutePath(),true)));
            writer.append(line);
            writer.newLine();
            writer.flush();
            writer.close();
            if(!aCID.contains(Path+","+details[0]+","+FN))
                aCID.add(Path+","+details[0]+","+FN);
        } catch (IOException e) {
            logger.info("      ==> 縣市資料分檔失敗");
        }
    }

    // 取三碼 City ID
    private String get3digitCID(String CityName) {

        ArrayList Data01=new ArrayList();
        ArrayList Data02=new ArrayList();
        String CID="", CityName_1="", sql_script="";
        
        try {
        	if(CityName.contains("台")) CityName_1=CityName.replace("台","臺");
            if(CityName.contains("臺")) CityName_1=CityName.replace("臺","台");
            
        	sql_script="SELECT CID FROM COUNTRY2 WHERE CNAME='"+ CityName +"'";
            Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery(sql_script));
            
            if(Data01.size()==0) {
            	sql_script="SELECT CID FROM COUNTRY2 WHERE CNAME='"+ CityName_1 +"'";
            	Data01=_mdsDB.getDataArrayList(_mdsDB.exeQuery(sql_script));
            }
            
        } catch (SQLException e) {
            logger.info("ORA ERR: select COUNTRY2 " + e.getMessage());
        }

        for(int i=0;i<Data01.size();i++){
            Data02=(ArrayList) Data01.get(i);
            CID=(Data02.get(0)==null)?"":(String)Data02.get(0);
        }
        return CID;
    }

    private void getEncrypt(ArrayList<String> aCID) {

        /* 暫存檔 : 檔名更新 */
        int countyCount=aCID.size()-1;
        logger.info("    4.各縣市檔案進行加密");
        do {
            String[] fileInfo=aCID.get(countyCount).split(",");
            File tmpFN=new File(fileInfo[0]+fileInfo[1]+File.separator+"tmp"+fileInfo[2]);
            File destFN=new File(fileInfo[0]+fileInfo[1]+File.separator+fileInfo[2]);
            if (destFN.exists()) destFN.delete();
            /* 內容加密 */
            encrypt(tmpFN.getAbsolutePath(),"@"+fileInfo[1].replaceAll("\'", "")+"x");
            if(!tmpFN.renameTo(destFN))
                logger.info("      ==> 加密失敗:" + "tmp" +fileInfo[1]);
            countyCount--;
        }while(countyCount>=0);
    }

    private void encrypt(String tmpFN, String key) {

        File file=new File(tmpFN);
        String destFile=file.getPath().substring(0,file.getPath().lastIndexOf(File.separator))+File.separator+"abc";
        File dest=new File(destFile);
        InputStream in;
        OutputStream out;
        byte[] buffer=new byte[1024];
        byte[] buffer2=new byte[1024];
        int r;

        try {
            in=new FileInputStream(file);
            out=new FileOutputStream(destFile);
            while (( r= in.read(buffer)) > 0) {
                for(int i=0;i<r;i++) {
                    byte b=buffer[i];
                    buffer2[i]=b==255?0:++b;
                }
                out.write(buffer2, 0, r);
                out.flush();
            }
            out.close();
            in.close();
        } catch (IOException e) {
            logger.info("Ecrypt ERR: " + e.getMessage());
        } finally {
            file.delete();
            dest.renameTo(file);
            appendMethodA(file.getAbsolutePath(), key);
        }
    }

    private void appendMethodA(String fileName, String content) {

        try {
            RandomAccessFile randomFile=new RandomAccessFile(fileName, "rw");
            long fileLength=randomFile.length();
            randomFile.seek(fileLength);
            randomFile.writeBytes(content);
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean DataInputDB(String line) {

        boolean success=true;
        int diff=0;
        String[] XY=null, records, columns;
        String strNum="",contactNum;
        String strColumns="CID,PID,NAME,BEFORE_ROC_YEAR,BIRTHDAY,GENDER,CON_ADDR,CON_TEL,"
                         +"CON_MOBILE,KIND,KIND_LEVEL,SUBSIDY,CON_TEL_NOTE,CON_MOBILE_NOTE,"
                         +"X,Y,GEOMETRY";

        try {
            // 資料清除
            if(Truncate) {
                this._mdsDB.exeUpdate("TRUNCATE TABLE DIS_CHK_DEFECTIVE_NFA");
                this._mdsDB.commit();
                Truncate=false;
            }
            records=(line.startsWith("'"))?line.split("','"):line.split(",");
            columns=strColumns.split(",");
            Arrays.fill(columns, " ");
            // 資料匯入 TABLE
            for(int i=0; i<records.length; i++) {
            	columns[i]=(records[i].trim().startsWith("'")) ? records[i].trim().replaceFirst("'", ""):records[i].trim();
            	columns[i]=(columns[i].endsWith("'")) ? columns[i].substring(0, columns[i].length()-1):columns[i];

                if(i==6) { // 地址
                    columns[i]=records[i].trim().replaceAll("\\s+","").replaceAll("'", "");
                    XY=getXY(columns[i]).split(",");
                    columns[14]=(XY[0].trim().length()>0) ? XY[0]:"";
                    columns[15]=(XY[0].trim().length()>0) ? XY[1]:"";
                    columns[16]=(XY[0].trim().length()>0 && XY[1].trim().length()>0) ? "getpoint("+XY[0]+","+XY[1]+")":"";
                }

                if(i==7 || i==8) {
                    contactNum=records[i].trim().replaceFirst("^\\(","").replaceFirst("\\)","");
                    if(i==7) {
                        columns[12]=records[i].replaceAll("\\s+"," ").replaceAll("'", "");
                        if(contactNum.matches("\\d{2}-\\d{4}-\\d{4}|\\d{2}-\\d{4}-\\d{3}|\\d{2}-\\d{3}-\\d{4}|"
                                              +"\\d{2}-\\d{3}-\\d{3}|\\d{3}-\\d{3}-\\d{3}|\\d{4}-\\d{3}-\\d{3}"))
                            contactNum=contactNum.replaceAll("\\-","");
                        contactNum=contactNum.replaceFirst("\\-","").replaceAll("\\轉|\\分機|\\*", "#").trim();
                        if(contactNum.matches("\\d{2}\\s+\\d+")) contactNum=contactNum.replaceAll("\\s+","");
                    }
                    if(i==8) {
                        columns[13]=records[i].replaceAll("\\s+"," ").replaceAll("'", "");
                        if(contactNum.replaceAll("[^0-9]","").length()>10) {
                            if(!contactNum.matches("\\d{10}-\\d{10}")) contactNum=contactNum.replaceAll("\\-","");
                            if(contactNum.matches("\\d{10}\\d{10}")) contactNum=contactNum.substring(0,10);
                        } else {
                            contactNum=contactNum.replaceAll("[^0-9]","");
                        }
                    }

                    if(contactNum.trim().length()>20 || !contactNum.matches("\\d+|\\d+?\\#\\d+")) {
                        int x=0,e=0,s=0;
                        for(char c: contactNum.toCharArray()) {
                            x++;
                            if(x==1 && !Character.isDigit(c)) {
                                s=x;
                                strNum+="";
                                continue;
                            }
                            if(x<21 && Character.isDigit(c)) {
                                strNum+=c;
                            } else {
                                if(c=='#') strNum+=c;
                                else {e=x-1; break;}
                            }
                        }
                        strNum=(e<s) ? records[i].substring(s,s):contactNum.substring(s,e);
                    } else {
                        strNum=contactNum.trim();
                    }
                    columns[i]=strNum.replaceAll("\\#+","#").replaceAll("[^0-9~#]","");
                }
                
                if(i>=9 && records[i].length()!=2 && records[9].isEmpty()) {
                	diff++;
                } else {
                	records[i-diff]=records[i];
                }
            }

            line="";
            for(String value: columns) {
                if(value==null) value="";
                if(value.contains("getpoint"))  line+=","+value+"";
                else line+=(line.length()==0)? "'"+value.replaceAll("'", "")+"'":",'"+value.replaceAll("'", "")+"'";
                
            }
            //全形轉半形
            for(char c:line.toCharArray()){
                if((int)c >= 65281 && (int)c <= 65374)
                    line=line.replace(c, (char)(((int)c)-65248));
            }
            this._mdsDB.exeUpdate("INSERT INTO DIS_CHK_DEFECTIVE_NFA("+ strColumns +") VALUES(" + line +")");

        } catch (SQLException e) {
            success=false;
            logger.info("Insert Values: " + line);
            logger.info("ORA ERR: " + e.getMessage());
        } finally {
            try {
                if(success) {
                    this._mdsDB.commit();
                } else {
                    this._mdsDB.rollback();
                }
            } catch (SQLException e) {
                logger.info("ORA ERR: commit/rollback " + e.getMessage());
            }
        }
        return success;
    }

    private boolean findString(String file, String searchString) {

        boolean result=false;
        Scanner in=null;
        File f=new File(file);

        Pattern pattern = Pattern.compile(searchString);
        Matcher matcher;
        try {
            in=new Scanner(new FileReader(f));
            while(in.hasNextLine() && !result) {
            	matcher=pattern.matcher(in.nextLine());
            	while(matcher.find()) {
                    result=(!matcher.group().isEmpty()) ? true:false;
            	}
            }
        } catch (IOException e) {
            logger.info("Decrypt ERR: search KEY " + e.getMessage());
        }
        in.close();
        return result;
    }

    private void decrypt(String temFN, String destFN, int keyLength) {

        File srcFile=new File(temFN);
        File trgFile=new File(destFN);
        InputStream is;
        OutputStream out;
        byte[] buffer=new byte[1024];
        byte[] buffer2=new byte[1024];
        byte bMax=(byte)255;
        long size=srcFile.length()-keyLength;
        int mod=(int) (size%1024);
        int div=(int) (size>>10);
        int count=mod==0?div:(div+1);
        int k=1, r;

        try {
            is=new FileInputStream(temFN);
            out=new FileOutputStream(destFN);
            while ((k <= count && ( r=is.read(buffer)) > 0)) {
                if(mod != 0 && k==count) r= mod;
                for(int i=0;i<r;i++) {
                    byte b=buffer[i];
                    buffer2[i]=b==0?bMax:--b;
                }
                out.write(buffer2, 0, r);
                k++;
            }
            out.close();
            is.close();
        } catch (IOException e) {
            logger.info("Decrypt ERR: " + e.getMessage());
        } finally {
            if(srcFile.exists()) srcFile.delete();
            if(!srcFile.exists()) trgFile.renameTo(srcFile);
        }
    }

    private void writeRecord(int RCOUNT, int UCOUNT, String CID) {

        ArrayList Data01=new ArrayList();
        ArrayList Data02=new ArrayList();
        String seq="";

        try {
            Data01=this._mdsDB.getDataArrayList(
                   this._mdsDB.exeQuery("SELECT CASE WHEN MAX(SEQ) IS NULL THEN 1 ELSE MAX(SEQ)+1 END FROM DTS_RECORD"));

            for(int i=0;i<Data01.size();i++){
                Data02=(ArrayList) Data01.get(i);
                seq=(Data02.get(0)==null)?"0":Data02.get(0).toString();
            }

            //寫入資料交換紀錄表DTS_RECORD
            this._mdsDB.exeUpdate("INSERT INTO DTS_RECORD (CITY,SEQ,KIND,UPDATETIME,RCOUNT,UCOUNT,LDATE,LUSER) "
                                  +"VALUES ('"+CID+"',"+seq+",'Disabled','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"'"
                                  +",'"+RCOUNT+"','"+UCOUNT+"','"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"','DT')");
        } catch (Exception e) {
            logger.info("ORA ERR: insert  DTS_RECORD " + e.getMessage());
        }
    }

    private String getXY(String addr) {

        ArrayList Data01=new ArrayList();
        ArrayList Data02=new ArrayList();
        String X=" ",Y=" ", Addr_0="", Addr_1="", sql_script="";

        try {
        	Addr_0=parseAddr(addr.replaceAll("'", ""));
        	if(Addr_0.startsWith("台")) Addr_1=Addr_0.replaceFirst("台","臺");
            if(Addr_0.startsWith("臺")) Addr_1=Addr_0.replaceFirst("臺","台");

            sql_script="SELECT X_COORD, Y_COORD FROM GIS_MAP_ADDRESSPOINT WHERE FULLADDRES='" + Addr_0 + "'";
            Data01=this._mdsDB.getDataArrayList(this._mdsDB.exeQuery(sql_script));
            
            if(Data01.size()==0 && !Addr_1.isEmpty()) {
            	sql_script="SELECT X_COORD, Y_COORD FROM GIS_MAP_ADDRESSPOINT WHERE FULLADDRES='" + Addr_1 + "'";
            	Data01=this._mdsDB.getDataArrayList(this._mdsDB.exeQuery(sql_script));
            }
            	
            for(int i=0;i<Data01.size();i++){
                Data02=(ArrayList) Data01.get(i);
                X=(Data02.get(0)==null)?"0":Data02.get(0).toString();
                Y=(Data02.get(1)==null)?"0":Data02.get(1).toString();
            }

        } catch (SQLException e) {
            logger.info("GIS XY ERR: " + e.getMessage());
        }
        return X+","+Y;
    }

    private String parseAddr(String addr) {

        String pattern="(?<zipcode>(^\\d{5}|^\\d{3})?)"
                      +"(?<city>\\D+?[縣市])"
                      +"(?<region>\\D+?(市區|鎮區|鎮市|[鄉鎮市區]))?"
                      +"(?<village>\\D+?[村里])?"
                      +"(?<neighbor>\\d+[鄰])?"
                      +"(?<road>\\D+?(村路|[路街道段]))?"
                      +"(?<section>\\D?段)?"
                      +"(?<lane>\\d+巷)?"
                      +"(?<alley>\\d+弄)?"
                      +"(?<no>\\d+號?)?"
                      +"(?<seq>-\\d+?(號))?"
                      +"(?<floor>\\d+樓)?"
                      +"(?<others>.+)?";
        Pattern r=Pattern.compile(pattern);
        Matcher m=r.matcher(addr);
        if(m.find()) {
            addr=addr.substring(0,addr.indexOf("號")+1).trim();
            addr=(m.group("zipcode")==null) ? addr:addr.replaceAll(m.group("zipcode"), "");
            addr=(m.group("neighbor")==null) ? addr:addr.replaceAll(m.group("neighbor"), "");
        } else {
            addr="";
        }
        return addr;
    }
}
