package se.independent.proxy;


import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;


public abstract class AbstractProxy extends HttpServlet {

	private static final long serialVersionUID = 1L;

	//private static final Logger LOG = LogManager.getLogger(Proxy.class);	
//	static {
//		Configurator.currentConfig()
//		   .writer(new RollingFileWriter("proxy.log", 8), "{date:HH:mm:ss} {level|min-size=7} [{thread}] {class} ({line}): {message}")
//		   .level(Level.DEBUG)
//		   .activate();
//	}
	
	private DataSource proxyDS;
	

	protected AbstractProxy() {
		super();
		proxyDS = lookup();
	}

	
	protected AbstractProxy(DataSource ds) {
		super();
		proxyDS = ds;
	}
	
	protected Connection connect() {
		Connection rv = null;
	    try {	    	
    		if (proxyDS != null) {
    			rv = proxyDS.getConnection();
    		}
	    }
	    catch(SQLException sqx){
	    	Logger.error("! connect()", sqx);
	    }
	    return rv;
	}

	
	protected DataSource lookup() {
		Logger.debug("> lookup()");
		DataSource rv = null;

		try {
	    	final Context initialContext = new InitialContext();
	      
	    	if  (initialContext != null) {	    		
	    		rv = (DataSource)initialContext.lookup("jdbc/proxy-ds");
	    	}
	    }
	    catch (NamingException nx) {
	    	Logger.error("! lookup()", nx);
	    }
		Logger.debug("< lookup() = " + rv);
	    return rv;
	}

		

	@Override
	public void destroy() {
		Logger.debug("> destroy()");
		proxyDS = null;;
		super.destroy();
		Logger.debug("< destroy()");
	}

	
	@Override
	public String getServletName() {
		String rv =  this.getServletConfig().getInitParameter("bag");
		if (rv == null) {
			rv = super.getServletName();
		}
		Logger.debug("- getServletName() = " + rv);
		
		return rv;
	}
	
	
//	protected String getProperty(final String key) {
//		Logger.debug("> getProperty({})", key);
//		String rv = null;
//		ResultSet rs = null;
//		PreparedStatement ps = prepare();
//		try {
//			ps.setString(1, getServletName());
//			ps.setString(2, key);
//
//			synchronized (ps.getConnection()) {
//				rs = ps.executeQuery();				
//			}
//			if (rs.next()) {
//				rv = rs.getString("VALUE");
//			}
//		} 
//		Logger.debug("< getProperty() = {}", rv);
//		return rv;
//	}

	
	private int so_timeout = 12000;
	private int so_connect = 3000;
	private String location = "";
	private long expires = System.currentTimeMillis() + (1000*60*5);
	private int redirect_sc = HttpStatus.SC_TEMPORARY_REDIRECT;
	
	public int getRedirect_sc() {
		return redirect_sc;
	}

	public void setRedirect_sc(int redirect_sc) {
		this.redirect_sc = redirect_sc;
	}

	@Override
	public void init(ServletConfig arg0) throws ServletException {
		Logger.info("> init() [name={} path={}]", arg0.getServletName(), arg0.getServletContext().getContextPath());
		super.init(arg0);
		
		refreshProxyConfig();
		
		Logger.info("< init()");
	}

	
	protected void refreshProxyConfig() throws ServletException {
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement stmnt = null;
		
		try {
			conn = connect();
			
			stmnt = conn.prepareStatement("select * from PROXY.PROXY where SERVLET_NAME = ? and (EXPIRES is null or ? < EXPIRES) order by EXPIRES asc");
			stmnt.setString(1, getServletName());
			stmnt.setDate(2, new Date(System.currentTimeMillis()));

			rs = stmnt.executeQuery();
			if (rs.next()) {
				location = rs.getString("LOCATION");
				try {
					new URL(location);
				} catch (MalformedURLException mux) {
					Logger.warn("- refreshProxyConfig() location=" + location, mux);
				}
				
				so_timeout = Math.abs(rs.getInt("SOCKET_TIMEOUT"));
				if (so_timeout < 100) {
					Logger.warn("- refreshProxyConfig() so.timeout=" + so_timeout + " ");
				}
				so_connect = Math.abs(rs.getInt("CONNECT_TIMEOUT"));
				if (so_connect < 100) {
					Logger.warn("- refreshProxyConfig() so.connect=" + so_connect + " ");
				}
				Date gt = rs.getDate("EXPIRES");
				if (gt == null) {
					expires = -1l;
				} else {
					expires = gt.getTime();
				}
				
				redirect_sc = rs.getInt("REDIRECT_SC");
				
			}
		} catch (SQLException sqx) {
			Logger.error(sqx, "# init()");
			throw new ServletException(sqx);
		} finally {
			if (rs != null) { try {rs.close(); } catch (Exception ign) {} }
			try { if (stmnt != null) stmnt.close(); } catch (Exception ign) {}
			try { if (conn != null) conn.close();; } catch (Exception ign) {}
		}
	}

	public int getSocketTimeout() {
		return so_timeout;
	}

	
	public int getConnectTimeout() {
		return so_connect;
	}

	public Date getGoodThru() {
		return new Date(expires);
	}
	
