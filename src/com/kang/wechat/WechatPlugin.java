
 /**
  * This Plugin used for send incident alert to wechat
  * The Dynatrace community portal can be found here: http://community.dynatrace.com/
  * For information how to publish a plugin please visit https://community.dynatrace.com/community/display/DL/How+to+add+a+new+plugin/
  * Author : Xue Kang (Michael)
  * Version : 1.0.0
  * Date : 2017.5.6
  **/ 

package com.kang.wechat;

import com.dynatrace.diagnostics.pdk.*;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;


public class WechatPlugin implements ActionV2 {

	private static final Logger log = Logger.getLogger(WechatPlugin.class.getName());
	private String m_getTokenUrl = "";
	private String m_corpId = "";
	private String m_corpSecret= "";
	private String m_token = "";
	private String m_url= "";
	private String m_touser = "";
	private String m_toparty= "";
	private String m_msgtype= "";
	private String m_agentid = "";	
	private String m_safe= "";
	/**
	 * Initializes the Plugin. This method is called in the following cases:
	 * <ul>
	 * <li>before <tt>execute</tt> is called the first time for this
	 * scheduled Plugin</li>
	 * <li>before the next <tt>execute</tt> if <tt>teardown</tt> was called
	 * after the last execution</li>
	 * </ul>
	 * 
	 * <p>
	 * If the returned status is <tt>null</tt> or the status code is a
	 * non-success code then {@link #teardown(ActionEnvironment)} will be called
	 * next.
	 * 
	 * <p>
	 * Resources like sockets or files can be opened in this method.
	 * Resources like sockets or files can be opened in this method.
	 * @param env
	 *            the configured <tt>ActionEnvironment</tt> for this Plugin
	 * @see #teardown(ActionEnvironment)
	 * @return a <tt>Status</tt> object that describes the result of the
	 *         method call
	 */
	@Override
	public Status setup(ActionEnvironment env) throws Exception {
		m_getTokenUrl = env.getConfigString("url_for_token");
		m_corpId = env.getConfigString("corpid");
		m_corpSecret = env.getConfigString("corpsecret");
		m_url = env.getConfigString("corpsecret");
		m_touser = env.getConfigString("touser");
		m_toparty = env.getConfigString("toparty");
		m_msgtype = env.getConfigString("msgtype");
		m_agentid = env.getConfigString("agentid");
		m_safe = env.getConfigString("safe");
		m_url = env.getConfigString("url");
		return new Status(Status.StatusCode.Success);
	}


	/**
	 * Executes the Action Plugin to process incidents.
	 * 
	 * <p>
	 * This method may be called at the scheduled intervals, but only if incidents
	 * occurred in the meantime. If the Plugin execution takes longer than the
	 * schedule interval, subsequent calls to
	 * {@link #execute(ActionEnvironment)} will be skipped until this method
	 * returns. After the execution duration exceeds the schedule timeout,
	 * {@link ActionEnvironment#isStopped()} will return <tt>true</tt>. In this
	 * case execution should be stopped as soon as possible. If the Plugin
	 * ignores {@link ActionEnvironment#isStopped()} or fails to stop execution in
	 * a reasonable timeframe, the execution thread will be stopped ungracefully
	 * which might lead to resource leaks!
	 * 
	 * @param env
	 *            a <tt>ActionEnvironment</tt> object that contains the Plugin
	 *            configuration and incidents
	 * @return a <tt>Status</tt> object that describes the result of the
	 *         method call
	 */
	@Override
	public Status execute(ActionEnvironment env) throws Exception {		
		// this sample shows how to receive and act on incidents
		Collection<Incident> incidents = env.getIncidents();
		for (Incident incident : incidents) {
			String message = incident.getMessage();
			log.info("Incident " + message + " triggered.");
			for (Violation violation : incident.getViolations()) {
				log.info("Measure " + violation.getViolatedMeasure().getName() + " violoated threshold.");
				log.info(violation.toString());
				String alert_msg = message + "---Detail info: " + violation.getTriggerValues();
				getAccessToken(m_getTokenUrl,m_corpId, m_corpSecret);
				sentIncidentAlert(alert_msg);
			}
		}
		
		return new Status(Status.StatusCode.Success);
	}

