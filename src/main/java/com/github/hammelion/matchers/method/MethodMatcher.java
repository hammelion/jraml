package com.github.hammelion.matchers.method;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.raml.model.Action;
import org.raml.model.Resource;

/**
 * TODO JavaDoc in org.jraml.matchers
 */
@Named
public class MethodMatcher {
    private final MethodNameRule methodNameRule;

    private final MethodParametersRule methodParametersRule;

    @Inject
    public MethodMatcher(MethodNameRule methodNameRule, MethodParametersRule methodParametersRule) {
        this.methodNameRule = methodNameRule;
        this.methodParametersRule = methodParametersRule;
    }

    public boolean matches(Method method, Set<String> missingUriParameters, Action action) {
        if (!this.methodParametersRule.matches(method, extractRequiredParameters(missingUriParameters, action))) {
            return false;
        }
        return this.methodNameRule.matches(method, action.getType().name().toLowerCase());
    }

    public boolean matches(Method method, Resource resource) {
        return this.methodNameRule.matches(method, resource.getRelativeUri().replaceAll("[\\{\\}/]", ""));
    }

    private Set<String> extractRequiredParameters(Set<String> missingUriParameters, Action action) {
        final Set<String> parameters = new HashSet<>();
        parameters.addAll(action.getQueryParameters().keySet());
        parameters.addAll(missingUriParameters);
        /*if (action.getBody() != null) {
            parameters.addAll(action.getBody().keySet());
        }*/
        parameters.addAll(action.getHeaders().keySet());
        return parameters;
    }
}
