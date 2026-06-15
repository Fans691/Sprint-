package request;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class ProcessRequest extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
            processRequest(req, res);
        }

        public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
            processRequest(req, res);
        }

        public void processRequest(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
            String lien = req.getRequestURL().toString();
            String[] parts = lien.split("/");
            String lastPart = parts[parts.length - 1];
            PrintWriter out = res.getWriter();

            out.println("URL: " + lien);            
        }
    }
