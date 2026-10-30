package itu.webdynamique.framework;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class Session {
    private final HttpSession session;

    public Session(HttpServletRequest request) {
        this.session = request.getSession();
    }

    public Session(HttpSession session) {
        this.session = session;
    }

    public String getId() {
        return session.getId();
    }

    public void setAttribute(String name, Object value) {
        session.setAttribute(name, value);
    }

    public Object getAttribute(String name) {
        return session.getAttribute(name);
    }

    public void removeAttribute(String name) {
        session.removeAttribute(name);
    }

    public HttpSession getHttpSession() {
        return session;
    }
}
