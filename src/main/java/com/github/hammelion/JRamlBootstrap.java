package com.github.hammelion;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class JRamlBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("JRaml is starting...");
        final Injector injector = Guice.createInjector(new JRamlModule());
        final JRamlClassPatcher classPatcher = injector.getInstance(JRamlClassPatcher.class);
        classPatcher.applyConfig();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
