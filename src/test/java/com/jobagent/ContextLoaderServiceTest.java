package com.jobagent;

import com.jobagent.config.AgentProperties;
import com.jobagent.model.UserContext;
import com.jobagent.service.ContextLoaderService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextLoaderServiceTest {

    @Test
    void testLoadContext() {
        AgentProperties properties = new AgentProperties();
        ContextLoaderService service = new ContextLoaderService(properties);

        UserContext ctx = service.loadContext();

        assertNotNull(ctx);
        assertNotNull(ctx.getFullName());
        assertTrue(ctx.getFullName().contains("Kashyap"), "Name should match profile.md or default");
        assertNotNull(ctx.getTargetRoles());
        assertFalse(ctx.getTargetRoles().isEmpty(), "Target roles should be populated from preferences.md");
        assertTrue(ctx.getTargetRoles().contains("Java Developer") || ctx.getTargetRoles().contains("Spring Boot Developer"));
    }
}
