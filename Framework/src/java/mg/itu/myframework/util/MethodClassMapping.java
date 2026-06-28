package mg.itu.myframework.util;

import java.lang.reflect.Method;

public class MethodClassMapping {
    private Class<?> classe;
    private Method methode;

    public MethodClassMapping(Class<?> classe, Method methode) {
        this.classe = classe;
        this.methode = methode;
    }

    public Class<?> getClasse() {
        return classe;
    }
    public void setClasse(Class<?> classe) {
        this.classe = classe;
    }
    public Method getMethode() {
        return methode;
    }
    public void setMethode(Method methode) {
        this.methode = methode;
    }
    
}
