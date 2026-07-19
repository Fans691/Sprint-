package mg.itu.framework.util;

public class MethodExecutor {

    public static Object execute(MethodClassMapping mapping) throws Exception {
        return execute(mapping, null);
    }

    public static Object execute(MethodClassMapping mapping, Object springContext) throws Exception {
        Object instance = SpringContextManager.getBean(springContext, mapping.getClasse());
        if (instance == null) {
            instance = mapping.getClasse().getDeclaredConstructor().newInstance();
        }
        mapping.getMethode().setAccessible(true);
        return mapping.getMethode().invoke(instance);
    }
}
