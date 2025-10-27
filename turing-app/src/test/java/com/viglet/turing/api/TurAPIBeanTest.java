package com.viglet.turing.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurAPIBeanTest {

    @Test
    void testGettersAndSetters() {
        TurAPIBean bean = new TurAPIBean();
        
        bean.setProduct("Turing ES");
        bean.setMultiTenant(true);
        bean.setKeycloak(false);
        
        assertEquals("Turing ES", bean.getProduct());
        assertTrue(bean.isMultiTenant());
        assertFalse(bean.isKeycloak());
    }

    @Test
    void testDefaultValues() {
        TurAPIBean bean = new TurAPIBean();
        
        assertNull(bean.getProduct());
        assertFalse(bean.isMultiTenant());
        assertFalse(bean.isKeycloak());
    }

    @Test
    void testMultiTenantFlag() {
        TurAPIBean bean = new TurAPIBean();
        
        assertFalse(bean.isMultiTenant());
        
        bean.setMultiTenant(true);
        assertTrue(bean.isMultiTenant());
        
        bean.setMultiTenant(false);
        assertFalse(bean.isMultiTenant());
    }

    @Test
    void testKeycloakFlag() {
        TurAPIBean bean = new TurAPIBean();
        
        assertFalse(bean.isKeycloak());
        
        bean.setKeycloak(true);
        assertTrue(bean.isKeycloak());
        
        bean.setKeycloak(false);
        assertFalse(bean.isKeycloak());
    }
}
