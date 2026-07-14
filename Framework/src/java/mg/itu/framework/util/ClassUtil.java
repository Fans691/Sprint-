package mg.itu.framework.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import mg.itu.framework.annotation.UrlMapping;
import java.lang.reflect.Method;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;


import java.lang.annotation.Annotation;

public class ClassUtil {

    public static List<Class<?>> getClassesWithAnnotation(List<String> packageNames, Map<UrlMethod, MethodClassMapping> urlMappings, Class<? extends Annotation> annotation) {
        List<Class<?>> classes = new ArrayList<>();
        for (String packageName : packageNames) {
            classes.addAll(getClasses(packageName));
        }
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) { 
                        UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);
                        String url = normalizeUrl(urlMapping.url());
                        String methode = normalizeMethod(urlMapping.method());
                        MethodClassMapping mapping = new MethodClassMapping(clazz, method);
                        UrlMethod urlMethod = new UrlMethod(url, methode);
                        if(urlMappings.containsKey(urlMethod)) throw new RuntimeException("URL "+ url + " (" + methode +")" + "existe deja.");
                        urlMappings.put(urlMethod, mapping);
                    }
                }
            }
        }
        return result;
    }

    public static List<Class<?>> getClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();

        try (ScanResult scan = new ClassGraph()
                .enableClassInfo()
                .scan()) {

            for (ClassInfo classInfo : scan.getAllClasses()) {
                try {
                    if (classInfo.getPackageName().equals(packageName)) {
                        classes.add(classInfo.loadClass());
                    }
                } catch (Throwable e) {
                }
            }
        }

        return classes;
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "/";
        }

        String normalizedUrl = url.trim();
        if (!normalizedUrl.startsWith("/")) {
            normalizedUrl = "/" + normalizedUrl;
        }
        return normalizedUrl;
    }

    private static String normalizeMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "GET";
        }
        return method.trim().toUpperCase();
    }

}
