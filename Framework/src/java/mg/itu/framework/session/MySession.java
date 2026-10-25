package mg.itu.framework.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MySession {
    private final Map<String, Object> sessionData;

    public MySession() {
        this.sessionData = Collections.synchronizedMap(new HashMap<>());
    }

    public Object getAttribute(String key) {
        return sessionData.get(key);
    }

    public void setAttribute(String key, Object value) {
        sessionData.put(key, value);
    }

    public void removeAttribute(String key) {
        sessionData.remove(key);
    }

    public Map<String, Object> getSessionData() {
        return sessionData;
    }
}