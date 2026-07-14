package mg.itu.framework.util;

public class MethodExecutor {

    public static Object execute(MethodClassMapping mapping) throws Exception {
        Object instance = mapping.getClasse().getDeclaredConstructor().newInstance();
        mapping.getMethode().setAccessible(true);
        return mapping.getMethode().invoke(instance);
    }
}
