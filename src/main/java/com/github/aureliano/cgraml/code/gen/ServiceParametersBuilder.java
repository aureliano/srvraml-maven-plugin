package com.github.aureliano.cgraml.code.gen;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.raml.model.ActionType;

import com.github.aureliano.cgraml.code.builder.IBuilder;
import com.github.aureliano.cgraml.code.meta.ActionMeta;
import com.github.aureliano.cgraml.code.meta.ClassMeta;
import com.github.aureliano.cgraml.code.meta.FieldMeta;
import com.github.aureliano.cgraml.code.meta.MethodMeta;
import com.github.aureliano.cgraml.code.meta.ServiceMeta;
import com.github.aureliano.cgraml.code.meta.Visibility;
import com.github.aureliano.cgraml.helper.CodeBuilderHelper;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;

public class ServiceParametersBuilder implements IBuilder {

	private ClassMeta clazz;
	private static final Set<String> GENERATED_CLASSES = new HashSet<String>();
	
	public ServiceParametersBuilder() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceParametersBuilder parse(String pkg, String entity, Object resource) {
		ServiceMeta service = (ServiceMeta) resource;
		
		this.clazz = new ClassMeta()
			.withPackageName(pkg + ".parameters")
			.withJavaDoc("Generated by srvraml-maven-plugin.")
			.withClassName(CodeBuilderHelper.sanitizedTypeName(service.getUri()) + "Parameters");
	
		if (GENERATED_CLASSES.contains(this.clazz.getCanonicalClassName())) {
			throw new IllegalArgumentException("Class " + this.clazz.getCanonicalClassName() + " was already generated before. Skipping!");
		}
		
		ActionMeta action = this.getGetAction(service);
		if (action == null) {
			throw new IllegalArgumentException("Service " + service.getUri() + " does not have a GET method. Skipping!");
		} else if (action.getParameters().isEmpty()) {
			throw new IllegalArgumentException("GET method of service '" + service.getUri() + "' does not have any parameters. Skipping!");
		}
		
		this.addAttributesToClass(action);
		this.addAccessorMethods();
		
		GENERATED_CLASSES.add(this.clazz.getCanonicalClassName());
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceParametersBuilder build() {
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

	private void appendClassAttributes(JCodeModel codeModel, JDefinedClass definedClass) {
		for (FieldMeta field : this.clazz.getFields()) {
			CodeBuilderHelper.addAttributeToClass(codeModel, definedClass, field);
		}
	}

	private void appendClassMethods(JCodeModel codeModel, JDefinedClass definedClass) {
		for (MethodMeta method : this.clazz.getMethods()) {
			CodeBuilderHelper.addMethodToClass(codeModel, definedClass, method);
		}
	}

	private void addAccessorMethods() {
		for (FieldMeta field : this.clazz.getFields()) {
			this.clazz.addMethod(CodeBuilderHelper.createGetterMethod(field));
			this.clazz.addMethod(CodeBuilderHelper.createBuilderMethod(this.clazz.getClassName(), field));
		}
	}

	private void addAttributesToClass(ActionMeta action) {
		for (FieldMeta field : action.getParameters()) {
			field.setVisibility(Visibility.PRIVATE);
			this.clazz.addField(field);
		}
	}
	
	private ActionMeta getGetAction(ServiceMeta service) {
		for (ActionMeta action : service.getActions()) {
			if (ActionType.GET.equals(action.getMethod())) {
				return action;
			}
		}
		
		return null;
	}
	
	public ClassMeta getClazz() {
		return clazz;
	}
	
	public ServiceParametersBuilder withClazz(ClassMeta clazz) {
		this.clazz = clazz;
		return this;
	}
}