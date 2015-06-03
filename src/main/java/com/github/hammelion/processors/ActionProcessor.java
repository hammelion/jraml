package com.github.hammelion.processors;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Resource;
import org.raml.model.Response;
import org.raml.model.parameter.UriParameter;

import com.github.hammelion.exceptions.CorrespondingMethodNotFoundException;
import com.github.hammelion.matchers.method.MethodMatcher;
import com.github.hammelion.utils.javassist.Methods;
import com.google.common.collect.Sets;

/**
 * TODO JavaDoc in com.github.hammelion.processors
 */
@Named
public class ActionProcessor {
    private final ClassPool POOL = ClassPool.getDefault();

    private final MethodMatcher methodMatcher;

    @Inject
    public ActionProcessor(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public void process(Resource resource, ConstPool constPool, CtClass clazz, Class<?> originalClass) throws NotFoundException,
            CorrespondingMethodNotFoundException {
        Set<String> missingUriParameters = determineMissingUriParameters(originalClass, resource.getUriParameters());
        for (Action action : resource.getActions().values()) {
            processAction(clazz, constPool, originalClass, missingUriParameters, action);
        }
    }

    private Set<String> determineMissingUriParameters(Class<?> originalClass, Map<String, UriParameter> uriParameters) {
        Set<String> missingUriParameters = new HashSet<>();
        Set<String> fields = new HashSet<>();
        for (Field field : originalClass.getDeclaredFields()) {
            fields.add(Introspector.decapitalize(field.getType().getSimpleName()));
        }
        if (uriParameters != null) {
            Sets.difference(uriParameters.keySet(), fields).copyInto(missingUriParameters);
        }
        return missingUriParameters;
    }

    private void processAction(CtClass clazz, ConstPool constPool, Class<?> originalClass, Set<String> missingUriParameters,
            Action action) throws NotFoundException, CorrespondingMethodNotFoundException {
        for (Method originalMethod : originalClass.getMethods()) {
            if (this.methodMatcher.matches(originalMethod, missingUriParameters, action)) {
                final CtMethod method = clazz.getDeclaredMethod(originalMethod.getName());
                Methods.annotateMethod(constPool, method, originalMethod,
                        POOL.getCtClass("javax.ws.rs." + action.getType().name()));
                annotateMethodWithMimeTypes(constPool, method, originalMethod, action);
                annotateMethodParameters(constPool, method, originalMethod, action, missingUriParameters);
                return;
            }
        }
        throw new CorrespondingMethodNotFoundException(MessageFormat.format(
                "Method''s name should start with ''{0}'' and have following parameters:\n{1} {2}", action.getType().toString()
                        .toLowerCase(), action.getBaseUriParameters(), action.getQueryParameters()));
    }

    private void annotateMethodWithMimeTypes(ConstPool constPool, CtMethod method, Method originalMethod, Action action)
            throws NotFoundException {
        Methods.annotateMethod(constPool, method, originalMethod, POOL.getCtClass(Produces.class.getName()),
                constructProducesMembers(constPool, action));
        Methods.annotateMethod(constPool, method, originalMethod, POOL.getCtClass(Consumes.class.getName()),
                constructConsumesMembers(constPool, action));
    }

    private void annotateMethodParameters(ConstPool constPool, CtMethod method, Method originalMethod, Action action,
            Set<String> missingUriParameters) throws NotFoundException {
        if (action != null) {
            ParameterAnnotationsAttribute attribute = ((ParameterAnnotationsAttribute) method.getMethodInfo().getAttribute(
                    ParameterAnnotationsAttribute.visibleTag));
            if (attribute == null) {
                attribute = new ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag);
                method.getMethodInfo().addAttribute(attribute);
            }
            Annotation[][] allAnnotations = attribute.getAnnotations();
            if (allAnnotations.length == 0) {
                allAnnotations = new Annotation[originalMethod.getParameters().length][];
            }
            Methods.annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, action.getQueryParameters()
                    .keySet(), QueryParam.class, POOL.getCtClass(QueryParam.class.getName()));
            Methods.annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, missingUriParameters,
                    PathParam.class, POOL.getCtClass(PathParam.class.getName()));
            if (action.getBody() != null) {
                Methods.annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, action.getBody().keySet(),
                        FormParam.class, POOL.getCtClass(FormParam.class.getName()));
            }
            Methods.annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, action.getHeaders().keySet(),
                    HeaderParam.class, POOL.getCtClass(HeaderParam.class.getName()));
            attribute.setAnnotations(allAnnotations);
        }
    }

    private ArrayMemberValue constructProducesMembers(ConstPool constPool, Action action) {
        final ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
        final List<MemberValue> memberValues = new ArrayList<>();
        for (Response response : action.getResponses().values()) {
            fillMemberValuesWithMimeTypes(constPool, memberValues, response.getBody());
        }
        arrayMemberValue.setValue(memberValues.toArray(new MemberValue[memberValues.size()]));
        return arrayMemberValue;
    }

    private ArrayMemberValue constructConsumesMembers(ConstPool constPool, Action action) {
        final ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
        final List<MemberValue> memberValues = new ArrayList<>();
        fillMemberValuesWithMimeTypes(constPool, memberValues, action.getBody());
        arrayMemberValue.setValue(memberValues.toArray(new MemberValue[memberValues.size()]));
        return arrayMemberValue;
    }

    private void fillMemberValuesWithMimeTypes(ConstPool constPool, List<MemberValue> memberValues, Map<String, MimeType> body) {
        if (body != null) {
            for (MimeType mimeType : body.values()) {
                final StringMemberValue stringMemberValue = new StringMemberValue(mimeType.getType(), constPool);
                memberValues.add(stringMemberValue);
            }
        }
    }

}
