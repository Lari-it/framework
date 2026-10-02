package itu.webdynamique.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import itu.webdynamique.framework.annotation.Json;
import com.google.gson.Gson;

import java.lang.reflect.Parameter;

public class FrontServlet extends HttpServlet {

    private Map<VerbUrl, Mapping> urlMappingMap;

    private String prefixe;
    private String suffixe;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext servletContext = config.getServletContext();
        Object attribute = servletContext.getAttribute(
                FrameworkContextListener.MAPPING_MAP_ATTRIBUTE);

        if (attribute instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<VerbUrl, Mapping> sharedMap = (Map<VerbUrl, Mapping>) attribute;
            this.urlMappingMap = sharedMap;
        } else {
            this.urlMappingMap = new HashMap<>();
            String packageToScan = config.getInitParameter("package_controllers");
            MappingInitializer initializer = new MappingInitializer();
            initializer.initializeMappings(packageToScan, this.urlMappingMap);
        }
        this.prefixe = config.getInitParameter("prefixe");
        this.suffixe = config.getInitParameter("suffixe");

        System.out.println("[Framework] prefixe = " + prefixe);
        System.out.println("[Framework] suffixe = " + suffixe);
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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String httpMethod = request.getMethod().toUpperCase();
        String contextPath = request.getContextPath();
        String requestedUrl = request.getRequestURI().substring(contextPath.length());

        if (requestedUrl.equals("/") || requestedUrl.isEmpty()) {
            response.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("=== Mappings supportes ===");
            for (VerbUrl cle : urlMappingMap.keySet()) {
                out.println(cle + "  ->  " + urlMappingMap.get(cle));
            }
            return;
        }

        VerbUrl cle = new VerbUrl(requestedUrl, httpMethod);

        if (urlMappingMap.containsKey(cle)) {
            Mapping mapping = urlMappingMap.get(cle);

            try {
                Class<?> laClasse = Class.forName(mapping.getClassName());
                Object instance = laClasse.getDeclaredConstructor().newInstance();
                Method laMethode = trouverMethode(laClasse, mapping.getMethodName());

                Object resultat = executerMethode(laMethode, instance, request);

                if (laMethode.isAnnotationPresent(Json.class)) {

                    response.setContentType("application/json;charset=UTF-8");
                    PrintWriter out = response.getWriter();

                    if (resultat instanceof String) {
                        out.print((String) resultat);
                    } else {
                        Gson gson = new Gson();
                        out.print(gson.toJson(resultat));
                    }

                } else {

                    if (resultat instanceof ModelAndView) {
                        ModelAndView mv = (ModelAndView) resultat;
                        String cheminJsp = prefixe + mv.getUrl() + suffixe;
                        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                        getServletContext().getRequestDispatcher(cheminJsp).forward(request, response);
                    } else {
                        response.setContentType("text/plain;charset=UTF-8");
                        response.getWriter().println("Methode executee sans retour.");
                    }

                }

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("Erreur : " + e.getMessage());
            }
            return;
        }

        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        PrintWriter out = response.getWriter();
        out.println("URL non supportee : " + requestedUrl);
        System.out.println("URL recue : " + requestedUrl);
        System.out.println("Methode HTTP : " + httpMethod);
        out.println("URLs disponibles :");
        for (VerbUrl k : urlMappingMap.keySet()) {
            out.println("  " + k);
        }

    }

    private Object executerMethode(Method laMethode, Object instance, HttpServletRequest request)
            throws Exception {

        Parameter[] params = laMethode.getParameters();

        if (params.length == 0) {
            return laMethode.invoke(instance);
        }

        Object[] valeurs = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String nom = params[i].getName();
            Class<?> type = params[i].getType();
            String valeurBrute = request.getParameter(nom);

            if (type == String.class) {
                valeurs[i] = valeurBrute;
            } else if (type == int.class || type == Integer.class) {
                valeurs[i] = valeurBrute != null ? Integer.parseInt(valeurBrute) : 0;
            } else if (type == double.class || type == Double.class) {
                valeurs[i] = valeurBrute != null ? Double.parseDouble(valeurBrute) : 0.0;
            } else if (type == long.class || type == Long.class) {
                valeurs[i] = valeurBrute != null ? Long.parseLong(valeurBrute) : 0L;
            } else if (type == float.class || type == Float.class) {
                valeurs[i] = valeurBrute != null ? Float.parseFloat(valeurBrute) : 0.0f;
            } else if (type == boolean.class || type == Boolean.class) {
                valeurs[i] = valeurBrute != null ? Boolean.parseBoolean(valeurBrute) : false;
            } else {
                valeurs[i] = null;
            }
        }

        return laMethode.invoke(instance, valeurs);
    }

    private Method trouverMethode(Class<?> laClasse, String nomMethode) throws Exception {
        for (Method m : laClasse.getDeclaredMethods()) {
            if (m.getName().equals(nomMethode)) {
                return m;
            }
        }
        throw new Exception("Methode introuvable : " + nomMethode);
    }
}