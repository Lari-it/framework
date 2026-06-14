package itu.webdynamique.framework; 

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        //recup URI complete
        String uri = request.getRequestURI();
        out.println("[Framework] URI complète détectée : " + uri);

        // decoupe URI
        String[] splitUri = uri.split("/");

        //extraction dernier element
        String lastInUri = "";
        if (splitUri.length > 0) {
            lastInUri = splitUri[splitUri.length - 1];
        }

        out.println("[Framework] Action finale extraite : " + lastInUri);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}