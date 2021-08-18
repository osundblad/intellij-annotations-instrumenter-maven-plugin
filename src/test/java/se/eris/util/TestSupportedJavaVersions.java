package se.eris.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ParameterizedTest(name = "Java target version: {arguments}")
@ValueSource(strings = {"1.7", "1.8", "1.9", "10", "11", "12", "13", "14", "15"})
public @interface TestSupportedJavaVersions {

    class SupportedVersions {
        public static String[] getSupportedVersions() {
            return TestSupportedJavaVersions.class.getAnnotationsByType(ValueSource.class)[0].strings();
        }
    }

}

