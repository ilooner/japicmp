package japicmp.cmp;

import japicmp.config.Options;
import japicmp.model.JApiChangeStatus;
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

public class IgnoreModifiersTest {
  @Test
	public void testNonSyntheticToSyntheticMethod() throws Exception {
    ignoreTestHelper(false, true, true);
	}

  @Test
	public void testNonSyntheticToSyntheticMethodIgnore() throws Exception {
    ignoreTestHelper(true, true, true);
	}

  @Test
	public void testNonSyntheticToSyntheticField() throws Exception {
    ignoreTestHelper(false, false, true);
	}

  @Test
	public void testNonSyntheticToSyntheticFieldIgnore() throws Exception {
    ignoreTestHelper(true, false, true);
	}

  @Test
	public void testNonBridgeToBridgeMethod() throws Exception {
    ignoreTestHelper(false, true, true);
	}

  @Test
	public void testNonBridgeToBridgeMethodIgnore() throws Exception {
    ignoreTestHelper(true, true, true);
	}

	private void ignoreTestHelper(boolean ignore, final boolean methodTrueFieldFalse, final boolean syntheticTrueBridgeFalse) throws Exception
  {
		JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
		options.setIncludeSynthetic(true);
    options.setIgnoreSynthetic(ignore);
		List<JApiClass> jApiClasses = ClassesHelper.compareClasses(options, new ClassesHelper.ClassesGenerator() {
			@Override
			public List<CtClass> createOldClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().name(CtClassBuilder.DEFAULT_CLASS_NAME).addToClassPool(classPool);
        if (methodTrueFieldFalse) {
          CtMethodBuilder.create().publicAccess().name("testMethod").addToClass(ctClass);
        } else {
          CtFieldBuilder.create().name("testField").addToClass(ctClass);
        }

				return Collections.singletonList(ctClass);
			}

			@Override
			public List<CtClass> createNewClasses(ClassPool classPool) throws Exception {
				CtClass ctClass = new CtClassBuilder().name(CtClassBuilder.DEFAULT_CLASS_NAME).addToClassPool(classPool);

        if (syntheticTrueBridgeFalse) {
          if (methodTrueFieldFalse) {
            CtMethodBuilder.create().publicAccess().name("testMethod").syntheticModifier().addToClass(ctClass);
          } else {
            CtFieldBuilder.create().name("testField").syntheticModifier().addToClass(ctClass);
          }
        } else {
          if (methodTrueFieldFalse) {
            CtMethodBuilder.create().publicAccess().name("testMethod").bridgeModifier().addToClass(ctClass);
          } else {
            CtFieldBuilder.create().name("testField").bridgeModifier().addToClass(ctClass);
          }
        }

				return Collections.singletonList(ctClass);
			}
		});
		JApiClass jApiClass = getJApiClass(jApiClasses, CtClassBuilder.DEFAULT_CLASS_NAME);

    if (methodTrueFieldFalse) {
      assertThat(jApiClass.getMethods().size(), is(1));
    } else {
      assertThat(jApiClass.getFields().size(), is(1));
    }

		Options configOptions = new Options();
		configOptions.setIncludeSynthetic(true);
    configOptions.setIgnoreSynthetic(ignore);
		StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(configOptions, jApiClasses, new File("v1.jar"), new File("v2.jar"));
		String output = stdoutOutputGenerator.generate();
    System.out.println(output);

    JApiChangeStatus status;

    if(ignore) {
      status = JApiChangeStatus.UNCHANGED;
    } else {
      status = JApiChangeStatus.MODIFIED;
    }

    if(methodTrueFieldFalse) {
      assertThat(jApiClass.getMethods().get(0).getChangeStatus(), is(status));
    } else {
      assertThat(jApiClass.getFields().get(0).getChangeStatus(), is(status));
    }
	}
}
