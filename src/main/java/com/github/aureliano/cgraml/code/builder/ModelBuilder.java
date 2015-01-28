package com.github.aureliano.cgraml.code.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.github.aureliano.cgraml.code.meta.ClassMeta;
import com.github.aureliano.cgraml.code.meta.FieldMeta;
import com.github.aureliano.cgraml.code.meta.MethodMeta;
import com.github.aureliano.cgraml.code.meta.Visibility;
import com.github.aureliano.cgraml.helper.CodeBuilderHelper;
import com.github.aureliano.cgraml.helper.GeneratorHelper;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;

public class ModelBuilder implements IBuilder {

	private ClassMeta clazz;
	private static final Set<String> GENERATED_CLASSES = new HashSet<String>();
	
	protected ModelBuilder() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ModelBuilder parse(String pkg, String entity, Object resource) {
		Map<?, ?> map = this.parseJsonString(resource.toString());
		Map<String, Map<String, String>> properties = (Map<String, Map<String, String>>) map.get("properties");
		
		String javaDoc = new StringBuilder()
			.append("Generated by srvraml-maven-plugin.")
			.append("\n\n")
			.append(map.get("description"))
			.toString();
		
		this.clazz = new ClassMeta()
			.withPackageName(pkg + ".model")
			.withJavaDoc(javaDoc)
			.withClassName(StringUtils.capitalize(entity));
		
		if (GENERATED_CLASSES.contains(this.clazz.getCanonicalClassName())) {
			throw new IllegalArgumentException("Class " + this.clazz.getCanonicalClassName() + " was already generated before. Skipping!");
		}
		
		for (String fieldName : properties.keySet()) {
			Map<String, String> property = properties.get(fieldName);
			property.put("name", fieldName);
			FieldMeta attribute = FieldMeta.parse(property);
			
			this.clazz
				.addField(attribute)
				.addMethod(CodeBuilderHelper.createGetterMethod(attribute))
				.addMethod(CodeBuilderHelper.createSetterMethod(attribute))
				.addMethod(CodeBuilderHelper.createBuilderMethod(this.clazz.getClassName(), attribute));
		}
		
		this.addLinkedDataMethods(map.get("$linkedData"));

		GENERATED_CLASSES.add(this.clazz.getCanonicalClassName());
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ModelBuilder build() {
		this.buildJavaClass();
		return this;
	}
	
	private void buildJavaClass() {
		try {
			JCodeModel codeModel = new JCodeModel();
			JDefinedClass definedClass = codeModel._class(this.clazz.getCanonicalClassName());
			definedClass.javadoc().append(this.clazz.getJavaDoc());
			
			this.appendClassAttributes(codeModel, definedClass);
			this.appendClassMethods(codeModel, definedClass);
			
			codeModel.build(new File("src/main/java"));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private void appendClassMethods(JCodeModel codeModel, JDefinedClass definedClass) {
		for (MethodMeta method : this.clazz.getMethods()) {
			CodeBuilderHelper.addMethodToClass(codeModel, definedClass, method);
		}
	}

	private void appendClassAttributes(JCodeModel codeModel, JDefinedClass definedClass) {
		for (FieldMeta field : this.clazz.getFields()) {
			CodeBuilderHelper.addAttributeToClass(codeModel, definedClass, field);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addLinkedDataMethods(Object map) {
		if (map == null) {
			return;
		}
		
		Map<String, Object> linkedData = (Map<String, Object>) map;
		for (String key : linkedData.keySet()) {
			Object services = linkedData.get(key);
			if (services == null || ((List<?>) services).isEmpty()) {
				throw new IllegalArgumentException("Malformed $linkedData schema.");
			}
			
			List<String> serviceNames = (List<String>) linkedData.get(key);
			
			MethodMeta method = this.createLinkedDataMethodMeta(key, serviceNames);
			MethodMeta overridedMethod = method.clone();
			overridedMethod.addParameter(this.createLinkedDataMethodParameter(serviceNames));
			
			method.setBody(method.getBody().replaceAll("\\.withParameters\\([\\w\\d]+\\)\\s*", ""));
			
			this.clazz.addMethod(method);
			this.clazz.addMethod(overridedMethod);
		}
	}
	
	private FieldMeta createLinkedDataMethodParameter(List<String> services) {
		if (services.isEmpty()) {
			return null;
		}
		
		String serviceType = CodeBuilderHelper.sanitizedTypeName(services.get(services.size() - 1));
		String name = serviceType.substring(0, 1).toLowerCase() + serviceType.substring(1);
		
		FieldMeta field = new FieldMeta();
		field.setName(name);
		field.setType(this.clazz.getPackageName().replace(".model", ".parameters.") + serviceType + "Parameters");
		
		return field;
	}
	
	private MethodMeta createLinkedDataMethodMeta(String name, List<String> services) {
		MethodMeta method = new MethodMeta();
		
		method.setName("get" + StringUtils.capitalize(name));
		method.setVisibility(Visibility.PUBLIC);
		method.setReturnType(this.getLinkedDataMethodReturnType(services));
		method.setBody(this.getLinkedDataMethodBody(services));
		
		return method;
	}
	
	private String getLinkedDataMethodBody(List<String> services) {
		StringBuilder builder = new StringBuilder();
		builder
			.append("return ")
			.append(this.clazz.getPackageName().replace(".model", ".service."))
			.append("ApiMapService.instance()")
			.append("\n" + CodeBuilderHelper.tabulation(3));
		
		for (String service : services) {
			String type = CodeBuilderHelper.sanitizedTypeName(service);
			String serviceMethodName = type.substring(0, 1).toLowerCase() + type.substring(1);
			String parameterName = this.getLinkedDataMethodParameterStatement(service);
			
			builder.append(String.format("._%s(%s)", serviceMethodName, parameterName));
		}
		
		String paramName = CodeBuilderHelper.sanitizedTypeName(services.get(services.size() - 1));
		paramName = paramName.substring(0, 1).toLowerCase() + paramName.substring(1);
		
		return builder
			.append("\n" + CodeBuilderHelper.tabulation(3))
			.append(".withParameters(" + paramName + ")")
			.append("\n" + CodeBuilderHelper.tabulation(3))
			.append(".httpGet();")
			.toString();
	}
	
	private String getLinkedDataMethodParameterStatement(String service) {
		String type = CodeBuilderHelper.sanitizedTypeName(service);
		
		if (service.endsWith("}")) {
			List<String> names = new ArrayList<String>();
			for (FieldMeta field : this.clazz.getFields()) {
				if (type.endsWith(StringUtils.capitalize(field.getName()))) {
					names.add(field.getName());
				}
			}
			
			int max = 0, index = 0;
			for (int i = 0; i < names.size(); i++) {
				if (max < names.get(i).length()) {
					max = names.get(i).length();
					index = i;
				}
			}
			
			return (names.isEmpty()) ? "null" : "this." + names.get(index) + ".toString()";
		}
		
		return "";
	}
	
	private String getLinkedDataMethodReturnType(List<String> services) {
		Map<?, ?> map = (Map<?, ?>) GeneratorHelper.getDataFromCurrentRamlHelper(services);
		if (map.get("type") != null) {
			Map<String, Map<String, ?>> type = (Map<String, Map<String, ?>>) map.get("type");
			String key = type.keySet().iterator().next();
			Map<String, String> schemaTypes = (Map<String, String>) type.get(key);
			
			if (StringUtils.isEmpty(schemaTypes.get("collectionSchema"))) {
				return CodeBuilderHelper.getJavaType(schemaTypes.get("schema"));
			} else {
				return CodeBuilderHelper.getJavaType(schemaTypes.get("collectionSchema"));
			}
		} else {
			return Object.class.getName();
		}
	}

	private Map<?, ?> parseJsonString(String json) {
		try {
			return OBJECT_MAPPER.readValue(json, HashMap.class);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public ClassMeta getClazz() {
		return clazz;
	}
	
	public ModelBuilder withClazz(ClassMeta clazz) {
		this.clazz = clazz;
		return this;
	}
}