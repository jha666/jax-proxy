//package se.independent.proxy;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.Set;
//
//import javax.naming.Context;
//import javax.naming.InitialContext;
//import javax.naming.NameClassPair;
//import javax.naming.NamingEnumeration;
//import javax.naming.NamingException;
//import javax.servlet.ServletConfig;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRegistration.Dynamic;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.sql.DataSource;
//
//import org.pmw.tinylog.Configurator;
//import org.pmw.tinylog.Level;
//import org.pmw.tinylog.Logger;
//import org.pmw.tinylog.writers.RollingFileWriter;
//
//@SuppressWarnings("serial")
//public class Admin extends HttpServlet {
//
//	static {
//		Configurator.currentConfig()
//		   .writer(new RollingFileWriter(System.getenv("HOMEPATH") + "/logs/proxy.log", 8), "{date:HH:mm:ss,SSS} {level|min-size=7} [{thread}] {class} ({line}): {message}")
//		   .level(Level.DEBUG)
//		   .activate();
//	}
//
//	private Connection conn;
//
//	@Override
//	public String getServletInfo() {
//		return Admin.class.getSimpleName();
//	}
//
//	
//	@Override
//	public void init(ServletConfig arg0) throws ServletException {
//		super.init(arg0);
//		Logger.info("> init()");
//		connect(arg0.getInitParameter("proxy-ds"));
//		
//		String tableName = arg0.getInitParameter("property-table-qn");
//		if (tableName == null || "".equals(tableName.trim())) {
//			tableName = "proxy.properties";
//		}
//		
//		PreparedStatement stmnt = null;
//		ResultSet rs = null;
//		try {
//			stmnt = conn.prepareStatement("select name, value from " + tableName + " where key = 'pattern'");
//			rs = stmnt.executeQuery();
//			while (rs.next()) {
//				final String servletName = rs.getString("name");
//				final String urlPatterns = rs.getString("value");
//				Logger.info("- init() servletName=" + servletName + " urlPatterns=" + urlPatterns);
//				Dynamic dyn = arg0.getServletContext().addServlet(servletName, new AsyncProxy());
//				Set<String> x = dyn.addMapping(urlPatterns);
//				dyn.setAsyncSupported(true);
//				if (!x.isEmpty()) {
//				}
//			}
//		} catch (Exception ex) {
//			Logger.error(ex,"! init()");
//		} finally {
//			try { if (rs != null) rs.close(); } catch (SQLException sqx) {}
//			try { if (stmnt != null) stmnt.close(); } catch (SQLException sqx) {}
//		}
//		Logger.info("< init()");
//	}
//
//
//	@Override
//	public void destroy() {
//		Logger.info("> destroy()");
//		if (conn != null) {
//			try { conn.close(); } catch (Exception ex) { conn = null;}
//		}
//		
//		super.destroy();
//		Logger.info("< destroy()");
//	}
//	
//
//	@Override
//	public void doGet(HttpServletRequest req, final HttpServletResponse res) {
//		Logger.info("> doGet(" + req.getRequestURI() + ", " + res + ")");
//		res.setStatus(200);
//	}
//
//	@Override
//	public void doPost(HttpServletRequest req, final HttpServletResponse res) {
//		Logger.info("> doPost(" + req.getRequestURI() + ", " + res + ")");
//		final String servletName = req.getParameter("servletName");
//		final String urlPatterns = req.getParameter("urlMappings");
//		Dynamic dyn = this.getServletContext().addServlet(servletName, new AsyncProxy());
//		Set<String> x = dyn.addMapping(urlPatterns);
//		if (x.isEmpty()) {
//			res.setStatus(200);
//		} else {
//		}
//		Logger.info("< doPost()");
//	}
//	
//	protected void connect(final String jndi) {
//		Logger.info("> connect(" + jndi + ")");
//		
//		String JNDI = "jdbc/proxy-ds";
//	    try {
//	    	final Context initialContext = new InitialContext();
//	      
//	    	if  (initialContext != null) {
//	    		
//	    		NamingEnumeration<NameClassPair> list = initialContext.list("");
//	    		while (list.hasMore()) {
//	    			Logger.info("- connect() jndi: " + list.next().getName());
//	    		}
//	    		
//	    		DataSource datasource = (DataSource)initialContext.lookup(JNDI);
//	    		if (datasource != null) {
//	    			conn = datasource.getConnection();
//	    		}
//	    	}
//	    }
//	    catch (NamingException nx) {
//	    	Logger.error("! connect()", nx);
//	    }
//	    catch(SQLException sqx){
//	    	Logger.error("! connect()", sqx);
//	    }
//
//	    
////		final String driver = System.getProperty("DbClassLoader.Driver", "org.postgresql.Driver");
////		final String jdbc = System.getProperty("DbClassLoader.JDBC", "jdbc:postgresql://127.0.0.1:5432/postgres");
////
////		try {
////			Class.forName(driver);
////			conn = DriverManager.getConnection(jdbc, "DBCLASSLOAD", "Tr1ss");
////
////		} catch (Exception ex) {
////			Logger.error(ex, "# connect()");
////		}
//	}
//}
