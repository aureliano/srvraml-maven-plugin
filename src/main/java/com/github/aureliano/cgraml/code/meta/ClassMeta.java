package com.github.aureliano.cgraml.code.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassMeta {

	private String packageName;
	private String className;
	private String javaDoc;
	private Set<FieldMeta> fields;
	private List<MethodMeta> methods;
	private List<String> interfaces;
	
	public ClassMeta() {
		this.fields = new HashSet<FieldMeta>();
		this.methods = new ArrayList<MethodMeta>();
		this.interfaces = new ArrayList<String>();
	}

	public String getPackageName() {
		return packageName;
	}

	public ClassMeta withPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	public String getClassName() {
		return className;
	}

	public ClassMeta withClassName(String className) {
		this.className = className;
		return this;
	}

	public String getJavaDoc() {
		return javaDoc;
	}

	public ClassMeta withJavaDoc(String javaDoc) {
		this.javaDoc = javaDoc;
		return this;
	}

	public Set<FieldMeta> getFields() {
		return fields;
	}

	public ClassMeta withFields(Set<FieldMeta> fields) {
		this.fields = fields;
		return this;
	}

	public List<MethodMeta> getMethods() {
		return methods;
	}

	public ClassMeta withMethods(List<MethodMeta> methods) {
		this.methods = methods;
		return this;
	}

	public ClassMeta addField(FieldMeta field) {
		this.fields.add(field);
		return this;
	}
	
	public ClassMeta addMethod(MethodMeta method) {
		this.methods.add(method);
		return this;
	}
	
	public String getCanonicalClassName() {
		return this.packageName + "." + this.className;
	}
	
	public List<String> getInterfaces() {
		return interfaces;
	}
	
	public ClassMeta addInterface(String interfaceName) {
		this.interfaces.add(interfaceName);
		return this;
	}
	
	public ClassMeta withInterfaces(List<String> interfaces) {
		this.interfaces = interfaces;
		return this;
	}
	
	public FieldMeta findField(String fieldName) {
		FieldMeta field = new FieldMeta();
		field.setName(fieldName);
		
		return this.findField(field);
	}
	
	public FieldMeta findField(FieldMeta field) {
		for (FieldMeta f : this.fields) {
			if (f.equals(field)) {
				return f;
			}
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return new StringBuilder("[ ")
			.append("packageName => \"" + this.packageName)
			.append("\", className => \"" + this.className)
			.append("\", javaDoc => \"" + this.javaDoc)
			.append("\", fields => " + this.fields)
			.append(", methods => " + this.methods)
			.append(" ]")
			.toString();
	}
}