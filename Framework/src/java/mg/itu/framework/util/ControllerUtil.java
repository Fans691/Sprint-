package mg.itu.framework.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;

import mg.itu.framework.model.MethodClassMapping;

public class ControllerUtil {
    private static final String MAP_CONTROLLERS_KEY = "mapControllers";

    public static Object getInstance(MethodClassMapping mapping, ServletContext context)
            throws ReflectiveOperationException {
        Map<String, Object> mapControllers = getMapControllers(context);
        Class<?> classe = mapping.getClasse();

        Object instance = mapControllers.get(classe.getName());
        if (instance == null) {
            synchronized (mapControllers) {
                instance = mapControllers.get(classe.getName());
                if (instance == null) {
                    instance = classe.getDeclaredConstructor().newInstance();
                    mapControllers.put(classe.getName(), instance);
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMapControllers(ServletContext context) {
        synchronized (context) {
            Map<String, Object> mapControllers = (Map<String, Object>) context.getAttribute(MAP_CONTROLLERS_KEY);
            if (mapControllers == null) {
                mapControllers = new ConcurrentHashMap<>();
                context.setAttribute(MAP_CONTROLLERS_KEY, mapControllers);
            }
            return mapControllers;
        }
    }
}
