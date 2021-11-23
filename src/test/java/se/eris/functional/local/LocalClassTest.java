package se.eris.functional.local;

import org.junit.jupiter.api.BeforeAll;
import se.eris.util.ReflectionUtil;
import se.eris.util.TestClass;
import se.eris.util.TestCompiler;
import se.eris.util.TestSupportedJavaVersions;
import se.eris.util.version.VersionCompiler;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalClassTest {

	private static final File SRC_DIR = new File("src/test/data");
	private static final Path DESTINATION_BASEDIR = new File("target/test/data/classes").toPath();

	private static final Map<String, TestCompiler> compilers = new HashMap<>();
	private static final TestClass outerClass = new TestClass("se.eris.local.TestLocal");
	private static final TestClass localClass = new TestClass("se.eris.local.TestLocal$1LocalClass");

	private static final String METHOD_NAME = "localMethod";

	@BeforeAll
	static void beforeClass() {
		compilers.putAll(VersionCompiler.withSupportedVersions().compile(DESTINATION_BASEDIR, outerClass.getJavaFile(SRC_DIR)));
	}

	@TestSupportedJavaVersions
	void localClassConstructorShouldValidate(final String javaVersion) throws Exception {
		final Class<?> outer = compilers.get(javaVersion).getCompiledClass(outerClass);
		final Constructor<?> outerConstructor = outer.getConstructor();
		final Object outerInstance = ReflectionUtil.simulateConstructorCall(outerConstructor);

		final Class<?> local = compilers.get(javaVersion).getCompiledClass(localClass);
		final Constructor<?> localConstructor = local.getConstructor(outer, String.class, String.class);

		localConstructor.setAccessible(true);

		//first parameter for inner class is auto generated and
		ReflectionUtil.simulateConstructorCall(localConstructor, outerInstance, "A String", "Another String");
		ReflectionUtil.simulateConstructorCall(localConstructor, null, "A String", "Another String");
		ReflectionUtil.simulateConstructorCall(localConstructor, outerInstance, null, "Another String");
		ReflectionUtil.simulateConstructorCall(localConstructor, null, null, "Another String");

		final IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateConstructorCall(localConstructor, outerInstance, null, null));
		assertEquals(String.format("NotNull annotated argument 1%s of %s.<init> must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "notNull"), localClass.getAsmName()), exception1.getMessage());
	}

	@TestSupportedJavaVersions
	void localClassMethodShouldValidate(final String javaVersion) throws Exception {
		final Class<?> outer = compilers.get(javaVersion).getCompiledClass(outerClass);
		final Constructor<?> outerConstructor = outer.getConstructor();
		final Object outerInstance = ReflectionUtil.simulateConstructorCall(outerConstructor);

		final Class<?> local = compilers.get(javaVersion).getCompiledClass(localClass);
		final Constructor<?> localConstructor = local.getConstructor(outer, String.class, String.class);
		localConstructor.setAccessible(true);

		final Method localMethod = local.getMethod(METHOD_NAME, String.class, String.class);
		localMethod.setAccessible(true);

		final Object innerInstance = ReflectionUtil.simulateConstructorCall(localConstructor, outerInstance, "A String", "Another String");

		ReflectionUtil.simulateMethodCall(innerInstance, localMethod, "A String", "Another String");
		ReflectionUtil.simulateMethodCall(innerInstance, localMethod, null, "Another String");

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.simulateMethodCall(innerInstance, localMethod, "A String", null));
		assertEquals(String.format("NotNull annotated argument 1%s of %s.%s must not be null", VersionCompiler.maybeName(compilers.get(javaVersion), "notNull"), localClass.getAsmName(), METHOD_NAME), exception.getMessage());
	}

}
