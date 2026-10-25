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
import itu.webdynamique.framework.annotation.Auth;
import com.google.gson.Gson;
import jakarta.servlet.annotation.MultipartConfig;
import itu.webdynamique.framework.Binder;

import java.lang.reflect.Parameter;
import java.util.UUID;

@MultipartConfig
public class FrontServlet extends HttpServlet {

    private Map<VerbUrl, Mapping> urlMappingMap;
    private Map<String, Session> sessionMap;

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

        Object sessionAttribute = servletContext.getAttribute(
                FrameworkContextListener.SESSION_MAP_ATTRIBUTE);
        if (sessionAttribute instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Session> sharedSessionMap = (Map<String, Session>) sessionAttribute;
            this.sessionMap = sharedSessionMap;
        } else {
            this.sessionMap = new HashMap<>();
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

                Session session = getOrCreateSession(request);

                if (laMethode.isAnnotationPresent(Auth.class)) {
                    if (session == null || !session.isAuthenticated()) {
                        response.setContentType("text/plain;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().println("Erreur : Authentification requise");
                        return;
                    }
                }

                Object resultat = executerMethode(laMethode, instance, request, session);

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

    private Object executerMethode(Method laMethode, Object instance, HttpServletRequest request, Session session)
            throws Exception {

        Parameter[] params = laMethode.getParameters();

        if (params.length == 0) {
            return laMethode.invoke(instance);
        }

        Object boundParams = Binder.bindParams(params, request, session);

        if (boundParams instanceof Object[]) {
            return laMethode.invoke(instance, (Object[]) boundParams);
        } else {
            return laMethode.invoke(instance, boundParams);
        }
    }

    private Method trouverMethode(Class<?> laClasse, String nomMethode) throws Exception {
        for (Method m : laClasse.getDeclaredMethods()) {
            if (m.getName().equals(nomMethode)) {
                return m;
            }
        }
        throw new Exception("Methode introuvable : " + nomMethode);
    }

    private Session getOrCreateSession(HttpServletRequest request) {
        String sessionId = request.getParameter("sessionId");

        if (sessionId != null && sessionMap.containsKey(sessionId)) {
            return sessionMap.get(sessionId);
        }

        String newSessionId = UUID.randomUUID().toString();
        Session newSession = new Session(newSessionId);
        sessionMap.put(newSessionId, newSession);

        return newSession;
    }
}