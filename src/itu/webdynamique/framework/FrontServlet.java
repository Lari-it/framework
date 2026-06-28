package itu.webdynamique.framework;

import itu.webdynamique.framework.annotation.Controller;
import itu.webdynamique.framework.annotation.UrlMapping;
import itu.webdynamique.framework.util.PackageScanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    
    private HashMap<String, Mapping> urlMappingMap = new HashMap<>();

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
                if (!cls.isAnnotationPresent(Controller.class)) continue;

                
                for (Method method : cls.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(UrlMapping.class)) continue;

                    
                    String url = method.getAnnotation(UrlMapping.class).value();

                    
                    if (urlMappingMap.containsKey(url)) {
                        throw new ServletException(
                            "URL en conflit : '" + url + "' declaree deux fois."
                        );
                    }

                    urlMappingMap.put(url, new Mapping(cls.getName(), method.getName()));
                    System.out.println("[Framework] URL enregistree : " + url
                        + " -> " + cls.getSimpleName() + "." + method.getName() + "()");
                }
            }

            System.out.println("[Framework] " + urlMappingMap.size() + " URL supportee.");

        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controllers", e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        
        String contextPath = request.getContextPath();
        String requestedUrl = request.getRequestURI().substring(contextPath.length());

        //on affiche toutes les URLs supportees
        if (requestedUrl.equals("/") || requestedUrl.isEmpty()) {
            out.println("=== URLs supportees par le framework ===");
            for (String url : urlMappingMap.keySet()) {
                out.println(url + "  ->  " + urlMappingMap.get(url));
            }
            return;
        }

        //URL demandee connue -> on affiche ses info
        if (urlMappingMap.containsKey(requestedUrl)) {
            Mapping mapping = urlMappingMap.get(requestedUrl);
            out.println("=== URL reconnue ===");
            out.println("URL      : " + requestedUrl);
            out.println("Classe   : " + mapping.getClassName());
            out.println("Methode  : " + mapping.getMethodName());
            return;
        }

        //URL inconnue -> exception + liste des URL connues
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.println("=== Erreur : URL non supportee ===");
        out.println("URL demandee : " + requestedUrl);
        out.println("");
        out.println("URLs disponibles :");
        for (String url : urlMappingMap.keySet()) {
            out.println("  " + url);
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