package mg.itu.framework.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mg.itu.framework.annotation.Controller;
import mg.itu.framework.util.ClassUtil;
import mg.itu.framework.util.MethodClassMapping;
import mg.itu.framework.util.UrlMethod;

@WebListener
public class AppStartUpListener implements ServletContextListener {

    private final List<String> listController = new ArrayList<>();
    private final Map<UrlMethod, MethodClassMapping> listUrlMapping = new HashMap<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {

            String packageName = sce.getServletContext().getInitParameter("packageNames");

            if (packageName == null || packageName.trim().isEmpty()) {
                throw new RuntimeException(
                        "Le context-param 'packageNames' est introuvable dans web.xml.");
            }

            List<String> packageNames = List.of(packageName.split(";"));

            System.out.println("Packages scannés : " + packageNames);

            List<Class<?>> controllers = ClassUtil.getClassesWithAnnotation(
                    packageNames,
                    listUrlMapping,
                    Controller.class);

            for (Class<?> controller : controllers) {
                listController.add(controller.getName());
                System.out.println("Controller trouvé : " + controller.getName());
            }

            sce.getServletContext().setAttribute("listController", listController);
            sce.getServletContext().setAttribute("listUrlMapping", listUrlMapping);

            System.out.println("Application initialisée avec succès.");

        } catch (Throwable e) {
            System.err.println("Erreur lors de l'initialisation de l'application :");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application arrêtée !");
    }
}