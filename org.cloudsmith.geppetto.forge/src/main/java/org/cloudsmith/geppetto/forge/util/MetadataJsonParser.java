/**
 * Copyright (c) 2011 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Cloudsmith
 * 
 */
package org.cloudsmith.geppetto.forge.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.cloudsmith.geppetto.diagnostic.Diagnostic;
import org.cloudsmith.geppetto.diagnostic.DiagnosticType;
import org.cloudsmith.geppetto.diagnostic.FileDiagnostic;
import org.cloudsmith.geppetto.forge.v2.model.ModuleName;
import org.cloudsmith.geppetto.semver.Version;
import org.cloudsmith.geppetto.semver.VersionRange;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;

public abstract class MetadataJsonParser extends JsonPositionalParser {
	public static final DiagnosticType METADATA_JSON = new DiagnosticType(
		"METADATA_JSON", MetadataJsonParser.class.getName());

	protected abstract void call(CallSymbol key, int line, int offset, int length, List<JElement> arguments);

	protected Diagnostic createDiagnostic(JElement element, int severity, String message) {
		FileDiagnostic diag = new FileDiagnostic(severity, METADATA_JSON, message, element.getFile());
		diag.setLineNumber(element.getLine());
		return diag;
	}

	public void parse(File file, String content, Diagnostic chain) throws JsonParseException, IOException {
		JElement root = parse(file, content);
		if(!(root instanceof JObject))
			throw new JsonParseException("Excpected Json Object", JsonLocation.NA);

		for(JEntry entry : ((JObject) root).getEntries()) {
			try {
				CallSymbol symbol = CallSymbol.valueOf(entry.getKey());
				if(symbol == CallSymbol.dependency)
					// Not recognized in metadata.json
					throw new IllegalArgumentException();

				JElement args = entry.getElement();
				if(symbol == CallSymbol.name)
					validateModuleName(args, chain);
				else if(symbol == CallSymbol.version)
					validateVersion(args, chain);
				else
					switch(symbol) {
						case dependencies:
							validateDependencies(args, symbol.name(), chain);
							break;
						case types:
							validateTypes(args, symbol.name(), chain);
							break;
						case checksums:
							validateChecksums(args, symbol.name(), chain);
							break;
						default:
							validateString(args, symbol.name(), chain);
					}

				if(args instanceof JArray)
					call(symbol, entry.getLine(), entry.getOffset(), entry.getLength(), ((JArray) args).getValues());
				else
					call(
						symbol, entry.getLine(), entry.getOffset(), entry.getLength(),
						Collections.singletonList(entry.getElement()));
			}
			catch(IllegalArgumentException e) {
				chain.addChild(createDiagnostic(entry, Diagnostic.ERROR, "Unrecognized call: " + entry.getKey()));
			}
		}
	}

	protected List<JElement> validateArray(JElement element, String symbol, Diagnostic chain) {
		if(element instanceof JArray)
			return ((JArray) element).getValues();

		chain.addChild(createDiagnostic(element, Diagnostic.ERROR, symbol + " must be an array"));
		return Collections.emptyList();
	}

	private void validateChecksums(JElement args, String name, Diagnostic chain) {
		// This is derived material. Hardly a need to validate
	}

	private void validateDependencies(JElement args, String name, Diagnostic chain) {
		for(JElement dep : validateArray(args, name, chain)) {
			for(JEntry entry : validateObject(dep, name, chain)) {
				if("name".equals(entry.getKey()))
					validateModuleName(entry.getElement(), chain);
				else if("version_requirement".equals(entry.getKey()) || "versionRequirement".equals(entry.getKey()))
					validateVersionRequirement(entry.getElement(), chain);
				else
					chain.addChild(createDiagnostic(entry, Diagnostic.WARNING, "Unrecognized entry: " + entry.getKey()));
			}
		}
	}

	protected void validateModuleName(JElement jsonName, Diagnostic chain) {
		String moduleName = validateString(jsonName, CallSymbol.name.name(), chain);
		if(moduleName != null) {
			try {
				new ModuleName(moduleName, true);
			}
			catch(IllegalArgumentException e) {
				try {
					new ModuleName(moduleName, false);
					chain.addChild(createDiagnostic(jsonName, Diagnostic.WARNING, e.getMessage()));
				}
				catch(IllegalArgumentException e2) {
					chain.addChild(createDiagnostic(jsonName, Diagnostic.ERROR, e2.getMessage()));
				}
			}
		}
	}

	protected void validateNamedDocEntry(JElement elem, String name, Diagnostic chain) {
		for(JEntry entry : validateObject(elem, name, chain)) {
			if("name".equals(entry.getKey()) || "doc".equals(entry.getKey()))
				validateString(entry.getElement(), entry.getKey(), chain);
			else
				chain.addChild(createDiagnostic(entry, Diagnostic.WARNING, "Unrecognized entry: " + entry.getKey()));
		}
	}

	protected List<JEntry> validateObject(JElement element, String symbol, Diagnostic chain) {
		if(element instanceof JObject)
			return ((JObject) element).getEntries();

		chain.addChild(createDiagnostic(element, Diagnostic.ERROR, symbol + " must be an object"));
		return Collections.emptyList();
	}

	protected String validateString(JElement element, String symbol, Diagnostic chain) {
		String str = null;
		if(element instanceof JPrimitive) {
			Object value = ((JPrimitive) element).getValue();
			if(value instanceof String)
				str = (String) value;
		}
		if(str == null)
			chain.addChild(createDiagnostic(element, Diagnostic.ERROR, symbol + " must be a string"));
		return str;
	}

	protected void validateTypes(JElement args, String name, Diagnostic chain) {
		for(JElement dep : validateArray(args, name, chain)) {
			for(JEntry entry : validateObject(dep, name, chain)) {
				String key = entry.getKey();
				if("name".equals(entry.getKey()) || "doc".equals(entry.getKey()))
					validateString(entry.getElement(), entry.getKey(), chain);
				else if("parameters".equals(key) || "providers".equals(key) || "properties".equals(key))
					for(JElement param : validateArray(entry.getElement(), key, chain))
						validateNamedDocEntry(param, key, chain);
				else
					chain.addChild(createDiagnostic(entry, Diagnostic.WARNING, "Unrecognized entry: " + entry.getKey()));
			}
		}
	}

	protected void validateVersion(JElement jsonVersion, Diagnostic chain) {
		String version = validateString(jsonVersion, CallSymbol.version.name(), chain);
		if(version != null) {
			try {
				Version.create(version);
			}
			catch(IllegalArgumentException e) {
				chain.addChild(createDiagnostic(jsonVersion, Diagnostic.ERROR, e.getMessage()));
			}
		}
	}

	protected void validateVersionRequirement(JElement jsonVersionRequirement, Diagnostic chain) {
		String versionRequirement = validateString(jsonVersionRequirement, "version_requirement", chain);
		if(versionRequirement != null) {
			try {
				VersionRange.create(versionRequirement);
			}
			catch(IllegalArgumentException e) {
				chain.addChild(createDiagnostic(jsonVersionRequirement, Diagnostic.ERROR, e.getMessage()));
			}
		}
	}
}