/*
 * 程式名稱：DIToEOC.java
 * 中文名稱：
 * 作者：
 * 建立日期：2006/05/16 08:00:18
 * 版本：
 * 修改日期：2013/08/30	17:21	銘輝
 **/
package service.http;
  
//抓來源IP,strlog寫在method內用回傳的方式接值
import java.io.*;
import java.sql.ResultSet;
import java.text.DecimalFormat;

import javax.servlet.*;
import javax.servlet.http.*;

import base.ConnectionProvider;
import base.EnvCtx;

import com.mds.mdsDB;
//import com.sun.xml.internal.txw2.Document;








import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import sun.rmi.runtime.Log;
 

public class DT_Case_KPI_Upload extends HttpServlet {

	private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("TTask");
	public void doGet(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {

		doPost(arg0, arg1);
	}
    String title = "消防署KPI上傳服務";
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		base.MdsDbExtend g_mdsDB = null;
		String result = "";
		try {
			java.util.ArrayList js_tableData = new java.util.ArrayList();
			java.util.ArrayList rsList;
			String j_sqlCommand = "";
			String CaseData = "";
			String strSendUrl = "";
			java.sql.ResultSet rs = null;
			java.sql.ResultSet rs1 = null;

			String strDate = "";
			String xml = "";

			//先找出上傳網址
			j_sqlCommand = "SELECT SET_VALUE FROM DIS_CONFIG WHERE SET_NAME='DT_CASEKPI_URL'";
			g_mdsDB = base.ConnectionProvider.getMdsDb();
			rs = g_mdsDB.exeQuery(j_sqlCommand);
			if(rs.next()){
				strSendUrl = rs.getString("SET_VALUE");
			}
			if(!strSendUrl.trim().equals("")){
				StringBuffer sourFileName = new StringBuffer();
				String strFileName = "119CaseKPI_"+ com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+ ".xml";
	    	    sourFileName.append(base.EnvCtx.getSystemRealPath())
	    	    .append(java.io.File.separator)
		        .append("upload")
		        .append(java.io.File.separator)
		        .append("DT_CASEKPI_DAY")
		        .append(java.io.File.separator)
		        .append(strFileName);
	    	    
				xml = "<?xml version=" + "\"" + "1.0" + "\"" + "  encoding=" + "\"" + "UTF-8" + "\"" + " ?>"+"\r\n";
				xml +="<CASEDATA>"+"\r\n";
				xml +="<CITYCODE>"+base.EnvCtx.get("City")+"</CITYCODE>"+"\r\n";

				
				String Date1 = com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss");

				js_tableData = g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(getQuerySql()));
				if (js_tableData.size() > 0) {

					for (int i = 0; i < js_tableData.size(); i++) {
						rsList = (java.util.ArrayList)js_tableData.get(i);
						xml += "<REC>" ;
						xml += "<CS_NO>"   +((rsList.get(0)==null)?"":rsList.get(0).toString())+"</CS_NO>";
						xml += "<IN_TIME>" +((rsList.get(1)==null)?"":rsList.get(1).toString())+"</IN_TIME>";
						xml += "<CS_PLACE>"+((rsList.get(2)==null)?"":rsList.get(2).toString())+"</CS_PLACE>";
						xml += "<DIS_TIME>"+((rsList.get(3)==null)?"":rsList.get(3).toString())+"</DIS_TIME>";
						xml += "<CS_KIND>" +((rsList.get(4)==null)?"":rsList.get(4).toString())+"</CS_KIND>";
						xml += "<CS_CODE>" +((rsList.get(5)==null)?"":rsList.get(5).toString())+"</CS_CODE>";
						xml += "<S1>"      +((rsList.get(6)==null)?"":rsList.get(6).toString())+"</S1>";
						xml += "<S2>"      +((rsList.get(7)==null)?"":rsList.get(7).toString())+"</S2>";
						xml += "<S3>"      +((rsList.get(8)==null)?"":rsList.get(8).toString())+"</S3>";
						xml += "<S4>"      +((rsList.get(9)==null)?"":rsList.get(9).toString())+"</S4>";
						xml += "<USRID>"   +((rsList.get(10)==null)?"":rsList.get(10).toString())+"</USRID>";
						xml += "<DEPTNO>"  +((rsList.get(11)==null)?"":rsList.get(11).toString())+"</DEPTNO>";
						if(i==(js_tableData.size()-1))
							xml += "</REC>";
						else
							xml += "</REC>"+"\r\n";
					}
					xml +="<UPLOADTIME>"+com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+"</UPLOADTIME>"+"\r\n";
					xml +="<UPLOADMAN>系統定期上傳</UPLOADMAN>"+"\r\n";
					xml +="</CASEDATA>";
					//xml = xml.replaceAll("\r\n", "");
				}

logger.info(title+"==="+getNowTime()+"xml上傳內容組合完畢準備輸出檔案\r\n");
				
				
//System.out.println(strlog);
				java.io.OutputStreamWriter test = new java.io.OutputStreamWriter(new FileOutputStream(sourFileName.toString()),"utf8");
				test.write(xml);
				test.close();
logger.info(title+"==="+getNowTime()+"所產生的檔案名稱及位置為"+base.EnvCtx.getSystemRealPath()+"\r\n");
				String results  = "";
				String results2 = "";
				try{	
					results2 =this.Http_To_119(xml,strSendUrl);

					results = results2;
				}catch(Exception e){
logger.info(title+"==="+getNowTime()+"上傳EMIC.method Exception======"+getErrorInfoFromException(e)+"\r\n");	
					e.printStackTrace();	
				}
logger.info(title+"==="+"results====================="+results);
				rs.close();
			}else{
logger.info(title+"===strSendUrl空白,不做上傳====");
			}

			//ArrayList data = root.getChildren("DATA", root.getNamespace());
		} catch (Exception ex) {

			ex.printStackTrace();
			result = "FAIL";
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
					request.setCharacterEncoding("UTF-8");
					response.setContentType("text/html;charset=utf-8");
					java.io.PrintWriter out = response.getWriter();

					out.print(result);
					out.flush();
					out.close();
				} catch (IOException ex) {
					// throw ex;
				}
			}
			
		}
	}
	
		private String getNowTime(){
			String strNowTime = "";
			try{
				strNowTime = com.mds.mdsDateTime.getNowDateTime("yyyy-MM-dd HH:mm:ss")+"*******";
				return strNowTime;
			}catch(Exception e){

			}finally{
				return strNowTime;
			}
			
		}
		 public String Http_To_119( String  xml119 ,String urlpath){
		    	
				java.net.URL url = null;
				java.net.HttpURLConnection httpConnection = null;
				StringBuffer resultData = null;
				String strResponseXML=null;
				String HTTP_METHOD = "POST";
				String returnResultValue="";
				String ReturnXMLString ="";
				try{
					url = new java.net.URL(urlpath);
					httpConnection = (java.net.HttpURLConnection) url.openConnection();
		            httpConnection.setDoInput(true);
		            httpConnection.setDoOutput(true);
		            httpConnection.setUseCaches(false);
		            httpConnection.setDefaultUseCaches(false);
		            java.net.HttpURLConnection.setFollowRedirects(true);
		            httpConnection.setRequestMethod(HTTP_METHOD);
		            httpConnection.setConnectTimeout(10000);

					
		            

					OutputStreamWriter wr = null;
					wr = new OutputStreamWriter(httpConnection.getOutputStream(),"UTF-8");
					//wr.write("inData="+xml119 );
					wr.write(xml119 );
					wr.flush();
					wr.close();
					HttpConnectionManagerParams params = new HttpConnectionManagerParams();
					params.setConnectionTimeout(30000); // 設定TIME OUT時間(毫秒)

					MultiThreadedHttpConnectionManager m = new MultiThreadedHttpConnectionManager();
					m.setParams(params);
					HttpClient httpClient = new HttpClient(m);
					
					PostMethod postMethod = new PostMethod(urlpath);
			        // 要送出的資料
					String DISASTER119DATATYPE_XML = xml119;

					StringRequestEntity sre = new StringRequestEntity(DISASTER119DATATYPE_XML, "text/xml", "UTF-8");
					postMethod.setRequestEntity(sre);
					httpClient.executeMethod(postMethod);
					
					String tempSTR="";
					int statusCode = postMethod.getStatusCode();
logger.info(title+"==="+getNowTime()+"與"+urlpath+"連線狀態測試狀態======"+statusCode+"\r\n");
					if (!(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_ACCEPTED)) {
						// 連線失敗 ==> 紀錄錯誤訊息
						System.out.println("連線失敗");
						ReturnXMLString = "<ReturnData><ReturnData>EMIC無法提供服務，錯誤代碼："+statusCode+"</ReturnData>"
								+"<ReturnValue>error</ReturnValue></ReturnData>";
logger.info(title+"==="+getNowTime()+"連線失敗 ********* to 119 result: connection fail."+"\r\n");
		//logger.info(strlog);
		//logger.info("連線失敗 ********* to EMIC result: connection fail.");
					} else {
						// 傳送XML資料成功 ==> 取得回覆訊息
						InputStream is = postMethod.getResponseBodyAsStream();
						//System.out.println("資料成功B:\n"+resultData);
						
						Writer writer = new StringWriter();
						char[] buffer = new char[1024];
						try {
			                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			                int n;
			                while ((n = reader.read(buffer)) != -1) {
			                    writer.write(buffer, 0, n);
			                }
			            } finally {
			                is.close();
			            }
			            strResponseXML = writer.toString();
						//System.out.println("資料成功A tempSTR:\n"+strResponseXML);
					}
		            /////////////////
					
			
		            //strResponseXML = resultData.toString();
logger.info(title+"==="+getNowTime()+"119回傳上傳 結果 xml:"+strResponseXML+"\r\n");
				}catch(Exception e){
logger.info(title+"==="+getNowTime()+"fail====="+"\r\n"+getErrorInfoFromException(e));
					e.printStackTrace();
				}
				return strResponseXML;
		    	
		    }
		 
		    //輸出e.printStackTrace()
		    private  String getErrorInfoFromException(Exception e) {  
		        try {  
		            StringWriter sw = new StringWriter();  
		            PrintWriter pw = new PrintWriter(sw);  
		            e.printStackTrace(pw);  
		            return  "\r\n"+sw.toString();  
		        } catch (Exception e2) {  
logger.info(title+"==="+getNowTime()+"輸出e.printStackTrace()發生錯誤====="+getErrorInfoFromException(e2)+"\r\n");
		            return "bad getErrorInfoFromException";  
		        }  
		    }  
		    
		    private String getQuerySql() throws Exception{
				
				StringBuffer sb = new StringBuffer();
				String strDate = com.mds.mdsDateTime.getNowDateTime("yyyyMMdd");
				//火災調整為60秒,且火災排除預通告
				sb.append(" select a.CS_NO,a.IN_TIME, ");
				sb.append(" a.CS_PLACE,a.DIS_TIME, ");
				sb.append(" a.CS_KIND,a.CS_CODE,b.S1,b.S2,b.S3,b.S4,(select NAME from EMPL where USERID=a.USRID) NAME, ");
				sb.append(" (select DEPTNAME from DEPT where DEPTNO=(select DEPTNO from EMPL where USERID=a.USRID)) DEPTNO ");
				sb.append(" from dis_case_acc a join DIS_CASE_KPI b on a.CS_NO=b.CS_NO where a.USRID!='RS106' and a.CS_KIND in ('01','02')  and ( DIS_TIME is not null or DIS_TIME!='') 	");
				//日期時間
	    		sb.append(" and substr(a.IN_TIME,0,8) ='"+strDate+"'");
		    	
		    	sb.append(" order by a.CS_NO, a.IN_TIME ");
		/*System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX====");    	
		System.out.println("===="+sb.toString());
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX====");*/
				return sb.toString();
			}		    
}