	/**
	 * Shuts the Plugin down and frees resources. This method is called in the
	 * following cases:
	 * <ul>
	 * <li>the <tt>setup</tt> method failed</li>
	 * <li>the Plugin configuration has changed</li>
	 * <li>the execution duration of the Plugin exceeded the schedule timeout</li>
	 * <li>the schedule associated with this Plugin was removed</li>
	 * </ul>
	 * <p>
	 * The Plugin methods <tt>setup</tt>, <tt>execute</tt> and
	 * <tt>teardown</tt> are called on different threads, but they are called
	 * sequentially. This means that the execution of these methods does not
	 * overlap, they are executed one after the other.
	 * 
	 * <p>
	 * Examples:
	 * <ul>
	 * <li><tt>setup</tt> (failed) -&gt; <tt>teardown</tt></li>
	 * <li><tt>execute</tt> starts, configuration changes, <tt>execute</tt>
	 * ends -&gt; <tt>teardown</tt><br>
	 * on next schedule interval: <tt>setup</tt> -&gt; <tt>execute</tt> ...</li>
	 * <li><tt>execute</tt> starts, execution duration timeout,
	 * <tt>execute</tt> stops -&gt; <tt>teardown</tt></li>
	 * <li><tt>execute</tt> starts, <tt>execute</tt> ends, schedule is
	 * removed -&gt; <tt>teardown</tt></li>
	 * </ul>
	 * Failed means that either an unhandled exception is thrown or the status
	 * returned by the method contains a non-success code.
	 * 
	 * <p>
	 * All by the Plugin allocated resources should be freed in this method.
	 * Examples are opened sockets or files.
	 * 
	 * @see #setup(ActionEnvironment)
	 */
	@Override
	public void teardown(ActionEnvironment env) throws Exception {
		// TODO
	}
	
	
	/**
	 * get access token
	 */
	public Status getAccessToken(String url, String corpid, String corpsecret) throws Exception {
		// TODO
		log.info("try to getAccessToken");
		CloseableHttpClient httpClient = getHttpClient();
		 try {
			 String posturl = url +"?corpid="+ corpid + "&corpsecret=" + corpsecret;
			 log.info("---------------------------------------");
			 log.info(posturl);
			 log.info("---------------------------------------");			 
			 HttpPost post = new HttpPost(posturl);
			 CloseableHttpResponse httpResponse = httpClient.execute(post);
			 try{
	                HttpEntity entity = httpResponse.getEntity();
	                if (null != entity){
	                	String response = EntityUtils.toString(entity);
	                	log.info(response);
	                    JSONObject resp_json = new JSONObject(response);	                    
	                    m_token = resp_json.get("access_token").toString();
	                }	                
	            } finally{
	                httpResponse.close();
	            }
		 }
		 finally{
	            try{
	                closeHttpClient(httpClient);                
	            } catch(Exception e){
	                e.printStackTrace();
	            }
	      }		
		return new Status(Status.StatusCode.Success);
	}
	
	
	/**
	 * sent incident alert message
	 */
	public Status sentIncidentAlert(String msg) throws Exception {
		// TODO
		CloseableHttpClient httpClient = getHttpClient();
		 try {			 
			 String url_withToken = m_url + "?access_token=" + m_token;
			 HttpPost post = new HttpPost(url_withToken);			 
			 String alert_setting = "{\"touser\":\"" + m_touser + "\",\"toparty\":\"" + m_toparty + "\",\"msgtype\":\"" + m_msgtype + "\",\"agentid\":\"" + m_agentid + "\",\"text\":{\"content\":\"" + msg + "\"},\"safe\":\"" + m_safe +  "\"}";
			 log.info("---------------------------------------");
			 log.info(alert_setting);
			 log.info("---------------------------------------");
			 StringEntity se = new StringEntity(alert_setting);
		     post.setEntity(se);
			 CloseableHttpResponse httpResponse = httpClient.execute(post);
			 try{
	                HttpEntity entity = httpResponse.getEntity();
	                if (null != entity){
	                	log.info(entity.toString());
	                }	                
	            } finally{
	                httpResponse.close();
	            }
		 }
		 finally{
	            try{
	                closeHttpClient(httpClient);                
	            } catch(Exception e){
	                e.printStackTrace();
	            }
	      }
		return new Status(Status.StatusCode.Success);
	}
	
	private static CloseableHttpClient getHttpClient(){
        return HttpClients.createDefault();
    }

    private static void closeHttpClient(CloseableHttpClient client) throws IOException{
        if (client != null){
            client.close();
        }
    }
}
