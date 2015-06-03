package com.github.hammelion.classloaders;

import java.net.URLClassLoader;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.hammelion.JRamlBootstrap;
import com.github.hammelion.annotations.RAMLConfig;

/**
 * TODO JavaDoc in org.jraml.classloaders
 */
@Named
public class JRamlClassLoader extends URLClassLoader {
    private final JRamlBootstrap jRamlBootstrap;

    @Inject
    public JRamlClassLoader(JRamlBootstrap jRamlBootstrap) {
        super(((URLClassLoader) getSystemClassLoader()).getURLs());
        this.jRamlBootstrap = jRamlBootstrap;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        final Class<?> clazz = Class.forName(name, false, getParent());
        if (clazz != null) {
            final RAMLConfig annotation = clazz.getAnnotation(RAMLConfig.class);
            if (annotation != null) {
                this.jRamlBootstrap.applyConfig(clazz, this);
                return Class.forName(name, true, this);
            }
        }
        return super.loadClass(name);
    }
}
