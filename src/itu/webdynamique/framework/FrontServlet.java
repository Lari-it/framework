package itu.webdynamique.framework;

import itu.webdynamique.framework.annotation.Controller;
import itu.webdynamique.framework.annotation.UrlMapping;
import itu.webdynamique.framework.util.PackageScanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    private HashMap<VerbUrl, Mapping> urlMappingMap = new HashMap<>();

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

                if (!cls.isAnnotationPresent(Controller.class))
                    continue;

                for (Method method : cls.getDeclaredMethods()) {

                    if (!method.isAnnotationPresent(UrlMapping.class))
                        continue;

                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                    String url = annotation.value();
                    String methodeHttp = annotation.method();

                    VerbUrl cle = new VerbUrl(url, methodeHttp);

                    if (urlMappingMap.containsKey(cle)) {
                        throw new ServletException(
                                "Conflit : " + cle + " est declaree deux ");
                    }

                    urlMappingMap.put(cle, new Mapping(cls.getName(), method.getName()));

                    System.out.println("[framework] enregistre : "
                            + cle + " -> "
                            + cls.getSimpleName() + "." + method.getName() + "()");
                }
            }

            System.out.println("[framework] " + urlMappingMap.size() + " mapping(s) charge(s).");

        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan", e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String httpMethod = request.getMethod().toUpperCase();

        String contextPath = request.getContextPath();
        String requestedUrl = request.getRequestURI().substring(contextPath.length());

        if (requestedUrl.equals("/") || requestedUrl.isEmpty()) {
            out.println("=== Mappings supportes ===");
            for (VerbUrl cle : urlMappingMap.keySet()) {
                out.println(cle + "  ->  " + urlMappingMap.get(cle));
            }
            return;
        }

        VerbUrl cle = new VerbUrl(requestedUrl, httpMethod);

        if (urlMappingMap.containsKey(cle)) {
            Mapping mapping = urlMappingMap.get(cle);
            out.println("=== URL reconnue ===");
            out.println("Requête  : " + cle);
            out.println("Classe   : " + mapping.getClassName());
            out.println("Methode  : " + mapping.getMethodName());

            //sprint3 BIS
            try {

                Class<?> laClasse = Class.forName(mapping.getClassName());
                Object instance = laClasse.getDeclaredConstructor().newInstance();
                Method laMethode = laClasse.getDeclaredMethod(mapping.getMethodName());

                laMethode.invoke(instance);

                out.println("Methode executee. (voir console Tomcat)");

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Erreur lors de l'execution : " + e.getMessage());
            }
            return;
        }

        //URL inconnue → erreur + liste des mappings disponibles
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.println("=== URL non supportee ===");
        out.println("Demandee : " + cle);
        out.println("");
        out.println("URLs disponibles :");
        for (VerbUrl k : urlMappingMap.keySet()) {
            out.println("  " + k);
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