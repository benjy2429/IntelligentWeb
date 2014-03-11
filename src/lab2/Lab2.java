package lab2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.*;
import javax.servlet.http.*;


public class Lab2 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		/* Display some response to the user */
		out.println("<html><head><title>TestServlet</title>");
		out.println("\t<style>body { font-family: 'Lucida Grande', " +
		"'Lucida Sans Unicode';font-size: 13px; }</style>");
		out.println("</head><body>");
		out.println("<h1>The Intelligent Web Lab</h1>");
		out.println("<form action='' method='post'><fieldset><legend>Contact Us:</legend>");
		out.println("<label for='name'>Name:</label><input type='text' name='name' id='name' required><br>");
		out.println("<label for='email'>Email:</label><input type='email' name='email' id='email' required><br>");
		out.println("<label for='comments'>Comments:</label><textarea rows='3' cols='30' name='comments' id='comments' required></textarea><br>");
		out.println("<label for='mailing'>Would you like to sign up for our mailing list?</label><input type='checkbox' name='mailing' id='mailing'><br>");
		out.println("<input type='submit' value='Submit'><input type='reset' value='Reset'>");
		out.println("</fieldset></form>");
		out.println("</body></html>");
		out.close();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/xml");
		File xml = new File(getServletContext().getRealPath("/") + "/customers.xml");
		FileWriter xmlout = new FileWriter(xml, true);
		xmlout.write("<customer>\n");
		xmlout.write("\t<name>" + request.getParameter("name") + "</name>\n");
		xmlout.write("\t<email>" + request.getParameter("email") + "</email>\n");
		String mailing = (request.getParameter("mailing") == null) ? "no" : "yes";
		xmlout.write("\t<mailing>" + mailing + "</mailing>\n");
		xmlout.write("</customer>\n");
		xmlout.close();
		
		
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html><head><title>TestServlet</title>");
		out.println("\t<style>body { font-family: 'Lucida Grande', " +
		"'Lucida Sans Unicode';font-size: 13px; }</style>");
		out.println("<p>Dear " + request.getParameter("name") + "<br>");
		out.println("Many thanks for your subscription</p>");
		out.close();

	}

}
