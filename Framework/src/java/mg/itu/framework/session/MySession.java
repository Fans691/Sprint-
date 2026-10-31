package mg.itu.framework.session;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

public class MySession {
    private final HttpSession httpSession;

    public MySession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    public Object getAttribute(String key) {
        return httpSession.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        httpSession.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        httpSession.removeAttribute(key);
    }

    public void invalidate() {
        httpSession.invalidate();
    }

    public Map<String, Object> getSessionData() {
        Map<String, Object> sessionData = new HashMap<>();
        Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            sessionData.put(name, httpSession.getAttribute(name));
        }
        return Collections.unmodifiableMap(sessionData);
    }
}
