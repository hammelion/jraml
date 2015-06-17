package com.github.hammelion;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.reflections.Reflections;

import com.github.hammelion.annotations.RAMLConfig;
import com.github.hammelion.processors.ResourceProcessor;

@Named
public class JRamlClassPatcher {
    private final ResourceProcessor resourceProcessor;

    @Inject
    public JRamlClassPatcher(ResourceProcessor resourceProcessor) {
        this.resourceProcessor = resourceProcessor;
    }

    public void applyConfig() {
        Reflections reflections = new Reflections();
        final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(RAMLConfig.class);
        for (Class<?> originalClass : classes) {
            applyConfig(originalClass);
        }
        // TODO Remove listener from context
    }

    public void applyConfig(Class<?> originalClass) {
        applyConfig(originalClass, ClassLoader.getSystemClassLoader());
    }

    public void applyConfig(Class<?> originalClass, ClassLoader classLoader) {
        try {
            this.resourceProcessor.process(originalClass, classLoader);
            System.out.println(originalClass.getSimpleName() + " was successfully initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
