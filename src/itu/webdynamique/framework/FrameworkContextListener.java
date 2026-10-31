package itu.webdynamique.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.HashMap;
import java.util.Map;

public class FrameworkContextListener implements ServletContextListener {

    public static final String MAPPING_MAP_ATTRIBUTE = "framework.urlMappingMap";
    public static final String SESSION_MAP_ATTRIBUTE = "framework.sessionMap";
    public static final String AUTH_SESSION_NAME_ATTRIBUTE = "framework.authSessionName";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();

        String packageToScan = context.getInitParameter("package_controllers");

        Map<VerbUrl, Mapping> mappings = new HashMap<>();
        MappingInitializer initializer = new MappingInitializer();

        try {
            initializer.initializeMappings(packageToScan, mappings);
            context.setAttribute(MAPPING_MAP_ATTRIBUTE, mappings);
            System.out.println("[framework] mappings charges au demarrage de l'application.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des mappings au demarrage", e);
        }

        Map<String, Session> sessionMap = new HashMap<>();
        context.setAttribute(SESSION_MAP_ATTRIBUTE, sessionMap);
        System.out.println("[framework] session map initialisee au demarrage de l'application.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(MAPPING_MAP_ATTRIBUTE);
        event.getServletContext().removeAttribute(SESSION_MAP_ATTRIBUTE);
    }
}