package mg.itu.framework.model;

import java.util.Map;

public class MySession {
    private Map<String, Object> sessionData;  
    
    public MySession(Map<String, Object> sessionData) {
        this.sessionData = sessionData;
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
