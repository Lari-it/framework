package itu.webdynamique.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    private Map<VerbUrl, Mapping> urlMappingMap;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext servletContext = config.getServletContext();
        Object attribute = servletContext.getAttribute(FrameworkContextListener.MAPPING_MAP_ATTRIBUTE);

        if (attribute instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<VerbUrl, Mapping> sharedMap = (Map<VerbUrl, Mapping>) attribute;
            this.urlMappingMap = sharedMap;
            return;
        }

        this.urlMappingMap = new HashMap<>();

        String packageToScan = config.getInitParameter("package_controllers");
        MappingInitializer initializer = new MappingInitializer();
        initializer.initializeMappings(packageToScan, this.urlMappingMap);
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
            out.println("Requete  : " + cle);
            out.println("Classe   : " + mapping.getClassName());
            out.println("Methode  : " + mapping.getMethodName());

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