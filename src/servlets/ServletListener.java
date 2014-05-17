package servlets;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import projectFiles.RDFConnector;

public class ServletListener implements ServletContextListener {
	//Logger
	private static final Logger LOGGER = Logger.getLogger(ServletListener.class.getName());
	
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("SERVLET ENDING!!");
		
	}

	public void contextInitialized(ServletContextEvent arg0){
		System.out.println("SERVLET IS ABOUT TO BEGIN!!");
		try {
			RDFConnector datastoreConn = new RDFConnector(arg0.getServletContext().getRealPath(""));
			arg0.getServletContext().setAttribute("rdfConnector", datastoreConn);
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}

}
