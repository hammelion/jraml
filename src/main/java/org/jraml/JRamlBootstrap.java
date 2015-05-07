package org.jraml;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jraml.parsers.RAMLParserFacade;
import org.jraml.processors.ResourceProcessor;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class JRamlBootstrap implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(JRamlBootstrap.class);

    private final RAMLParserFacade ramlParserFacade;

    private final ResourceProcessor resourceProcessor;

    @Inject
    public JRamlBootstrap(RAMLParserFacade ramlParserFacade, ResourceProcessor resourceProcessor) {
        this.ramlParserFacade = ramlParserFacade;
        this.resourceProcessor = resourceProcessor;
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Reflections reflections = new Reflections();
        final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RAMLConfig.class);
        for (Class<?> originalClass : classes) {
            applyConfig(originalClass);
        }
        System.out.println("JRaml was successfully initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

    public void applyConfig(Class<?> originalClass) {
        applyConfig(originalClass, ClassLoader.getSystemClassLoader());
    }

    public void applyConfig(Class<?> originalClass, ClassLoader classLoader) {
        try {
            this.resourceProcessor.process(ramlParserFacade, originalClass, classLoader);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
