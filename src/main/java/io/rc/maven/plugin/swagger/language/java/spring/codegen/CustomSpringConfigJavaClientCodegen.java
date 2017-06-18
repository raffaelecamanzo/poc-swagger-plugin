package io.rc.maven.plugin.swagger.language.java.spring.codegen;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.rc.maven.plugin.swagger.language.java.spring.CustomSpringConfig;
import io.swagger.codegen.*;
import io.swagger.codegen.languages.JavaClientCodegen;
import io.swagger.models.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User:        raffaelecamanzo
 * Date:        15/11/2016
 *
 * Description:
 * The codegen
 *
 */
public class CustomSpringConfigJavaClientCodegen extends JavaClientCodegen implements CodegenConfig, CustomSpringConfig {

	public static final String CODEGEN_NAME = "custom-spring";
	private static final Logger LOG = LoggerFactory.getLogger(CustomSpringConfigJavaClientCodegen.class);

	@Override
	public CodegenType getTag() {
		return CodegenType.SERVER;
	}

	@Override
	public String getName() {
		return CODEGEN_NAME;
	}

	@Override
	public String getHelp() {
		return "Custom Spring Java Swagger Codegen";
	}

	private boolean excludeSupportingFiles = false;

	public void excludeSupportingFiles() {
		excludeSupportingFiles = true;
	}

	public CustomSpringConfigJavaClientCodegen() {
		super();
		embeddedTemplateDir = templateDir = "custom_spring_templates";
		modelTemplateFiles.put("model.mustache", ".java");
		apiTemplateFiles.put("api.mustache", ".java");

		// Removing unused templates
		modelDocTemplateFiles.remove("model_doc.mustache");
		apiTestTemplateFiles.remove("api_test.mustache");
		apiDocTemplateFiles.remove("api_doc.mustache");

		typeMapping.put("UUID", "UUID");
		library = "jersey2";
	}

	@Override
	public List<SupportingFile> supportingFiles() {
		supportingFiles.clear();
		if(!excludeSupportingFiles) {
			supportingFiles.add(new SupportingFile("ApiException.mustache", apiPackage.replace(".", "/"), "ApiException.java"));
			supportingFiles.add(new SupportingFile("ApiOriginFilter.mustache", apiPackage.replace(".", "/"), "ApiOriginFilter.java"));
		}
		return supportingFiles;
	}

	@Override
	public String apiFileFolder() {
		return outputFolder + "/" + apiPackage().replace('.', File.separatorChar);
	}

	@Override
	public String modelFileFolder() {
		return outputFolder + "/" + modelPackage().replace('.', File.separatorChar);
	}

	@Override
	public String modelPackage() {
		if (this.modelPackage == null || this.modelPackage.trim().isEmpty()) {
			throw new RuntimeException("'modelPackage' should not be null or empty");
		}
		return this.modelPackage;
	}

	@Override
	public String apiPackage() {
		if (this.apiPackage == null || this.apiPackage.trim().isEmpty()) {
			throw new RuntimeException("'apiPackage' should not be null or empty");
		}
		return this.apiPackage;
	}

	@Override
	public void addOperationToGroup(final String tag,
	                                final String resourcePath,
	                                final Operation operation,
	                                final CodegenOperation co,
	                                final Map<String, List<CodegenOperation>> operations) {

		LOG.debug("Codegen Operation. path: {} - subresource OP: {}", co.path, co.subresourceOperation!=null ? (co.subresourceOperation ? "YES" : "NO") : "NO");
		LOG.debug("Codegen Operation. baseName: {}", co.baseName);
		String basePath = resourcePath;
		if (basePath.startsWith("/")) {
			basePath = basePath.substring(1);
		}

		LOG.debug("base path after first manip: {}", basePath);
		int pos = basePath.indexOf("/");
		if (pos > 0) {
			basePath = basePath.substring(0, pos);
		}

		LOG.debug("base path after second manip: {}", basePath);

		if(Strings.isNullOrEmpty(basePath)) {
			basePath = "default";
		} else {
			if(co.path.startsWith("/" + basePath)) {
				co.path = co.path.substring(("/" + basePath).length());
			}

			co.subresourceOperation = !co.path.isEmpty();
		}

		LOG.debug("base path after third manip: {}", basePath);
		LOG.debug("Codegen Operation. path: {} - subresource OP: {}", co.path, co.subresourceOperation ? "YES" : "NO");
		LOG.debug("Codegen Operation. baseName: {}", co.baseName);

		List<CodegenOperation> opList;
		if((opList=operations.get(basePath)) == null) {
			opList = new ArrayList<>();
			operations.put(basePath, opList);
		}

		opList.add(co);
		co.baseName = basePath;
	}

	private final List<String> methodsWithoutRequestBody = Lists.newArrayList("GET");

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
		super.postProcessOperations(objs);

		Map<String, Object> operations = (Map<String, Object>)objs.get("operations");
		if(operations != null) {
			List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
			for (CodegenOperation operation : ops) {
				if(methodsWithoutRequestBody.contains(operation.httpMethod)) {
					operation.vendorExtensions.put("consumesExpected", false);
				}else{
					operation.vendorExtensions.put("consumesExpected", true);
				}
				LOG.info("Operation: {} - base name: {}", operation.path, operation.baseName);
				boolean multipleResponseTypes = false;
				List<CodegenResponse> responses = operation.responses;
				if(responses!=null && !responses.isEmpty()) {
					String respDataType = responses.get(0).dataType;
					for(CodegenResponse resp : responses) {
						LOG.info("Response data type: {} - code: {}", resp.dataType, resp.code);
						multipleResponseTypes = multipleResponseTypes ||
								!((Strings.isNullOrEmpty(respDataType) && Strings.isNullOrEmpty(resp.dataType)) ||
										(respDataType!=null && respDataType.equalsIgnoreCase(resp.dataType)));

						if ("0".equals(resp.code)) {
							LOG.info("Default response code. Assuming generic error ->> HTTP 500");
							resp.code = "500";
						}
					}
				}
				LOG.info("Return type: {} - return container: {} - operation id: {}", operation.returnType, operation.returnContainer, operation.operationId);
				LOG.info("Response type defaults to ResponseEntity!");
				operation.returnType = "ResponseEntity";
				if(multipleResponseTypes) {
					if(operation.returnContainer!=null) {
						operation.returnContainer = null;
						operation.isListContainer = false;
						operation.isMapContainer = false;
					}
				}
			}
		}
		return objs;
	}

	public void setApiPackage(final String apiPackage) {
		this.apiPackage = apiPackage;
	}

	public void setModelPackage(final String modelPackage) {
		this.modelPackage = modelPackage;
	}

	public boolean isBuilderSupported() { return true; }

	public void enableBuilderSupport() {
		modelTemplateFiles.remove("model.mustache");
		modelTemplateFiles.put("modelBuilder.mustache", ".java");
	}

	@Override
	public String toApiName(String name) {
		if(name.length() == 0) {
			return "DefaultRestController";
		}
		name = sanitizeName(name);
		return camelize(name) + "RestController";
	}
}
