package com.wonderwon.downloadery;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MainActivityTest {
    @Test public void acceptsOnlyHttpUrlsWithHosts() {
        assertTrue(MainActivity.isHttpUrl("https://example.com/file.zip"));
        assertTrue(MainActivity.isHttpUrl("http://example.com/path"));
        assertFalse(MainActivity.isHttpUrl("ftp://example.com/file"));
        assertFalse(MainActivity.isHttpUrl("example.com/file"));
        assertFalse(MainActivity.isHttpUrl("https:///missing-host"));
    }

    @Test public void sanitizesPathSeparatorsAndEmptyNames() {
        assertEquals("a__b.txt", MainActivity.safeName("a:/b.txt"));
        assertEquals("download", MainActivity.safeName(" \u0000 "));
        assertEquals(180, MainActivity.safeName("x".repeat(200)).length());
    }

    @Test public void derivesFilenameFromUrlPath() {
        assertEquals("photo.jpg", MainActivity.fileNameFromUrl("https://example.com/images/photo.jpg?x=1"));
        assertTrue(MainActivity.fileNameFromUrl("https://example.com/").startsWith("download-"));
    }
}
