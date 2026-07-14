package mg.itu.framework;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.itu.framework.util.MethodClassMapping;
import mg.itu.framework.util.MethodExecutor;
import mg.itu.framework.util.ModelAndView;
import mg.itu.framework.util.UrlMethod;

@WebServlet(name = "ProcessRequest", urlPatterns = "/*")
public class ProcessRequest extends HttpServlet {

    private static final String URL_MAPPINGS_ATTRIBUTE = "listUrlMapping";

    private Map<UrlMethod, MethodClassMapping> urlMap;
    private String prefix;
    private String suffix;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        this.urlMap = (Map<UrlMethod, MethodClassMapping>) context.getAttribute(URL_MAPPINGS_ATTRIBUTE);
        this.prefix = context.getInitParameter("view-prefix");
        this.suffix = context.getInitParameter("view-suffix");

        if (this.prefix == null) {
            this.prefix = "";
        }
        if (this.suffix == null) {
            this.suffix = "";
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String url = getRequestPath(req);

        if (url.endsWith(".html")) {
            String cheminPhysique = getServletContext().getRealPath(url);
            File fichier = new File(cheminPhysique);

            if (fichier.exists()) {
                res.setContentType("text/html;charset=UTF-8");
                Files.copy(fichier.toPath(), res.getOutputStream());
                return;
            } else {
                res.sendError(404, "Fichier introuvable");
                return;
            }
        }

        String httpMethod = req.getMethod().toUpperCase();
        UrlMethod cle = new UrlMethod(url, httpMethod);

        MethodClassMapping mapping = (urlMap != null) ? urlMap.get(cle) : null;

        if (mapping == null) {
            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println("Aucune methode trouvee pour l'URL : " + url + " et la Methode : " + httpMethod);
            out.println("");
            out.println("URLs disponibles :");

            if (urlMap != null) {
                for (UrlMethod u : urlMap.keySet()) {
                    MethodClassMapping m = urlMap.get(u);
                    out.println(u.getMethod() + " " + u.getUrl() + "    " + m.getClasse().getName() + "."
                            + m.getMethode().getName() + "()");
                }
            }
            return;
        }

        try {
            Object obj = MethodExecutor.execute(mapping);

            if (obj instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) obj;
                Map<String, Object> map = mv.getModel();

                for (Map.Entry<String, Object> mm : map.entrySet()) {
                    req.setAttribute(mm.getKey(), mm.getValue());
                }
                if (isRedirectView(mv.getView())) {
                    res.sendRedirect(buildRedirectPath(req, mv.getView()));
                    return;
                }

                String path = buildViewPath(mv.getView());
                RequestDispatcher requestDispatcher = req.getRequestDispatcher(path);
                requestDispatcher.forward(req, res);
                return;
            }

            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println("URL     : " + url);
            out.println("Methode : " + mapping.getMethode().getName() + "()");
            out.println("Execution de :" + mapping.getMethode());
            out.println("Resultat : " + obj);

        } catch (Exception e) {
            res.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = res.getWriter();
            out.println("Erreur lors de l'execution de la methode : " + e.getMessage());
            e.printStackTrace(out);
        }
    }

    private String buildViewPath(String view) {
        if (view == null || view.trim().isEmpty()) {
            return "/";
        }

        String trimmedView = view.trim();
        if (trimmedView.startsWith("/")) {
            return trimmedView;
        }

        return this.prefix + trimmedView + this.suffix;
    }

    private boolean isRedirectView(String view) {
        return view != null && view.trim().startsWith("redirect:");
    }

    private String buildRedirectPath(HttpServletRequest req, String view) {
        String redirectPath = view.trim().substring("redirect:".length()).trim();
        if (redirectPath.startsWith("http://") || redirectPath.startsWith("https://")) {
            return redirectPath;
        }
        if (redirectPath.startsWith("/")) {
            return req.getContextPath() + redirectPath;
        }
        return req.getContextPath() + "/" + redirectPath;
    }

    private String getRequestPath(HttpServletRequest req) {
        String path = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (path == null || path.isEmpty()) {
            return "/";
        }

        int pathParamIndex = path.indexOf(';');
        if (pathParamIndex >= 0) {
            path = path.substring(0, pathParamIndex);
        }

        return path;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }
}
