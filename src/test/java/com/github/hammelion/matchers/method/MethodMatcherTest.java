package com.github.hammelion.matchers.method;

import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raml.model.Action;
import org.raml.model.ActionType;

import javax.inject.Inject;

@RunWith(JukitoRunner.class)
public class MethodMatcherTest {

    @Inject private MethodMatcher methodMatcher;

    @Test public void testMatches() throws Exception {
        final Action action = new Action();
        action.setType(ActionType.GET);
        action.getBaseUriParameters().put("testId", null);
//        this.methodMatcher.matches(RAMLFreeTestResource.class.getMethod("getById", TestId.class), action);
    }
}
