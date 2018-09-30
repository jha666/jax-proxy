package se.independent.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;
import org.pmw.tinylog.Logger;


public class Redirect extends AbstractProxy {

	protected Redirect(DataSource ds) {
		super(ds);
	}


	private static final long serialVersionUID = 1L;
		
	
	/* (non-Javadoc)
	 * @see javax.servlet.Servlet#getServletInfo()
	 */
	@Override
	public String getServletInfo() {
		return Redirect.class.getSimpleName();
	}


	@Override
	public void doPost(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doPost(" + req.getRequestURI() + ")");
		final StringBuffer ignored = new StringBuffer();
		final URI uri = buildURI(req, ignored);
	
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			Logger.info("< doPost()");
			return;
		}

		List<BasicHeader> headers = new ArrayList<BasicHeader>(1);
		headers.add(new BasicHeader("Location", uri.toASCIIString()));
		respond(res, getRedirectSC(req), headers);
			
		Logger.info("< doPost(" + uri.toASCIIString() + ")");
	}


	@Override
	public void doPut(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doPut(" + req.getRequestURI() + ")");
		final StringBuffer ignored = new StringBuffer();
		final URI uri = buildURI(req, ignored);
	
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			Logger.info("< doPut()");
			return;
		}

		List<BasicHeader> headers = new ArrayList<BasicHeader>(1);
		headers.add(new BasicHeader("Location", uri.toASCIIString()));
		respond(res, getRedirectSC(req), headers);
			
		Logger.info("< doPut(" + uri.toASCIIString() + ")");
	}

	
	@Override
	public void doGet(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doGet(" + req.getRequestURI() + ")");
		final StringBuffer ignored = new StringBuffer();
		final URI uri = buildURI(req, ignored);
	
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}

		List<BasicHeader> headers = new ArrayList<BasicHeader>(1);
		headers.add(new BasicHeader("Location", uri.toASCIIString()));
		respond(res, getRedirectSC(req), headers);
		
		Logger.info("< doGet(" + uri.toASCIIString() + ")");
	}
	

	@Override
	public void doHead(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doHead(" + req.getRequestURI() + ")");
		final StringBuffer ignored = new StringBuffer();
		final URI uri = buildURI(req, ignored);
	
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}

		List<BasicHeader> headers = new ArrayList<BasicHeader>(1);
		headers.add(new BasicHeader("Location", uri.toASCIIString()));
		respond(res, getRedirectSC(req), headers);
		
		Logger.info("< doHead(" + uri.toASCIIString() + ")");
	}

	
	@Override
	public void doDelete(HttpServletRequest req, final HttpServletResponse res) {
		Logger.info("> doDelete(" + req.getRequestURI() + ")");
		final StringBuffer ignored = new StringBuffer();
		final URI uri = buildURI(req, ignored);
	
		if (uri == null) {
			respond(res, HttpStatus.SC_SERVICE_UNAVAILABLE);
			return;
		}

		List<BasicHeader> headers = new ArrayList<BasicHeader>(1);
		headers.add(new BasicHeader("Location", uri.toASCIIString()));
		respond(res, getRedirectSC(req), headers);
		
		Logger.info("< doDelete(" + uri.toASCIIString() + ")");
	}

	
    private int getRedirectSC(final HttpServletRequest req ) {
    	final String key = "status_code";
    	//HttpStatus.SC_MOVED_PERMANENTLY;
    	final int rv = HttpStatus.SC_TEMPORARY_REDIRECT; 
        return rv;
    }

		
	private void respond(final HttpServletResponse res, final int sc, List<BasicHeader> headers) {
        res.setStatus(sc);
        res.setContentType("text/xml;charset=UTF-8");
        
        if (headers != null) {
        	for (BasicHeader bh : headers) {
        		res.setHeader(bh.getName(), bh.getValue());
        	}
        }
        
        byte[] data = new byte[] {};
        res.setContentLength(data.length);
        OutputStream os;
		try {
			os = res.getOutputStream();
			os.write(data);
		} catch (IOException ignored) {
			Logger.error("# respond()", ignored);
		}
	}
}
