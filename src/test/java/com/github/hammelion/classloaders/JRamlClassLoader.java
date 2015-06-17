package com.github.hammelion.classloaders;

import java.net.URLClassLoader;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.hammelion.JRamlClassPatcher;
import com.github.hammelion.annotations.RAMLConfig;

/**
 * TODO JavaDoc in org.jraml.classloaders
 */
@Named
public class JRamlClassLoader extends URLClassLoader {
    private final JRamlClassPatcher classPatcher;

    @Inject
    public JRamlClassLoader(JRamlClassPatcher classPatcher) {
        super(((URLClassLoader) getSystemClassLoader()).getURLs());
        this.classPatcher = classPatcher;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        final Class<?> clazz = Class.forName(name, false, getParent());
        if (clazz != null) {
            final RAMLConfig annotation = clazz.getAnnotation(RAMLConfig.class);
            if (annotation != null) {
                this.classPatcher.applyConfig(clazz, this);
                return Class.forName(name, true, this);
            }
        }
        return super.loadClass(name);
    }
}
