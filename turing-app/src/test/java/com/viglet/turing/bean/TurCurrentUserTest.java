package com.viglet.turing.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurCurrentUserTest {

    @Test
    void testGettersAndSetters() {
        TurCurrentUser user = new TurCurrentUser();
        
        user.setUsername("testuser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAdmin(true);
        
        assertEquals("testuser", user.getUsername());
        assertEquals("Test", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertTrue(user.isAdmin());
    }

    @Test
    void testDefaultValues() {
        TurCurrentUser user = new TurCurrentUser();
        
        assertNull(user.getUsername());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertFalse(user.isAdmin());
    }

    @Test
    void testAdminFlag() {
        TurCurrentUser user = new TurCurrentUser();
        
        assertFalse(user.isAdmin());
        
        user.setAdmin(true);
        assertTrue(user.isAdmin());
        
        user.setAdmin(false);
        assertFalse(user.isAdmin());
    }
}
