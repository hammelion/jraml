package org.jraml;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.ArrayUtils;
import org.jraml.classloaders.JRamlClassLoader;
import org.jraml.resources.model.TestId;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JukitoRunner.class)
public class JRamlBootstrapTest {
    private static final Logger LOG = LoggerFactory.getLogger(JRamlBootstrap.class);

    private Object testResource;

    @Inject
    private JRamlClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        this.testResource = classLoader.loadClass("org.jraml.resources.TestResource").newInstance();
    }

    @Test
    public void testContextInitialized() throws Exception {
        /*
         * final List<Annotation> annotations = (List<Annotation>) this.testResource.getClass().getMethod("postById",
         * TestId.class) .invoke(this.testResource, new TestId("id007"));
         */
        Object testSubResource = this.testResource.getClass().getMethod("testId", TestId.class)
                .invoke(this.testResource, new TestId("id007"));
        final List<Annotation> annotations = (List<Annotation>) testSubResource.getClass().getMethod("getById")
                .invoke(testSubResource);
        LOG.info(ArrayUtils.toString(annotations));
        // assertThat(annotations, IsIterableContainingInAnyOrder.<Annotation>containsInAnyOrder(typeCompatibleWith(GET.class))
    }
}
