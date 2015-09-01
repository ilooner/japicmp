package japicmp.cmp;

import japicmp.config.Options;
import japicmp.model.JApiClass;
import japicmp.output.stdout.StdoutOutputGenerator;
import japicmp.util.CtClassBuilder;
import japicmp.util.CtFieldBuilder;
import japicmp.util.CtMethodBuilder;
import javassist.ClassPool;
import javassist.CtClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static japicmp.util.Helper.getJApiClass;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class ShowSyntheticTest {

  @Test
	public void testNonSyntheticToSynthetic() throws Exception {
		JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
		options.setIncludeSynthetic(true);
		List<JApiClass> jApiClasses = ClassesHelper.compareClasses(options, new ClassesHelper.ClassesGenerator() {
			@Override
			public List<CtClass> createOldClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().addToClassPool(classPool);
				CtMethodBuilder.create().publicAccess().name("testMethod").addToClass(ctClass);
				CtFieldBuilder.create().name("testField").addToClass(ctClass);
				return Collections.singletonList(ctClass);
			}

			@Override
			public List<CtClass> createNewClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().addToClassPool(classPool);
				CtMethodBuilder.create().publicAccess().syntheticModifier().name("testMethod").addToClass(ctClass);
				CtFieldBuilder.create().syntheticModifier().name("testField").addToClass(ctClass);
				CtClass syntheticClass = new CtClassBuilder().syntheticModifier().name("japicmp.SyntheticClass").addToClassPool(classPool);
				return Arrays.asList(ctClass, syntheticClass);
			}
		});
		assertThat(jApiClasses.size(), is(2));
		JApiClass jApiClass = getJApiClass(jApiClasses, CtClassBuilder.DEFAULT_CLASS_NAME);
		assertThat(jApiClass.getMethods().size(), is(1));
		assertThat(jApiClass.getFields().size(), is(1));
		Options configOptions = new Options();
		configOptions.setIncludeSynthetic(false);
		StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(configOptions, jApiClasses, new File("v1.jar"), new File("v2.jar"));
		String output = stdoutOutputGenerator.generate();
    System.out.println(output);
	}

	@Test
	public void testShowSynthetic() throws Exception {
		JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
		options.setIncludeSynthetic(true);
		List<JApiClass> jApiClasses = ClassesHelper.compareClasses(options, new ClassesHelper.ClassesGenerator() {
			@Override
			public List<CtClass> createOldClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().addToClassPool(classPool);
				return Collections.singletonList(ctClass);
			}

			@Override
			public List<CtClass> createNewClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().addToClassPool(classPool);
				CtMethodBuilder.create().publicAccess().syntheticModifier().addToClass(ctClass);
				CtFieldBuilder.create().syntheticModifier().addToClass(ctClass);
				CtClass syntheticClass = new CtClassBuilder().syntheticModifier().name("japicmp.SyntheticClass").addToClassPool(classPool);
				return Arrays.asList(ctClass, syntheticClass);
			}
		});
		assertThat(jApiClasses.size(), is(2));
		JApiClass jApiClass = getJApiClass(jApiClasses, CtClassBuilder.DEFAULT_CLASS_NAME);
		assertThat(jApiClass.getMethods().size(), is(1));
		assertThat(jApiClass.getFields().size(), is(1));
		Options configOptions = new Options();
		configOptions.setIncludeSynthetic(true);
		StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(configOptions, jApiClasses, new File("v1.jar"), new File("v2.jar"));
		String output = stdoutOutputGenerator.generate();
		assertThat(output, containsString("+++  NEW CLASS: PUBLIC(+) SYNTHETIC(+) japicmp.SyntheticClass"));
		assertThat(output, containsString("+++  NEW FIELD: PUBLIC(+) SYNTHETIC(+) int field"));
		assertThat(output, containsString("+++  NEW METHOD: PUBLIC(+) SYNTHETIC(+) japicmp.Test method()"));
	}

	@Test
	public void testNotShowSynthetic() throws Exception {
		JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
		options.setIncludeSynthetic(false);
		List<JApiClass> jApiClasses = ClassesHelper.compareClasses(options, new ClassesHelper.ClassesGenerator() {
			@Override
			public List<CtClass> createOldClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().addToClassPool(classPool);
				return Collections.singletonList(ctClass);
			}

			@Override
			public List<CtClass> createNewClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().addToClassPool(classPool);
				CtMethodBuilder.create().syntheticModifier().addToClass(ctClass);
				CtFieldBuilder.create().syntheticModifier().addToClass(ctClass);
				CtClass syntheticClass = new CtClassBuilder().syntheticModifier().name("japicmp.SyntheticClass").addToClassPool(classPool);
				return Arrays.asList(ctClass, syntheticClass);
			}
		});
		assertThat(jApiClasses.size(), is(1));
		JApiClass jApiClass = getJApiClass(jApiClasses, CtClassBuilder.DEFAULT_CLASS_NAME);
		assertThat(jApiClass.getMethods().size(), is(0));
		assertThat(jApiClass.getFields().size(), is(0));
		Options configOptions = new Options();
		configOptions.setIncludeSynthetic(true);
		StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(configOptions, jApiClasses, new File("v1.jar"), new File("v2.jar"));
		String output = stdoutOutputGenerator.generate();
		assertThat(output, not(containsString("+++  NEW CLASS: PUBLIC(+) SYNTHETIC(+) japicmp.SyntheticClass")));
		assertThat(output, not(containsString("+++  NEW FIELD: PUBLIC(+) SYNTHETIC(+) int field")));
		assertThat(output, not(containsString("+++  NEW METHOD: PUBLIC(+) SYNTHETIC(+) japicmp.Test method()")));
	}
}
