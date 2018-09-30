package se.independent.proxy;

import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
//import javax.security.cert.X509Certificate;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.util.HttpAsyncClientUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.pmw.tinylog.Logger;


public class AsyncProxy extends AbstractProxy {

	private static final long serialVersionUID = 1L;

	//private static final Logger LOG = LogManager.getLogger(AsyncRedirectProxy.class);	

	private CloseableHttpAsyncClient  httpclient = null;
		
	public AsyncProxy(final DataSource ds) {
		super(ds);
		Logger.debug("- AsyncProxy()");
	}


	/* (non-Javadoc)
	 * @see javax.servlet.Servlet#getServletInfo()
	 */
	@Override
	public String getServletInfo() {
		return AsyncProxy.class.getSimpleName();
	}

	
	/* (non-Javadoc)
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(final ServletConfig sc) throws ServletException {
		super.init(sc);
		Logger.debug("> init()");
		httpclient = startQuietly();
		Logger.debug("< init()");
	}

	
	protected SSLContext getSSLContext() {
		SSLContext rv = null;
	    TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
	    	public boolean isTrusted(X509Certificate[] certificate,  String authType) {
	    		return true;
	    	}
	    };
	    
		try {
			rv = SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy)
					.build();
		} catch (Exception e) {
			Logger.error(e, "! getSSLContext()");
		}
		
		return rv;
	}
	
	
//	private SchemeIOSessionStrategy getSSLStrategy() {
//		SchemeIOSessionStrategy rv = null;
//		try {
//	        SSLContext sslcontext = SSLContexts.custom()
//	                .loadTrustMaterial(new TrustSelfSignedStrategy())
//	                .build();
//	        // Allow TLSv1 protocol only
//	        rv = new SSLIOSessionStrategy(
//	                sslcontext,
//	                new String[] { "TLSv1", "TLSv1.2" },
//	                null,
//	                SSLIOSessionStrategy.getDefaultHostnameVerifier());
//		} catch (Exception ex) {
//			Logger.error("# getSSLStrategy()", ex);
//		}
//		Logger.debug("- getSSLStrategy() = " + rv);
//		return rv;
//	}
	
	
	private CloseableHttpAsyncClient startQuietly() {
		CloseableHttpAsyncClient tmp = null;
		RequestConfig requestConfig = RequestConfig.custom()
	            .setSocketTimeout(getSocketTimeout())
	            .setConnectionRequestTimeout(1000)
	            .setConnectTimeout(getConnectTimeout())
	            .build();
	    
	    try {
//		    TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
//		        public boolean isTrusted(X509Certificate[] certificate,  String authType) {
//		            return true;
//		        }
//		    };
//	    	SSLContext sslContext = SSLContexts.custom()
//	    			.loadTrustMaterial(null, acceptingTrustStrategy).build();
//	 
//	    	tmp = HttpAsyncClients.custom()
//	    			.setDefaultRequestConfig(requestConfig)
//	    			.setSSLHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
//	    			.setSSLContext(sslContext)
//	    			.build();
	    
	    	tmp = HttpAsyncClients.custom()
				.setDefaultRequestConfig(requestConfig)
//				.setSSLStrategy(getSSLStrategy())
				.setSSLContext(getSSLContext())
				.setTargetAuthenticationStrategy(new AuthenticationStrategy() {

					public void authFailed(HttpHost arg0, AuthScheme arg1, HttpContext arg2) {
						Logger.debug(">< authFailed()");
					}

					public void authSucceeded(HttpHost arg0, AuthScheme arg1, HttpContext arg2) {
						Logger.debug(">< authSucceeded()");
					}

					public Map<String, Header> getChallenges(HttpHost arg0, HttpResponse arg1, HttpContext arg2)
							throws MalformedChallengeException {
						Logger.debug(">< getChallenges() = null");
						return null;
					}

					public boolean isAuthenticationRequested(HttpHost arg0, HttpResponse arg1, HttpContext arg2) {
						Logger.debug(">< isAuthenticationRequested() = false");
						return false;
					}

					public Queue<AuthOption> select(Map<String, Header> arg0, HttpHost arg1, HttpResponse arg2,
							HttpContext arg3) throws MalformedChallengeException {
						// TODO Auto-generated method stub
						return null;
					}
										
				})
				.build();	
	    } catch (Exception ex) {
		    tmp = HttpAsyncClients.createDefault();
	    }
	    
		tmp.start();	    	
		
		return tmp;
	}
	
	@Override
	public void destroy() {
		Logger.debug("> destroy()");
		if (httpclient != null) {
			Logger.info("- destroy() httpclient: " + httpclient.isRunning());
			try { HttpAsyncClientUtils.closeQuietly(httpclient); } catch (Throwable x) { Logger.error(x,"# destroy()"); }
			httpclient = null;
		}
		
		super.destroy();
		Logger.debug("< destroy()");
	}

	
	@Override
	public void doPost(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doPost(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		
		try {
			final AsyncContext ac = req.startAsync();
			final HttpPost post = new HttpPost(uri);			
			BasicHttpEntity basic = new BasicHttpEntity();
			basic.setContent(req.getInputStream());
			BufferedHttpEntity entity = new BufferedHttpEntity(basic);
			Logger.debug("- doPost() " + uri + " length: " + entity.getContentLength());		
			post.setEntity(entity);
						
			execute(req, res, post, ac);
				
		} catch (IOException iox) {
			respond(res, HttpStatus.SC_BAD_GATEWAY);
			Logger.error("# doPost()", iox);
	    }

		Logger.info("< doPost()");
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
		
		try {
			final AsyncContext ac = req.startAsync();
			final HttpPut post = new HttpPut(uri);			
			BasicHttpEntity basic = new BasicHttpEntity();
			basic.setContent(req.getInputStream());
			BufferedHttpEntity entity = new BufferedHttpEntity(basic);
			Logger.debug("- doPut() " + uri + " length: " + entity.getContentLength());		
			post.setEntity(entity);
						
			execute(req, res, post, ac);
				
		} catch (IOException iox) {
			respond(res, HttpStatus.SC_BAD_GATEWAY);
			Logger.error("# doPut()", iox);
	    }

		Logger.info("< doPut()");
	}

	
	// ------------------------------------------------------------------------
	
	@Override
	public void doGet(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doGet(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		int len = -1;
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		
		
    	final AsyncContext ac = req.startAsync();
		final HttpGet httpget = new HttpGet(uri);

		execute(req, res, httpget, ac);

		Logger.info("< doGet()");
	}

	
	@Override
	public void doDelete(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doDelete(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		int len = -1;
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		

    	final AsyncContext ac = req.startAsync();
		final HttpDelete httpget = new HttpDelete(uri);

		execute(req, res, httpget, ac);

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
		

    	final AsyncContext ac = req.startAsync();
		final HttpHead httpget = new HttpHead(uri);

		execute(req, res, httpget, ac);

		Logger.info("< doHead()");
	}

	
	@Override
	public void doOptions(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doOptions(" + req.getRemoteHost() + ", " + req.getRequestURI() + "," + res.hashCode() + ")");
		int len = -1;
		StringBuffer userInfo = new StringBuffer();
		final URI uri = buildURI(req, userInfo);
		
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}
		

    	final AsyncContext ac = req.startAsync();
		final HttpOptions httpget = new HttpOptions(uri);

		execute(req, res, httpget, ac);

		Logger.info("< doOptions()");
	}

	// ========================================================================
	// execute the request
	// ========================================================================
	private void execute(HttpServletRequest req, final HttpServletResponse res, 
			final HttpRequestBase proxy_req, final AsyncContext ac)  {

		copyHeaders(req, proxy_req);
		copyParams(req, proxy_req);		

		if (!httpclient.isRunning()) {
			Logger.debug("- execute() not Running");
			httpclient = startQuietly();
		}
		
		final int timeout = getConnectTimeout() + getSocketTimeout() + getConnectTimeout();
		Logger.debug("- execute() timeout=" + timeout);
		ac.setTimeout(timeout);
		
		ac.addListener(new AsyncListener() {
			
			public void onTimeout(AsyncEvent arg0) throws IOException {
				respond(res, HttpStatus.SC_GATEWAY_TIMEOUT);
				Logger.debug("- onTimeout() uri=" + proxy_req.getURI());
				HttpAsyncClientUtils.closeQuietly(httpclient);
			}
			
			public void onStartAsync(AsyncEvent arg0) throws IOException {
			}
			
			public void onError(AsyncEvent arg0) throws IOException {
				respond(res, HttpStatus.SC_BAD_GATEWAY);
				Logger.error("- onError() uri=" + proxy_req.getURI());
				Logger.error("- onError() response=" + arg0.getSuppliedResponse());
				Logger.error("- onError() event.throwable=" + arg0.getThrowable());
				Logger.error("- onError() event.class=" + arg0.getClass());
				HttpAsyncClientUtils.closeQuietly(httpclient);
			}
			
			public void onComplete(AsyncEvent arg0) throws IOException {
			}
			
		});
		
		
		Future<HttpResponse> resp =
				httpclient.execute(proxy_req, new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse response) {
				try {
					final HttpEntity responseEntity = new BufferedHttpEntity(response.getEntity());
					copyResponse(responseEntity, response.getAllHeaders(), response.getStatusLine(), res);					
				} catch (IOException iox) {
					respond(res, HttpStatus.SC_BAD_GATEWAY);
					Logger.error("! execute()", iox);
				} finally {
					ac.complete();
				}
            }

            public void failed(final Exception ex) {
            	respond(res, HttpStatus.SC_BAD_GATEWAY);
				ac.complete();
            	Logger.error(ex, "! execute() failed");
            }

            public void cancelled() {
            	respond(res, HttpStatus.SC_GATEWAY_TIMEOUT);
				ac.complete();
            	Logger.error("! execute() cancelled");
            }
		});	
		
		if (resp == null) {
			respond(res, HttpStatus.SC_BAD_GATEWAY);
			Logger.error("# execute() no response object");
		}
	}
}
