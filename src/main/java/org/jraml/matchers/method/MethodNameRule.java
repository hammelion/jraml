package org.jraml.matchers.method;

import java.lang.reflect.Method;

import javax.inject.Named;
import javax.ws.rs.Path;

/**
 * TODO JavaDoc in org.jraml.matchers.method
 */
@Named
class MethodNameRule {

    public boolean matches(Method method, String name) {
        return hasMatchingPathAnnotation(method, name) || nameStartsWithAction(method, name);
    }

    private boolean hasMatchingPathAnnotation(Method method, String name) {
        final Path path = method.getAnnotation(Path.class);
        if (path != null) {
            return path.value().equalsIgnoreCase(name);
        }
        return false;
    }

    private boolean nameStartsWithAction(Method method, String name) {
        return method.getName().startsWith(name);
    }
}
