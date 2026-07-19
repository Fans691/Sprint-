package mg.itu.framework.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mg.itu.framework.annotation.Controller;
import mg.itu.framework.util.ClassUtil;
import mg.itu.framework.util.MethodClassMapping;
import mg.itu.framework.util.UrlMethod;

@WebListener
public class AppStartUpListener implements ServletContextListener {

    private static final String PACKAGE_NAMES_PARAM = "packageNames";
    private static final String CONTROLLERS_ATTRIBUTE = "listController";
    private static final String URL_MAPPINGS_ATTRIBUTE = "listUrlMapping";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        try {
            List<String> packageNames = resolvePackageNames(servletContext);
            Map<UrlMethod, MethodClassMapping> urlMappings = new HashMap<>();
            List<String> controllers = findControllerNames(packageNames, urlMappings);

            servletContext.setAttribute(CONTROLLERS_ATTRIBUTE, Collections.unmodifiableList(controllers));
            servletContext.setAttribute(URL_MAPPINGS_ATTRIBUTE, Collections.unmodifiableMap(urlMappings));

            servletContext.log(String.format(
                    "Application initialisée avec succès : %d package(s), %d contrôleur(s), %d mapping(s) URL.",
                    packageNames.size(),
                    controllers.size(),
                    urlMappings.size()));

        } catch (RuntimeException e) {
            servletContext.log("Erreur lors de l'initialisation de l'application.", e);
            throw e;
        } catch (Exception e) {
            servletContext.log("Erreur lors de l'initialisation de l'application.", e);
            throw new IllegalStateException("Impossible d'initialiser l'application.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(CONTROLLERS_ATTRIBUTE);
        servletContext.removeAttribute(URL_MAPPINGS_ATTRIBUTE);
        servletContext.log("Application arrêtée.");
    }

    private List<String> resolvePackageNames(ServletContext servletContext) {
        String rawPackageNames = servletContext.getInitParameter(PACKAGE_NAMES_PARAM);

        if (rawPackageNames == null || rawPackageNames.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Le context-param '" + PACKAGE_NAMES_PARAM + "' est introuvable ou vide dans web.xml.");
        }

        Set<String> packageNames = new LinkedHashSet<>();
        for (String packageName : rawPackageNames.split(";")) {
            String trimmedPackageName = packageName.trim();
            if (!trimmedPackageName.isEmpty()) {
                packageNames.add(trimmedPackageName);
            }
        }

        if (packageNames.isEmpty()) {
            throw new IllegalStateException(
                    "Le context-param '" + PACKAGE_NAMES_PARAM + "' ne contient aucun package valide.");
        }

        servletContext.log("Packages scannés : " + packageNames);
        return new ArrayList<>(packageNames);
    }

    private List<String> findControllerNames(
            List<String> packageNames,
            Map<UrlMethod, MethodClassMapping> urlMappings) {

        List<Class<?>> controllerClasses = ClassUtil.getClassesWithAnnotation(
                packageNames,
                urlMappings,
                Controller.class);

        List<String> controllerNames = new ArrayList<>();
        for (Class<?> controllerClass : controllerClasses) {
            controllerNames.add(controllerClass.getName());
        }

        return controllerNames;
    }
}
