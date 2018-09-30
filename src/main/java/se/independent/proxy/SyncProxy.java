/**
 * 
 */
package se.independent.proxy;

import java.io.IOException;
import java.net.URI;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.pmw.tinylog.Logger;

public class SyncProxy extends AbstractProxy {

	protected SyncProxy(DataSource ds) {
		super(ds);
	}


	private static final long serialVersionUID = 1L;

	
	private CloseableHttpClient httpclient = null;
	
	
	@Override
	public String getServletInfo() {
		return SyncProxy.class.getSimpleName();
	}

	
	@Override
	public void init(ServletConfig arg0) throws ServletException {
		Logger.info("> init()");
		super.init(arg0);
		createHttpClient();
		Logger.info("< init()");
	}

	
	@Override
	public void destroy() {
		Logger.info("> destroy()");
		if (httpclient != null) {
			Logger.info("- destroy() close httpclient");
			try { 
				httpclient.close(); 
			} catch (Exception ex) {
				Logger.error("# destroy()", ex);
			}
			httpclient = null;
		}
		
		super.destroy();
		Logger.info("< destroy()");
	}
	
	
	private void createHttpClient() {
		if (httpclient != null) {
			try { httpclient.close(); } catch (Exception ignored) {}
			httpclient = null;
		}
		
		RequestConfig requestConfig = RequestConfig.custom()
	            .setSocketTimeout(getSocketTimeout())
	            .setConnectTimeout(getConnectTimeout()).build();
			
		httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
	}

	
	@Override
	public void doPost(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doPost(" + req.getRemoteHost() + ", " + req.getRequestURI() + ")");
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		
		int bytes = -1;
		try {
			final HttpPost post = new HttpPost(uri);			
			BasicHttpEntity basic = new BasicHttpEntity();
			basic.setContent(req.getInputStream());
			BufferedHttpEntity entity = new BufferedHttpEntity(basic);
			Logger.info("- doPost() " + uri + " length: " + entity.getContentLength());		
			post.setEntity(entity);

			bytes = execute(req, res, post);

		} catch (ClientProtocolException cpx) {
			respond(res, HttpStatus.SC_BAD_GATEWAY);
			Logger.error("# doPost()", cpx);
		} catch (IOException iox) {
			respond(res, HttpStatus.SC_GATEWAY_TIMEOUT);
			Logger.debug("# doPost()", iox);
		} catch (Exception ex) {
			Logger.info("# doPost()", ex);
	    	createHttpClient();
	    }

		Logger.info("< doPost(" + bytes + ")");
	}

	@Override
	public void doPut(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doPut(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}

		int bytes = -1;

		try {
			final HttpPut post = new HttpPut(uri);			
			BasicHttpEntity basic = new BasicHttpEntity();
			basic.setContent(req.getInputStream());
			BufferedHttpEntity entity = new BufferedHttpEntity(basic);
			Logger.info("- doPut() " + uri + " length: " + entity.getContentLength());		
			post.setEntity(entity);

			bytes = execute(req, res, post);

		} catch (IOException iox) {
			respond(res, HttpStatus.SC_BAD_GATEWAY);
			Logger.error("# doPut()", iox);
	    }

		Logger.info("< doPut()");
	}

	
	@Override
	public void doGet(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doGet(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		int len = -1;
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			Logger.info("< doGet(" + len + ")");
			return;
		}
		
	    try {
			final HttpGet httpget = new HttpGet(uri);
			Logger.info("- doGet() " + uri);		
			len = execute(req, res, httpget);
			
	    } catch (ClientProtocolException cpx) {
	    	respond(res, HttpStatus.SC_BAD_GATEWAY);
	    	Logger.error("# doGet()", cpx);
	    } catch (IOException iox) {
	    	respond(res, HttpStatus.SC_GATEWAY_TIMEOUT);
	    	Logger.error("# doGet()", iox);
	    } catch (Exception ex) {
	    	Logger.error("# doGet()", ex);
	    	createHttpClient();
	    }
		Logger.info("< doGet(" + len + ")");
	}
	
	
	@Override
	public void doDelete(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doDelete(" + req.getRemoteHost() + ", " + req.getRequestURI() + ")");
		int len = -1;
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		
	    try {
			final HttpDelete httpdel = new HttpDelete(uri);
			Logger.info("- doDelete() " + uri);		
			len = execute(req, res, httpdel);
			
	    } catch (ClientProtocolException cpx) {
	    	respond(res, HttpStatus.SC_BAD_GATEWAY);
	    	Logger.error("# doDelete()", cpx);
	    } catch (IOException iox) {
	    	respond(res, HttpStatus.SC_GATEWAY_TIMEOUT);
	    	Logger.error("# doDelete()", iox);
	    } catch (Exception ex) {
	    	Logger.error("# doDelete()", ex);
	    	createHttpClient();
	    }
		Logger.info("< doDelete()");
	}


	@Override
	public void doHead(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doHead(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		int len = -1;
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		
	    try {
			final HttpHead httphead = new HttpHead(uri);
			Logger.info("- doHead() " + uri);		
			len = execute(req, res, httphead);
			
	    } catch (ClientProtocolException cpx) {
	    	respond(res, HttpStatus.SC_BAD_GATEWAY);
	    	Logger.error("# doHead()", cpx);
	    } catch (IOException iox) {
	    	respond(res, HttpStatus.SC_GATEWAY_TIMEOUT);
	    	Logger.error("# doHead()", iox);
	    } catch (Exception ex) {
	    	Logger.error("# doHead()", ex);
	    	createHttpClient();
	    }

	    Logger.info("< doHead()");
	}

	
	private int execute(HttpServletRequest req, final HttpServletResponse res, 
			final HttpRequestBase proxy_req)  throws IOException {

		copyHeaders(req, proxy_req);
		copyParams(req, proxy_req);
				
		final CloseableHttpResponse response = httpclient.execute(proxy_req);		
    	final HttpEntity responseEntity = new BufferedHttpEntity(response.getEntity());
    	int rv = copyResponse(responseEntity, response.getAllHeaders(), response.getStatusLine(), res);
    	    	
    	return rv;
	}
}