	public URI buildURI(final HttpServletRequest req, StringBuffer userInfo) {
		Logger.debug("> buildURI()");
		URI rv = null;
		
		if (expires > 0 && System.currentTimeMillis() > expires) {
			try { refreshProxyConfig(); } catch (ServletException ign) { return rv;}
		}
		
		final String str = location;
		Logger.debug("- buildURI() redirect=" + str);
		
		if (str != null) {
			final String contexpath = req.getContextPath();
			final String servletpath = req.getServletPath();
			final String pathinfo = req.getPathInfo();
			final String querystring = req.getQueryString();
			final String fragment = URI.create(req.getRequestURI()).getFragment();
			
			if (Level.DEBUG.compareTo(Logger.getLevel()) >= 0) {
				Logger.debug("- buildURI() [req] contextPath=" + contexpath);
				Logger.debug("- buildURI() [req] servletPath=" + servletpath);
				Logger.debug("- buildURI() [req] pathinfo=" + pathinfo);
				Logger.debug("- buildURI() [req] querystring=" + querystring);
				Logger.debug("- buildURI() [req] fragment=" + fragment);
			}
			
			final URI tmp = URI.create(str);
			final String path = tmp.getPath() + pathinfo;
			Logger.debug("- buildURI()  path=" + path);

			try {
				rv = new URI(tmp.getScheme(), tmp.getUserInfo(), tmp.getHost(), tmp.getPort(),
						path.replaceAll("//", "/"), querystring, fragment);
				userInfo.append(tmp.getUserInfo());
			} catch (URISyntaxException usx) {
				Logger.error( usx, "# buildURI() str=" + str + " query=" + querystring + " path=" + path);
			}
		} else {
			Logger.error("- buildURI() [redirect] missing for " + req.getContextPath());
		}
		Logger.debug("< buildURI() = " + rv);
		return rv;
	}

	
	
	public void respond(final HttpServletResponse res, final int sc) {
        res.setStatus(sc);
        res.setContentType("text/xml;charset=UTF-8");

		Logger.debug("- respond() sc=" + sc);

        byte[] data = new byte[] {};
        res.setContentLength(data.length);
        OutputStream os = null;
		try {
			os = res.getOutputStream();
			os.write(data);
			os.flush(); 
		} catch (IOException ignored) {
			Logger.error(ignored, "# respond()");
		} finally {
        	if (os != null) {
        		try { os.close(); } catch (IOException ignored) {}
        	}
        }
	}

	
	public int copyResponse(final HttpEntity entity, Header[] headers, 
			StatusLine statusLine, final HttpServletResponse res)  {
		Logger.info("> copyResponse(" + res.hashCode() + ")");
		int len = (int) entity.getContentLength();
		if (statusLine != null) {
			res.setStatus(statusLine.getStatusCode());
		} else {
			res.setStatus(HttpStatus.SC_OK);
		}
		     
        if (entity.getContentType() != null) {
        	res.setContentType(entity.getContentType().getValue());
        } 
        
        if (entity.getContentEncoding() != null) {
        	res.setCharacterEncoding(entity.getContentEncoding().getValue());
        }
        
        if (headers != null) {
	        for (Header hdr : headers) {
	        	if ("Transfer-Encoding".equals(hdr.getName())) {
	        		res.addHeader("Content-Length", Integer.toString(len));
	        		continue;
	        	}

	        	res.addHeader(hdr.getName(), hdr.getValue());
	        }
        }
        
        OutputStream os = null;
        try {
	        os = res.getOutputStream();
        	entity.writeTo(os);
        	os.flush();
        } catch (IOException iox) {
        	Logger.error(iox, "# copyResponse()");
        	len = -1;
        } finally {
        	if (os != null) {
        		try { os.close(); } catch (IOException ignored) {}
        	}
        }

        Logger.info("< copyResponse() [" + res.getStatus() + "] = " + len);
        return len;
	}


	public void copyHeaders(HttpServletRequest req, HttpRequestBase proxy_req) {
		String fff = null;
    	final Enumeration<String> en_headers = req.getHeaderNames();
		while (en_headers.hasMoreElements()) {
			final String name = (String) en_headers.nextElement();
			if ("Host".equals(name)) {
				continue;
			}
			if ("X-Forwarded-For".equals(name)) {
				fff = req.getHeader(name);
			}
			Logger.debug("- copyHeaders() name=" + name + " value=" + req.getHeader(name));
			proxy_req.setHeader(name, req.getHeader(name));
		}
		
		if (fff == null) {
			proxy_req.setHeader("X-Forwarded-For", req.getRemoteAddr() + ", " + req.getLocalAddr());
		} else {
			proxy_req.setHeader("X-Forwarded-For", fff + ", " + req.getLocalAddr());
		}
    }

    
	public void copyParams(HttpServletRequest req, HttpRequestBase proxy_req) {
		final Enumeration<String> en_params = req.getParameterNames(); 
		while (en_params.hasMoreElements()) {
			final String name = (String) en_params.nextElement();
			Logger.debug("- copyParams() name=" + name);
			proxy_req.getParams().setParameter(name, req.getParameter(name));
		}
	}
	
	
	protected boolean compare(HttpResponse response, HttpResponse reference) {
		final StatusLine slt = response.getStatusLine();
		final StatusLine slf = reference.getStatusLine();
		if (slf.getStatusCode() != slf.getStatusCode()) {
			Logger.info("- compare() [status code] " + slt.getStatusCode() + " <> " + slf.getStatusCode());
		}
		
		for (Header hr : reference.getAllHeaders()) {
			Header hc = response.getFirstHeader(hr.getName());
			if (hc == null || !hr.getValue().equalsIgnoreCase(hc.getValue())) {
				Logger.info("- compare [header] name=" + hr.getName() + " " + hr.getValue() + " <> " + hc == null ? "null" : hc.getValue());
			}
		}

		return slt.getStatusCode() == 200; 
	}
}
