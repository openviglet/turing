package com.viglet.turing.exchange;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.viglet.turing.exchange.sn.TurSNSiteImport;

class TurImportExchangeTest {

    @Test
    void shouldReturnEmptyExchangeWhenFileDoesNotExist() {
        TurSNSiteImport turSNSiteImport = org.mockito.Mockito.mock(TurSNSiteImport.class);
        TurImportExchange turImportExchange = new TurImportExchange(turSNSiteImport);

        TurExchange result = turImportExchange.importFromFile(new File("D:/this/path/does/not/exist.zip"));

        assertNotNull(result);
        verifyNoInteractions(turSNSiteImport);
    }

    @Test
    void shouldReturnEmptyExchangeWhenZipExtractionFails() {
        TurSNSiteImport turSNSiteImport = org.mockito.Mockito.mock(TurSNSiteImport.class);
        TurImportExchange turImportExchange = org.mockito.Mockito.spy(new TurImportExchange(turSNSiteImport));
        MultipartFile multipartFile = new MockMultipartFile("file", "any.zip", "application/zip", new byte[] { 1, 2 });

        org.mockito.Mockito.doReturn(null).when(turImportExchange).extractZipFile(multipartFile);

        TurExchange result = turImportExchange.importFromMultipartFile(multipartFile);

        assertNotNull(result);
        verifyNoInteractions(turSNSiteImport);
    }
}
