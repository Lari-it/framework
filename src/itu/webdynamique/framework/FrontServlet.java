package itu.webdynamique.framework;

import itu.webdynamique.framework.annotation.Controller;
import itu.webdynamique.framework.util.PackageScanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    
    private List<Class<?>> controllers = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            
            String packageToScan = config.getInitParameter("package_controllers");
            if (packageToScan == null || packageToScan.trim().isEmpty()) {
                throw new ServletException("Parametre 'package_controllers' manquant dans web.xml");
            }

           
            List<Class<?>> allClasses = PackageScanner.findByPackage(packageToScan);

           
            for (Class<?> cls : allClasses) {
                if (cls.isAnnotationPresent(Controller.class)) {
                    controllers.add(cls);
                }
            }

            System.out.println("[Framework] " + controllers.size() + " controleur(s) detecte(s) dans " + packageToScan);

        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controleurs", e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String uri = request.getRequestURI();
        out.println("[Framework] URI complete detectee : " + uri);

        String[] splitUri = uri.split("/");
        String lastInUri = "";
        if (splitUri.length > 0) {
            lastInUri = splitUri[splitUri.length - 1];
        }
        out.println("[Framework] Action finale extraite : " + lastInUri);

        
        out.println("[Framework] Controleurs detectes au demarrage : " + controllers.size());
        for (Class<?> c : controllers) {
            out.println("   -> " + c.getName());
        }
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