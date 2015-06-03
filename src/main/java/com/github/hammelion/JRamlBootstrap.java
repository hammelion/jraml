package com.github.hammelion;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hammelion.annotations.RAMLConfig;
import com.github.hammelion.processors.ResourceProcessor;

@Named
public class JRamlBootstrap implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(JRamlBootstrap.class);

    private final ResourceProcessor resourceProcessor;

    @Inject
    public JRamlBootstrap(ResourceProcessor resourceProcessor) {
        this.resourceProcessor = resourceProcessor;
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Reflections reflections = new Reflections();
        final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RAMLConfig.class);
        for (Class<?> originalClass : classes) {
            applyConfig(originalClass);
        }
        LOG.info("JRaml was successfully initialized");
        // TODO Remove listener from context
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }

    public void applyConfig(Class<?> originalClass) {
        applyConfig(originalClass, ClassLoader.getSystemClassLoader());
    }

    public void applyConfig(Class<?> originalClass, ClassLoader classLoader) {
        try {
            this.resourceProcessor.process(originalClass, classLoader);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
