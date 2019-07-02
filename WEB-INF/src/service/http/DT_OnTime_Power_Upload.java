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
 

public class DT_OnTime_Power_Upload extends HttpServlet {

	private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("TTask");
	public void doGet(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {

		doPost(arg0, arg1);
	}
    String title = "消防署即時戰力上傳服務";
    base.MdsDbExtend g_mdsDB = null;
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		
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
			j_sqlCommand = "SELECT SET_VALUE FROM DIS_CONFIG WHERE SET_NAME='DT_ONTIMEPOWER_URL'";
			g_mdsDB = base.ConnectionProvider.getMdsDb();
			rs = g_mdsDB.exeQuery(j_sqlCommand);
			if(rs.next()){
				strSendUrl = rs.getString("SET_VALUE");
			}
			if(!strSendUrl.trim().equals("")){
				StringBuffer sourFileName = new StringBuffer();
				String strFileName = "119OnTimePower_"+ com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss")+ ".xml";
	    	    sourFileName.append(base.EnvCtx.getSystemRealPath())
	    	    .append(java.io.File.separator)
		        .append("upload")
		        .append(java.io.File.separator)
		        .append("DT_ONTIME_POWER")
		        .append(java.io.File.separator)
		        .append(strFileName);
	    	    
	    	    String strDate1 = com.mds.mdsDateTime.getNowDateTime("yyyyMMdd");
				xml = "<?xml version=" + "\"" + "1.0" + "\"" + "  encoding=" + "\"" + "UTF-8" + "\"" + " ?>"+"\r\n";
				xml +="<ONTIMEPOWERDATA>"+"\r\n";
				xml +="<CITYCODE>"+base.EnvCtx.get("City")+"</CITYCODE>"+"\r\n";
				xml +="<COUNTDATE>"+strDate1+"</COUNTDATE>"+"\r\n";

				
				String strUploadTime = com.mds.mdsDateTime.getNowDateTime("yyyyMMddHHmmss");

				js_tableData = setTableData("",g_mdsDB);
				if (js_tableData.size() > 0) {

					for (int i = 0; i < js_tableData.size(); i++) {
						rsList = (java.util.ArrayList)js_tableData.get(i);
						CaseData = "";
						for (int j = 1; j < rsList.size(); j++) {
							CaseData += (rsList.get(j)==null)?"0":rsList.get(j).toString()+",";
						}
						if(CaseData.trim().length()>0){
							CaseData = CaseData.substring(0,CaseData.trim().length()-1);
						}
						xml +="<REC><DEPTNO>"+((rsList.get(0)==null)?"":rsList.get(0).toString())+"</DEPTNO><DATA>"+CaseData+"</DATA></REC>\r\n";
					}
					xml +="<UPLOADTIME>"+strUploadTime+"</UPLOADTIME>"+"\r\n";
					xml +="<UPLOADMAN>系統定期上傳</UPLOADMAN>"+"\r\n";
					xml +="</ONTIMEPOWERDATA>"+"\r\n";
					xml = xml.replaceAll("\r\n", "");
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
logger.info(title+"===呼叫Http_To_119==============\r\n");
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
			g_mdsDB.disconnect();
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
		    
		    private java.util.ArrayList setTableData(String strCarList,base.MdsDbExtend g_mdsDB) throws Exception{

		    	//strButtonType 1:可用戰力  2:已出動戰力  3:不可用戰力
		    	
		    	//臨時對象
		    	java.util.ArrayList<String> aTempList = new java.util.ArrayList<String>();
		    	java.util.ArrayList<String> aTempCarHeadList = new java.util.ArrayList<String>();
		    	
		    	//最終容器對象,此值會給this._formBean.Table1.DataList
		    	java.util.ArrayList aConList  = new java.util.ArrayList();

		    	java.util.ArrayList<java.util.ArrayList<String>> aCarList = this.getCarKind(strCarList,g_mdsDB);
		    	aTempCarHeadList.add("單位＼車種");
		    	aTempCarHeadList.add("人力");
		    	aTempList.add("戰力總計");
		    	aTempList.add("0");
		    	for (int i = 0; i < aCarList.size(); i++) {
		    		aTempCarHeadList.add(aCarList.get(i).get(0));
		    		aTempList.add("0");
				}
		    	
		    	//將第1,2行標題和總計放入容器中
		    	aConList.add((java.util.ArrayList)aTempCarHeadList.clone());
		    	aConList.add((java.util.ArrayList)aTempList.clone());
		    	
		    	
		    	String strSQL = getAllDeptSQL(g_mdsDB);
		    	java.util.ArrayList<java.util.ArrayList<String>> aDeptList 
		    		= g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(strSQL));
		    	
		    	for (int i = 0; i < aDeptList.size(); i++) {
		    		String strDeptNM = aDeptList.get(i).get(0);
		    		String strDeptNO = aDeptList.get(i).get(1);
		    		
		    		//獲取人力
		    		String strPersonCount = this.getPersonCount("1",strDeptNO,g_mdsDB);

		    		strSQL = this.getQuerySQL("1", strDeptNO,strCarList);
		    		java.util.ArrayList<java.util.ArrayList<String>> aDeptDateList 
		    		= g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(strSQL));
		    		
		    		this.setData(strDeptNM,aConList,aDeptDateList,strPersonCount);
		    		
				}
		    	
		    	if(aDeptList.size()==0) aConList = new java.util.ArrayList();
		    	
		    	
		    	return aConList;
			}
		    
		  //設置表格數據的統計值
			private void setData(String strDeptNM,java.util.ArrayList aConList,java.util.ArrayList aList,String strPersonCount){
				
				//零時存放某個分隊行記錄
				java.util.ArrayList aRowTempList = new java.util.ArrayList();
				//第一列為分隊的中文名稱
				aRowTempList.add(strDeptNM);
				//第二列為分隊的人力
				aRowTempList.add(strPersonCount);
				
				//取得標題下一行的統計行,取1，第0行為標題列了 ^^
				java.util.ArrayList aTempList  = (java.util.ArrayList)aConList.get(1);
				
				//設置人力的標題統計，人力在第二列
				int iHeadCnt = Integer.valueOf((String)aTempList.get(1)).intValue();
				aTempList.set(1, ""+(Integer.valueOf(strPersonCount).intValue()+iHeadCnt));
				
				//算出某細類畫面最后一列總計以及累加最上層總計
				for (int i = 0; i < aList.size(); i++) {
					java.util.ArrayList aRowList = (java.util.ArrayList)aList.get(i);
					int iRowCnt = Integer.valueOf((String)aRowList.get(1)).intValue();
					
					aRowTempList.add(""+iRowCnt);//添加某個分隊各個車子數量的值
					
					//取得最上層總計并累加最上總計
					iHeadCnt = Integer.valueOf((String)aTempList.get(i+2)).intValue();
					aTempList.set(i+2, ""+(iRowCnt+iHeadCnt));
					
				}
				//加入容器
				aConList.add(aRowTempList);
			}
			
		    
		  //獲取所有車總信息
			private java.util.ArrayList<java.util.ArrayList<String>> getCarKind(String strCarList, base.MdsDbExtend g_mdsDB)throws Exception{
				String sb = "SELECT   NAME FROM     sys_vehicle_code ";
				if(strCarList.trim().length()>0){
					sb = sb+" where NAME in('"+strCarList+")";
				}
				sb = sb + "ORDER BY id";
				return g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(sb));
			}
			
