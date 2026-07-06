package mg.itu.framework.util;

import java.util.Objects;

public class UrlMethod {
    private String url;
    private String method;

    public UrlMethod(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UrlMethod urlMethod = (UrlMethod) obj;

        return url.equals(urlMethod.url) && method.equals(urlMethod.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url , method);
    }
    
}
