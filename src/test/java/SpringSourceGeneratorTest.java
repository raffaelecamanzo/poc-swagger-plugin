import com.google.common.collect.Lists;
import io.rc.maven.plugin.swagger.language.java.spring.SpringSourceGenerator;
import io.rc.maven.plugin.swagger.language.java.spring.codegen.CustomSpringConfigJavaClientCodegen;
import io.rc.maven.plugin.swagger.mojo.CodegenInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * User:        raffaelecamanzo
 * Description: Source generation test class
 *
 */
public class SpringSourceGeneratorTest {

	@Test
	public void testSourceGeneration() throws Exception {
		File file = new File(getClass().getResource("/test-resource.yaml").getFile());
		List<CodegenInfo> codegenInfos = Lists.newArrayList(new CodegenInfo()
				.withFileName(file.getPath())
				.withApiPackage("io.rc.maven.plugin.swagger.api")
				.withModelPackage("io.rc.maven.plugin.swagger.model")
				.skipApi(false));

		SpringSourceGenerator generator = SpringSourceGenerator.builder()
				.withCodegenInfos(codegenInfos)
				.forLanguage(CustomSpringConfigJavaClientCodegen.CODEGEN_NAME)
				.enableBuilderSupport(true)
				.writeStubTo(generateOutputDir()).build();

		generator.generate();
	}

	protected File generateOutputDir() {
		File userDir = new File(System.getProperty("user.dir"));
		File outputDirectory = new File(userDir, "/target/" + UUID.randomUUID().toString());
		if (!outputDirectory.mkdirs()) {
			System.out.println("NOT_CREATED at " + outputDirectory.getAbsolutePath());
		}

		System.out.println("OUTPUT TO : " + outputDirectory.getAbsolutePath());
		return outputDirectory;
	}
}
