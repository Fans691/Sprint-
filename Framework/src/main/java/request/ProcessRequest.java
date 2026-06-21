package request;

import framework.annotation.FrameworkAnnotation;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ProcessRequest extends HttpServlet {
    private static final String DEFAULT_SCAN_PACKAGE = "request";

    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
            processRequest(req, res);
        }

        public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
            processRequest(req, res);
        }

        public void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
            String scanPackage = req.getParameter("scanPackage");
            String report = scanJavaClasses(scanPackage);

            res.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println(report);
        }

        public String scanJavaClasses() {
            return scanJavaClasses(null);
        }

        public String scanJavaClasses(String packageName) {
            String resolvedPackage = packageName;
            if (resolvedPackage == null || resolvedPackage.trim().isEmpty()) {
                resolvedPackage = DEFAULT_SCAN_PACKAGE;
            }

            StringBuilder report = new StringBuilder();
            report.append("Package scannee: ").append(resolvedPackage).append('\n');

            List<Class<?>> classes = findClassesInPackage(resolvedPackage);
            if (classes.isEmpty()) {
                report.append("Aucune classe trouvee.");
                return report.toString();
            }

            for (Class<?> clazz : classes) {
                report.append("Classe: ").append(clazz.getName()).append('\n');
                boolean classHasAnnotation = appendAnnotations(report, "CLASS", clazz.getSimpleName(), clazz.getAnnotations());

                for (Field field : clazz.getDeclaredFields()) {
                    boolean fieldHasAnnotation = appendAnnotations(report, "FIELD", clazz.getSimpleName() + "." + field.getName(), field.getAnnotations());
                    classHasAnnotation = classHasAnnotation || fieldHasAnnotation;
                }

                for (Method method : clazz.getDeclaredMethods()) {
                    boolean methodHasAnnotation = appendAnnotations(report, "METHOD", clazz.getSimpleName() + "." + method.getName() + "()", method.getAnnotations());
                    classHasAnnotation = classHasAnnotation || methodHasAnnotation;
                }

                if (!classHasAnnotation) {
                    report.append("  Aucune annotation runtime detectee.\n");
                }

                report.append('\n');
            }

            return report.toString();
        }

        private boolean appendAnnotations(StringBuilder report, String targetType, String elementName, Annotation[] annotations) {
            boolean found = false;

            for (Annotation annotation : annotations) {
                found = true;
                report.append("  ")
                      .append(targetType)
                      .append(" ")
                      .append(elementName)
                      .append(" -> ")
                      .append(formatAnnotation(annotation))
                      .append('\n');
            }

            return found;
        }

        private String formatAnnotation(Annotation annotation) {
            if (annotation instanceof FrameworkAnnotation) {
                FrameworkAnnotation frameworkAnnotation = (FrameworkAnnotation) annotation;
                if (frameworkAnnotation.value() != null && !frameworkAnnotation.value().trim().isEmpty()) {
                    return "@" + frameworkAnnotation.value().trim() + " (" + annotation.annotationType().getSimpleName() + ")";
                }
            }

            return "@" + annotation.annotationType().getSimpleName();
        }

        private List<Class<?>> findClassesInPackage(String packageName) {
            if (packageName == null || packageName.trim().isEmpty()) {
                return Collections.emptyList();
            }

            String packagePath = packageName.replace('.', '/');
            Set<String> classNames = new LinkedHashSet<String>();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            try {
                Enumeration<URL> resources = classLoader.getResources(packagePath);
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    String protocol = resource.getProtocol();

                    if ("file".equals(protocol)) {
                        String decodedPath = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8.name());
                        collectClassesFromDirectory(packageName, Paths.get(decodedPath), classNames);
                    } else if ("jar".equals(protocol)) {
                        collectClassesFromJar(resource, packagePath, classNames);
                    }
                }
            } catch (Exception exception) {
                return Collections.emptyList();
            }

            List<Class<?>> classes = new ArrayList<Class<?>>();
            for (String className : classNames) {
                try {
                    classes.add(Class.forName(className, false, classLoader));
                } catch (ClassNotFoundException exception) {
                    // Ignore classes that cannot be loaded from the current runtime.
                }
            }

            return classes;
        }

        private void collectClassesFromDirectory(String packageName, Path directory, Set<String> classNames) throws IOException {
            if (directory == null || !Files.exists(directory)) {
                return;
            }

            Files.walk(directory)
                 .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".class"))
                 .forEach(path -> {
                     Path relativePath = directory.relativize(path);
                     String className = packageName + "." + relativePath.toString()
                         .replace('/', '.')
                         .replace('\\', '.')
                         .replaceAll("\\.class$", "");
                     classNames.add(className);
                 });
        }

        private void collectClassesFromJar(URL resource, String packagePath, Set<String> classNames) throws Exception {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            JarFile jarFile = connection.getJarFile();

            try {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    if (!entry.isDirectory() && entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                        classNames.add(entryName.replace('/', '.').replaceAll("\\.class$", ""));
                    }
                }
            } finally {
                jarFile.close();
            }
        }
    }