			//取得登錄用戶縣市代碼
			private String getCITYCODE(base.MdsDbExtend g_mdsDB)throws Exception{
				String sql = "select set_value from dis_config t where SET_NAME='CITY_ID'";
				java.util.ArrayList aList = g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(sql));
				String strCityCode = "";
				if(aList!=null){
					strCityCode = ((java.util.ArrayList)aList.get(0)).get(0).toString();
				}
				return strCityCode;
			}
			
			//獲取對應縣市所有分隊SQL
			private String getAllDeptSQL(base.MdsDbExtend g_mdsDB) throws Exception{
				StringBuffer  sb = new StringBuffer();
				sb.append("\n SELECT   deptname,deptno ");
				sb.append("\n FROM     dept t ");
				sb.append("\n WHERE    deptno LIKE '"+this.getCITYCODE(g_mdsDB)+"%' ");
				sb.append("\n          AND grade = 3 ");

				sb.append(" ORDER BY deptno ");

				return sb.toString();
			}
			
			private String getQuerySQL(String strButtonType,String strDEPT,String strCarList)throws Exception{
				String strQueryString = "" ;
				if("1".equals(strButtonType))     strQueryString = this.getQuerySQL1(strDEPT,strCarList);
				return strQueryString ; 
			}
			
			private String getPersonCount(String strButtonType,String strDEPT,base.MdsDbExtend g_mdsDB)throws Exception{
				String strPersonCount = "0" ;
				if("1".equals(strButtonType))     strPersonCount = ""+(Integer.valueOf(this.getPersonCount1(strDEPT,g_mdsDB)).intValue() - Integer.valueOf(this.getPersonCount2(strDEPT,g_mdsDB)).intValue());

				return strPersonCount ; 
			}
			
			//獲取某個分隊的可用車輛數
			private String getQuerySQL1(String strDEPT,String strCarList)throws Exception{
				
				StringBuffer  sb = new StringBuffer();
				sb.append("\n SELECT   aa.NAME, ");
				sb.append("\n          (Nvl(bb.totcount,0) ");
				sb.append("\n             - Nvl(cc.totcount,0) ");
				sb.append("\n             - Nvl(dd.totcount,0)) AS cnt ");
				sb.append("\n FROM     sys_vehicle_code aa ");
				sb.append("\n          LEFT JOIN (SELECT   a.id, ");
				sb.append("\n                              a.NAME, ");
				sb.append("\n                              COUNT(* ) AS totcount ");
				sb.append("\n                     FROM     sys_vehicle b, ");
				sb.append("\n                              sys_vehicle_code a ");
				sb.append("\n                     WHERE    b.car_kind = a.id ");
				sb.append("\n                              AND b.car_status IN ('1','5') ");
				sb.append("\n                              AND b.deptno = '"+strDEPT+"' ");
				sb.append("\n                     GROUP BY a.id,a.NAME) bb ");
				sb.append("\n            ON aa.id = bb.id ");
				sb.append("\n          LEFT JOIN (SELECT   a.id, ");
				sb.append("\n                              a.NAME, ");
				sb.append("\n                              COUNT(* ) AS totcount ");
				sb.append("\n                     FROM     dis_disp_carsub c, ");
				sb.append("\n                              sys_vehicle_code a, ");
				sb.append("\n                              sys_vehicle b ");
				sb.append("\n                     WHERE    is_back = 'N' ");
				sb.append("\n                              AND b.call_no = c.call_no ");
				sb.append("\n                              AND b.car_kind = a.id ");
				sb.append("\n                              AND c.depno = b.deptno ");
				sb.append("\n                              AND b.deptno = '"+strDEPT+"' ");
				sb.append("\n                     GROUP BY a.id,a.NAME) cc ");
				sb.append("\n            ON bb.id = cc.id ");
				sb.append("\n          LEFT JOIN (SELECT   a.id, ");
				sb.append("\n                              a.NAME, ");
				sb.append("\n                              COUNT(* ) AS totcount ");
				sb.append("\n                     FROM     dis_disp_car d, ");
				sb.append("\n                              sys_vehicle_code a, ");
				sb.append("\n                              sys_vehicle b ");
				sb.append("\n                     WHERE    is_sta = 'N' ");
				sb.append("\n                              AND b.call_no = d.call_no ");
				sb.append("\n                              AND b.car_kind = a.id ");
				sb.append("\n                              AND d.depno = b.deptno ");
				sb.append("\n                              AND b.call_no not in (select call_no from dis_disp_carsub where depno = '"+strDEPT+"' and is_back = 'N'   ) ");//過濾掉重複數據
				sb.append("\n                              AND b.deptno = '"+strDEPT+"' ");
				sb.append("\n                              AND d.cs_no in (select cs_no from  DIS_CASE_HANDLE ) ");//ADD BY WANGZHAOYANG DT:20090818
				sb.append("\n                     GROUP BY a.id,a.NAME) dd ");
				sb.append("\n            ON cc.id = dd.id ");
				if(strCarList.trim().length()>0){
					sb.append(" where aa.NAME in('"+strCarList+")");
				}
				sb.append("\n ORDER BY aa.id");

				//System.out.println(sb.toString());
				
				return sb.toString();
			}
			
			//--可用人力   - 各分隊
			private String getPersonCount1(String strDEPT,base.MdsDbExtend g_mdsDB)throws Exception{

				String sb = ""
					+ "SELECT nvl(sum(a.pcount),0) "
					+ "FROM   pln_sch_assn a ,sys_facode b "
					+ "WHERE  a.task = b.code and b.kind='R' "
					+ "       AND b.value='Y' AND a.deptno = '" + strDEPT + "' "
					+ "       AND a.taskdate = '" + this.getNowDateYMD() + "' "
					+ "       AND LPAD(a.timescale,2,'0') = '" + this.getNowDateHH() + "'";
				
				
				
				
				//System.out.println("各分隊可用人力》》》》》》》》》》》getPersonCount1 "+sb.toString());
				
				
				java.util.ArrayList<java.util.ArrayList<String>> aList = g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(sb));
				return aList.size()>0?aList.get(0).get(0):"0";
			}
			
			//--已出動人力 - 各分隊
			private String getPersonCount2(String strDEPT,base.MdsDbExtend g_mdsDB)throws Exception{
				String sb = ""
					+ "SELECT nvl(sum(PCOUNT),0) "
					+ "FROM   dis_rec_disp "
					+ "WHERE  cs_no IN (SELECT cs_no "
					+ "                 FROM   dis_case_acc "
					+ "                 WHERE  is_done = 'N' "
					+ "						   and CS_NO in (select cs_no from dis_case_handle))"
					+ "       AND depno = '" + strDEPT + "'";
				
				//System.out.println("各分隊已出動人力getPersonCount2 "+sb.toString());
				
				java.util.ArrayList<java.util.ArrayList<String>> aList = g_mdsDB.getDataArrayList(g_mdsDB.exeQuery(sb));
				return aList.size()>0?aList.get(0).get(0):"0";
			}
			
			//獲取當前年月日
			private String getNowDateYMD()throws Exception{
				return com.mds.mdsDateTime.getNowDateTime("yyyyMMdd");
			}
			
			//獲取當前時
			private String getNowDateHH()throws Exception{
				return "10";
			}
}
