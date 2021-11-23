package se.eris.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Retention(RetentionPolicy.RUNTIME)
@ParameterizedTest(name = "Java version: {arguments}")
@ValueSource(strings = { "16", "17" })
public @interface TestJava16AndLater {

    class SupportedVersions {
        public static String[] getSupportedVersions() {
            return TestJava16AndLater.class.getAnnotationsByType(ValueSource.class)[0].strings();
        }
    }

}
