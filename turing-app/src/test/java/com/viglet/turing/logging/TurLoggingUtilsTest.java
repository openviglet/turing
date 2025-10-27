package com.viglet.turing.logging;

import com.viglet.turing.client.sn.job.TurSNJobAction;
import com.viglet.turing.client.sn.job.TurSNJobItem;
import com.viglet.turing.commons.indexing.TurIndexingStatus;
import com.viglet.turing.commons.indexing.TurLoggingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TurLoggingUtilsTest {

    private TurSNJobItem turSNJobItem;

    @BeforeEach
    void setUp() {
        List<String> siteNames = new ArrayList<>();
        siteNames.add("test-site");
        
        turSNJobItem = new TurSNJobItem(TurSNJobAction.CREATE, siteNames, Locale.US);
        turSNJobItem.setEnvironment("test-env");
        turSNJobItem.setChecksum("checksum123");
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(TurLoggingUtils.URL, "http://test.com/page1");
        turSNJobItem.setAttributes(attributes);
    }

    @Test
    void testSetSuccessStatus() {
        assertDoesNotThrow(() -> {
            TurLoggingUtils.setSuccessStatus(turSNJobItem, TurIndexingStatus.INDEXED);
        });
    }

    @Test
    void testSetErrorStatus() {
        assertDoesNotThrow(() -> {
            TurLoggingUtils.setErrorStatus(turSNJobItem, TurIndexingStatus.IGNORED, "Error details");
        });
    }

    @Test
    void testSetLoggingStatus() {
        assertDoesNotThrow(() -> {
            TurLoggingUtils.setLoggingStatus(turSNJobItem, TurIndexingStatus.INDEXED, 
                TurLoggingStatus.SUCCESS, "Success details");
        });
    }

    @Test
    void testSetLoggingStatusWithNullDetails() {
        assertDoesNotThrow(() -> {
            TurLoggingUtils.setLoggingStatus(turSNJobItem, TurIndexingStatus.INDEXED, 
                TurLoggingStatus.SUCCESS, null);
        });
    }

    @Test
    void testConstants() {
        assertEquals("url", TurLoggingUtils.URL);
        assertEquals("Server", TurLoggingUtils.SERVER);
    }
}
