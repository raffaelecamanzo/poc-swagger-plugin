package io.rc.maven.plugin.swagger.mojo;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * User:        raffaelecamanzo
 * Date:        23/11/2016
 *
 * Description:
 * Codegen info definition class (data structure to handle multiple YAML file definitions handled by the Maven plugin)
 *
 */
public class CodegenInfo {

	@Parameter(required = true)
	private String fileName;

	@Parameter
	private String modelPackage;

	@Parameter
	private String apiPackage;

	@Parameter(defaultValue = "false")
	private boolean skipModel;

	@Parameter(defaultValue = "false")
	private boolean skipApi;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getModelPackage() {
		return modelPackage;
	}

	public void setModelPackage(String modelPackage) {
		this.modelPackage = modelPackage;
	}

	public String getApiPackage() {
		return apiPackage;
	}

	public void setApiPackage(String apiPackage) {
		this.apiPackage = apiPackage;
	}

	public boolean isSkipModel() {
		return skipModel;
	}

	public void setSkipModel(boolean skipModel) {
		this.skipModel = skipModel;
	}

	public boolean isSkipApi() {
		return skipApi;
	}

	public void setSkipApi(boolean skipApi) {
		this.skipApi = skipApi;
	}

	public CodegenInfo withFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public CodegenInfo withApiPackage(String apiPackage) {
		this.apiPackage = apiPackage;
		return this;
	}

	public CodegenInfo withModelPackage(String modelPackage) {
		this.modelPackage = modelPackage;
		return this;
	}

	public CodegenInfo skipApi(boolean skip) {
		this.skipApi = skip;
		return this;
	}

	public CodegenInfo skipModel(boolean skip) {
		this.skipModel = skip;
		return this;
	}
}
