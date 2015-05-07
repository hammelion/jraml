package org.jraml.matchers.method;

import org.jraml.resources.RAMLFreeTestResource;
import org.jraml.resources.TestResource;
import org.jraml.resources.model.TestId;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raml.model.Action;
import org.raml.model.ActionType;

import javax.inject.Inject;

import static org.junit.Assert.*;

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
