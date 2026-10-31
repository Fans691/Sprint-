package mg.itu.framework.controller;

import java.io.*;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;

import org.springframework.context.ApplicationContext;

import mg.itu.framework.annotation.Controller;
import mg.itu.framework.model.MethodClassMapping;
import mg.itu.framework.model.UrlMethod;
import mg.itu.framework.model.ModelAndView;
import mg.itu.framework.annotation.WebApi;
import mg.itu.framework.annotation.Auth;
import mg.itu.framework.session.MySession;
import mg.itu.framework.util.AuthUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@MultipartConfig
public class FrontControllerServlet extends HttpServlet {

    private List<String> listController = new ArrayList<>();
    private Map<UrlMethod, MethodClassMapping> listUrlMapping = new HashMap<>();
    private String prefix;
    private String suffix;
    private String sessionAuth;
    private String roleAuth;
    private ApplicationContext springContext;
    private MethodeController methodeController = new MethodeController();

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        try {
            ServletContext context = getServletContext();

            this.prefix = (String) context.getAttribute("prefix");
            this.suffix = (String) context.getAttribute("suffix");
            this.sessionAuth = (String) context.getAttribute("sessionAuth");
            this.roleAuth = (String) context.getAttribute("roleAuth");
            this.springContext = (ApplicationContext) context.getAttribute("springContext");

            List<String> controllersFromContext = (List<String>) context.getAttribute("listController");
            if (controllersFromContext != null) {
                this.listController = controllersFromContext;
            }

            Map<UrlMethod, MethodClassMapping> mappingsFromContext =
                    (Map<UrlMethod, MethodClassMapping>) context.getAttribute("listUrlMapping");
            if (mappingsFromContext != null) {
                this.listUrlMapping = mappingsFromContext;
            }
        } catch (Exception e) {
            throw new ServletException("Erreur initialisation", e);
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String path = uri.substring(req.getContextPath().length());

        String method = req.getMethod();
        MethodClassMapping mapping = getMapping(path, method);

        if (mapping != null) {
            invokeMethod(mapping, req, res);
        } else {
            res.setContentType("text/html");
            res.setCharacterEncoding("UTF-8");

            PrintWriter out = res.getWriter();

            out.println("URL : " + path + "<br>");

            out.println("Aucune correspondance trouvée pour l'URL : "
                    + path + "<br>");

            out.println("<br>Liste des URL disponibles : <br>");
            out.println("<table border='1'>");
            out.println("<tr><th>URL</th><th>Classe</th><th>Méthode</th></tr>");

            for (Map.Entry<UrlMethod, MethodClassMapping> entry
                    : listUrlMapping.entrySet()) {

                UrlMethod url = entry.getKey();
                MethodClassMapping m = entry.getValue();

                out.println("<tr><td>"
                        + url.getUrl()
                        + " (" + url.getMethod() + ")</td><td>");

                out.println(m.getClasse().getName() + "</td><td>");

                out.println(m.getMethode().getName()
                        + "</td></tr>");
            }

            out.println("</table>");

            out.println("<br>Liste des classes contrôleurs : <br>");

            for (String controller : listController) {
                out.println("- " + controller + "<br>");
            }
        }
    }

    private MethodClassMapping getMapping(String urlName, String method) {
        for (Map.Entry<UrlMethod, MethodClassMapping> entry : listUrlMapping.entrySet()) {
            UrlMethod url = entry.getKey();
            if (url.getUrl().equals(urlName) && url.getMethod().equals(method)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void invokeMethod(MethodClassMapping mapping, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        try {
            Auth auth = mapping.getMethode().getAnnotation(Auth.class);
            if (auth != null) {
                HttpSession session = req.getSession(false);
                if (getSessionValue(session, sessionAuth) == null) {
                    res.setContentType("text/html");
                    res.setCharacterEncoding("UTF-8");
                    out.println("<p>Cette methode exige une authentification</p>");
                    return;
                }
                if (!AuthUtil.verifyRoleAccess(getSessionValue(session, roleAuth), auth.role())) {
                    res.setContentType("text/html");
                    res.setCharacterEncoding("UTF-8");
                    out.println("<p>Vous n'avez pas le role requis pour acceder a cette methode.</p>");
                    return;
                }
            }

            Object instance = mapping.getClasse().getDeclaredConstructor().newInstance();

            Class<?>[] parameterTypes = mapping.getMethode().getParameterTypes();
            Object result;

            if (parameterTypes.length == 0) {
                result = mapping.getMethode().invoke(instance);
            } else if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(springContext.getClass())) {
                result = mapping.getMethode().invoke(instance, springContext);
            } else {
                Object[] parameters = methodeController.getMethodParameters(
                        req, mapping.getMethode(), springContext);
                result = mapping.getMethode().invoke(instance, parameters);
            }
            
            if (mapping.getMethode().isAnnotationPresent(WebApi.class)) {

                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");

                PrintWriter jsonWriter = res.getWriter();
                if (result instanceof String) {
                    jsonWriter.println((String) result);

                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String json = objectMapper.writeValueAsString(result);

                    jsonWriter.println(json);
                }

            }
            else if (result instanceof ModelAndView) {
                ModelAndView modelAndView = (ModelAndView) result;
                for (Map.Entry<String, Object> entry : modelAndView.getModel().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
                String viewPath = getPrefix() + modelAndView.getView() + getSuffix();
                req.getRequestDispatcher(viewPath).forward(req, res);
            } else {
                out.println("La méthode " + mapping.getMethode().getName() +
                        " de la classe " + mapping.getClasse().getName() +
                        " ne retourne pas un objet ModelAndView.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("Erreur lors du traitement de la requête : " + e.getMessage());
        }
    }

    private Object getSessionValue(HttpSession session, String key) {
        if (session == null || key == null) {
            return null;
        }
        return new MySession(session).getAttribute(key);
    }

}













