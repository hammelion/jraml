package com.github.hammelion;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.ArrayUtils;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.hammelion.classloaders.JRamlClassLoader;
import com.github.hammelion.resources.model.TestId;

@RunWith(JukitoRunner.class)
public class JRamlBootstrapTest {
    private Object testResource;

    @Inject
    private JRamlClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        this.testResource = classLoader.loadClass("com.github.hammelion.resources.TestResource").newInstance();
        // TODO Clear classLoader, or load it only once.
    }

    @Test
    public void testSubResource() throws Exception {
        Object testSubResource = this.testResource.getClass().getMethod("testId", TestId.class)
                .invoke(this.testResource, new TestId("id007"));
        final List<Annotation> annotations = (List<Annotation>) testSubResource.getClass().getMethod("getById")
                .invoke(testSubResource);
        System.out.println(ArrayUtils.toString(annotations));
    }

//    @Test
    public void testResource() throws Exception {
        final List<Annotation> annotations = (List<Annotation>) this.testResource.getClass().getMethod("postById", TestId.class)
                .invoke(this.testResource, new TestId("id007"));
        System.out.println(ArrayUtils.toString(annotations));
        // assertThat(annotations, IsIterableContainingInAnyOrder.<Annotation>containsInAnyOrder(typeCompatibleWith(GET.class))
    }
}
