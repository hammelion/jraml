package com.github.hammelion.resources;

import java.lang.annotation.Annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.github.hammelion.resources.model.TestId;

@Path("test")
public class RAMLFreeTestResource {

    @GET
    public Annotation[] getById(@PathParam("testId") TestId id) throws NoSuchMethodException {
        return methodAnnotations("getById", new Class[] { TestId.class });
    }

    public Annotation[] postById(TestId id) throws NoSuchMethodException {
        return methodAnnotations("getById", new Class[] { TestId.class });
    }

    private Annotation[] methodAnnotations(String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        return getClass().getMethod(methodName, parameterTypes).getAnnotations();
    }
}
