package service.TaskJobs;

import java.net.HttpURLConnection;
import java.net.URL;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class Task_DTS070188 implements Job{
	private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("TTask");
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		String title = "建築物資料服務";
	try{
logger.info("********"+title+"********");
		
logger.info("*******檢查"+title+"是否開啟及URL********");
		base.MdsDbExtend mdsDB = null;
		mdsDB = base.ConnectionProvider.getMdsDb();
		java.util.ArrayList Data = mdsDB.getDataArrayList(mdsDB.exeQuery("select SET_NAME,SET_VALUE from DIS_CONFIG where SET_NAME IN ('Task_DTS070188_DOWNLOAD','Task_DTS070188_DOWNLOAD_Flag')"));
		java.util.ArrayList TempData = null;
		String strSet_Name = "";
		String strUrl = "";
		String OpenFlag = "";
		for(int i=0;i<Data.size();i++){
			TempData = new java.util.ArrayList();
			TempData = (java.util.ArrayList)Data.get(i);
			strSet_Name = (TempData.get(0)==null?"":TempData.get(0).toString());
			if(strSet_Name.equals("Task_DTS070188_DOWNLOAD"))
				strUrl = (TempData.get(1)==null?"":TempData.get(1).toString());
			else if(strSet_Name.equals("Task_DTS070188_DOWNLOAD_Flag"))
				OpenFlag = (TempData.get(1)==null?"":TempData.get(1).toString());
		}
		if(OpenFlag.trim().equals("Y") && !(strUrl.trim().equals(""))){
logger.info("*******"+title+"開啟,URL====="+strUrl);
logger.info("*******開始"+title+"********");
			URL url = new URL(strUrl);
			HttpURLConnection servletConnection = (HttpURLConnection) url.openConnection();
			servletConnection.setRequestMethod("POST");
			servletConnection.setDoInput(true);
			servletConnection.setDoOutput(true);
			servletConnection.connect(); 

			java.io.BufferedReader br = new java.io.BufferedReader( new java.io.InputStreamReader(servletConnection.getInputStream()));
			String buf = "";
			while((buf=br.readLine())!=null){}
			br.close();
	    
			servletConnection.disconnect();
		}else{
logger.info("********"+title+"開關********"+OpenFlag);
logger.info("********"+title+"URL********"+strUrl);	
			
		}
		if(mdsDB!=null){mdsDB.disconnect();}
logger.info("********結束"+title+"********");
	}catch(final Exception e){
		
	}
}
}
