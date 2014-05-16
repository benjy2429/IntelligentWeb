package servlets;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServletListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("SERVLET ENDING!!");
		
	}

	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("SERVLET IS ABOUT TO BEGIN!!");
		
	}

}
