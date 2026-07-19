package mg.itu.framework.util;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.ServletContext;

public final class SpringContextManager {

    public static final String SPRING_CONTEXT_ATTRIBUTE = "springApplicationContext";

    private static final String SPRING_CONTEXT_CLASS =
            "org.springframework.context.annotation.AnnotationConfigApplicationContext";

    private SpringContextManager() {
    }

    public static Object initialize(
            ServletContext servletContext,
            List<String> packageNames,
            List<Class<?>> controllerClasses) {
        Object existingContext = servletContext.getAttribute(SPRING_CONTEXT_ATTRIBUTE);
        if (existingContext != null) {
            servletContext.log("Conteneur Spring déjà initialisé, réutilisation de l'instance existante.");
            return existingContext;
        }

        try {
            Class<?> contextClass = Class.forName(SPRING_CONTEXT_CLASS);
            Object springContext = contextClass.getDeclaredConstructor().newInstance();

            Method scanMethod = contextClass.getMethod("scan", String[].class);
            scanMethod.invoke(springContext, (Object) packageNames.toArray(new String[0]));
            registerControllerBeans(servletContext, contextClass, springContext, controllerClasses);

            Method refreshMethod = contextClass.getMethod("refresh");
            refreshMethod.invoke(springContext);

            servletContext.setAttribute(SPRING_CONTEXT_ATTRIBUTE, springContext);
            servletContext.log("Conteneur Spring initialisé une seule fois pour les packages : " + packageNames);
            return springContext;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Spring est introuvable. Ajoutez les dépendances Spring dans le classpath de l'application.",
                    e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible d'initialiser le conteneur Spring.", e);
        }
    }

    private static void registerControllerBeans(
            ServletContext servletContext,
            Class<?> contextClass,
            Object springContext,
            List<Class<?>> controllerClasses) {

        try {
            Method registerBeanMethod = contextClass.getMethod("registerBean", Class.class);
            for (Class<?> controllerClass : controllerClasses) {
                registerBeanMethod.invoke(springContext, controllerClass);
            }
        } catch (NoSuchMethodException e) {
            servletContext.log("Version de Spring sans registerBean(Class) : "
                    + "les contrôleurs doivent aussi être annotés avec @Component.");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible d'enregistrer les contrôleurs dans Spring.", e);
        }
    }

    public static void close(ServletContext servletContext) {
        Object springContext = servletContext.getAttribute(SPRING_CONTEXT_ATTRIBUTE);
        if (springContext == null) {
            return;
        }

        try {
            Method closeMethod = springContext.getClass().getMethod("close");
            closeMethod.invoke(springContext);
            servletContext.log("Conteneur Spring fermé.");
        } catch (ReflectiveOperationException e) {
            servletContext.log("Impossible de fermer correctement le conteneur Spring.", e);
        } finally {
            servletContext.removeAttribute(SPRING_CONTEXT_ATTRIBUTE);
        }
    }

    public static Object getBean(Object springContext, Class<?> beanClass) {
        if (springContext == null) {
            return null;
        }

        try {
            Method getBeanMethod = springContext.getClass().getMethod("getBean", Class.class);
            return getBeanMethod.invoke(springContext, beanClass);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
