# Sprint-
Web-dyn->S4 - S6

Sprint - 6 :
- Ajout d'un annotation @api-rest qui s'execute au niveau Method
- Printwiter ref String
- Else to json

Sprint - 10 :
- Ajout de l'annotation @Auth au niveau Method : la methode exige un utilisateur connecte
- La cle de session a verifier est le context-param `sessionAuth`
- MySession est liee a la HttpSession : une session par utilisateur

Sprint - 10 bis :
- @Auth(role = "admin, manager") : la methode exige un utilisateur connecte ayant un des roles listes
- La cle de session contenant le role est le context-param `roleAuth`
- Sans `role`, @Auth exige seulement d'etre connecte

Build : `Framework/run.bat` (Windows) ou `Framework/run.sh`
- JDK 17+ requis (Spring 6), detecte automatiquement
- Les jars manquants de `Framework/lib` sont telecharges depuis Maven Central
- Resultat : `Framework/build/WEB-INF/lib/Framework.jar`, a copier dans `WEB-INF/lib` du projet (Tomcat 10+)

web.xml du projet :
```xml
<context-param>
    <param-name>contextClass</param-name>
    <param-value>org.springframework.web.context.support.AnnotationConfigWebApplicationContext</param-value>
</context-param>
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>com.monprojet.controller</param-value>
</context-param>
<context-param><param-name>packageNames</param-name><param-value>com.monprojet.controller</param-value></context-param>
<context-param><param-name>prefix</param-name><param-value>/WEB-INF/views/</param-value></context-param>
<context-param><param-name>suffix</param-name><param-value>.jsp</param-value></context-param>
<context-param><param-name>sessionAuth</param-name><param-value>user</param-value></context-param>
<context-param><param-name>roleAuth</param-name><param-value>role</param-value></context-param>

<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>

<servlet>
    <servlet-name>FrontControllerServlet</servlet-name>
    <servlet-class>mg.itu.framework.controller.FrontControllerServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>FrontControllerServlet</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

Exemple :
```java
@Controller
public class AuthController {
    @UrlMapping(url = "/login", method = "POST")
    public ModelAndView login(MySession session, String nom, String role) {
        session.setAttribute("user", nom);
        session.setAttribute("role", role);
        return new ModelAndView("accueil");
    }

    @Auth
    @UrlMapping(url = "/profil")
    public ModelAndView profil() {
        return new ModelAndView("profil");
    }

    @Auth(role = "admin")
    @UrlMapping(url = "/admin")
    public ModelAndView admin() {
        return new ModelAndView("admin");
    }

    @UrlMapping(url = "/logout")
    public ModelAndView logout(MySession session) {
        session.invalidate();
        return new ModelAndView("login");
    }
}
```