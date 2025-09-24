package se.eris.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCompilerOptionsTest {

    private static final Path PATH = new File("a").toPath();

    @Test
    void hasParametersSupport() {
        assertFalse(TestCompilerOptions.from(PATH, "1.2").targetHasParametersSupport());
        assertFalse(TestCompilerOptions.from(PATH, "1.7").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "1.8").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "1.9").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "10").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "11").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "12").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "13").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "14").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "15").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "16").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "17").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "18").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "19").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "20").targetHasParametersSupport());
        assertTrue(TestCompilerOptions.from(PATH, "21").targetHasParametersSupport());
    }

    @Test
    void javaVersion_unsupported() {
        assertThrows(IllegalArgumentException.class, () -> TestCompilerOptions.from(PATH, "1.1").targetHasParametersSupport());
        assertThrows(IllegalArgumentException.class, () -> TestCompilerOptions.from(PATH, "22").targetHasParametersSupport());
    }

}