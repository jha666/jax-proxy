package se.independent.proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.ServletSecurityElement;
import javax.sql.DataSource;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.RollingFileWriter;

public class ProxyServletContextListener implements ServletContextListener {

	static {
		Configurator.currentConfig()
		   .writer(new RollingFileWriter(System.getenv("HOMEPATH") + "/logs/proxy.log", 8), "{date:HH:mm:ss,SSS} {level|min-size=7} [{thread}] {class} ({line}): {message}")
		   .level(Level.DEBUG)
		   .activate();
	}

	private static String JNDI = System.getProperty("proxy-ds", "jdbc/proxy-ds");

	private DataSource ds;
	

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	
		Logger.info("> contextInitialized()");
		ServletContext sc = sce.getServletContext();
		
		if (sc.getInitParameter("proxy-ds") != null) {
			JNDI = sc.getInitParameter("proxy-ds");
		}
		
		ds  = lookup();

		if (ds == null) {
			Logger.warn("- contextInitialized() no datasource with the given name was found (" + JNDI + ")");
			Logger.info("< contextInitialized()");
			return;
		}
		
		
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		Connection conn = null;
		try {
			conn = ds.getConnection();
			
			stmnt = conn.prepareStatement("select * from PROXY.SERVLET");
			rs = stmnt.executeQuery();
			while (rs.next()) {
				final String servletName = rs.getString("SERVLET_NAME");
				final String   proxyType = rs.getString("PROXY_TYPE");
				final String urlPatterns = rs.getString("URL_PATTERN");
				final String initParameters = rs.getString("INIT_PARAMETERS");
				final int loadOnStartup = rs.getInt("LOAD_ON_STARTUP");
				final String emptyrs = rs.getString("EMPTY_ROLE_SEMANTIC");
				final String transguran = rs.getString("TRANSPORT_GUaRANTEE");
				final String roleNames = rs.getString("ROLE_NAMES");
				
				Logger.debug("- contextInitialized() servletName=" + servletName + " urlPatterns=" + urlPatterns);
				AbstractProxy ap = null;
				if ("Redirect".equalsIgnoreCase(proxyType)) {
					ap = new Redirect(ds);
				} else if ("Sync".equalsIgnoreCase(proxyType)) {
					ap = new SyncProxy(ds);
				} else {
					ap = new AsyncProxy(ds);
				}
				
				if (initParameters != null && !"".equals(initParameters.trim())) {
					for (String pair :initParameters.split(" ;:\t\n")) {
						Logger.debug("- contextInitialized() pair=" + pair);
						String[] name_value = pair.split("=");
						ap.getServletContext().setInitParameter(name_value[0], name_value[1]);
					}
				}
				
				Dynamic dyn = sce.getServletContext().addServlet(servletName, ap);

				if (dyn == null) {
					Logger.warn("- contextInitialized() no servlet for: " + servletName);
					continue;
				}
				
				if (ap instanceof AsyncProxy) {
					dyn.setAsyncSupported(true);
				} else {
					dyn.setAsyncSupported(false);
				}

				Logger.info("- contextInitialized() loadOnStartup=" + loadOnStartup);
				dyn.setLoadOnStartup(loadOnStartup);				

				if (!"NONE".equals(transguran)) {
					HttpConstraintElement tg = 
							new HttpConstraintElement(EmptyRoleSemantic.PERMIT,
									TransportGuarantee.CONFIDENTIAL, "");
					ServletSecurityElement sse = new ServletSecurityElement(tg);
					
					dyn.setServletSecurity(sse);
				}

				Set<String> x = dyn.addMapping(urlPatterns);
				if (!x.isEmpty()) {
					for ( String p : x) {
						Logger.warn("- contextInitialized() pattern already mapped: " + p);
					}
				}

			}
			
			
		} catch (Exception ex) {
			Logger.error(ex,"! contextInitialized()");
		} finally {
			try { if (rs != null) rs.close(); } catch (SQLException sqx) {}
			try { if (stmnt != null) stmnt.close(); } catch (SQLException sqx) {}
			try { if (conn != null) conn.close(); } catch (SQLException sqx) {}
		}
		Logger.info("< contextInitialized()");
	}

	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		Logger.info("> contextDestroyed()");
		Logger.info("< contextDestroyed()");
	}

	
	protected DataSource lookup() {
		Logger.debug("> lookup()");
		DataSource rv = null;

		try {
	    	final Context initialContext = new InitialContext();
	      
	    	if  (initialContext != null) {

//	    		NamingEnumeration<NameClassPair> list = initialContext.list("");
//	    		while (list.hasMore()) {
//	    			Logger.debug("- lookup() jndi: " + list.next().getName());
//	    		}
	    		
	    		rv = (DataSource)initialContext.lookup(JNDI);
	    	}
	    }
	    catch (NamingException nx) {
	    	Logger.error("! lookup()", nx);
	    }
		Logger.debug("< lookup() = " + rv);
	    return rv;
	}
}
