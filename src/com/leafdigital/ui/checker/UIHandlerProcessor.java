/*
This file is part of leafdigital leafChat.

leafChat is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

leafChat is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with leafChat. If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.ui.checker;

import java.io.File;
import java.net.*;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic;
import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.leafdigital.ui.api.*;
import com.sun.source.util.*;

/**
 * Annotation processor that checks the UIHandler annotations to make sure
 * they really define all the correct callbacks.
 */
@SupportedAnnotationTypes("com.leafdigital.ui.api.*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions("xmlroot")
public class UIHandlerProcessor extends AbstractProcessor
{
	private static class Handler
	{
		TypeElement element;
		String[] xmlFiles;

		private Handler(TypeElement element, String[] xmlFiles)
		{
			this.element = element;
			this.xmlFiles = xmlFiles;
		}
	}

	private static class XmlData extends DefaultHandler
	{
		Set<String> requiredCallbacks = new HashSet<String>();
		Set<String> availableIds = new HashSet<String>();

		Map<String, Set<String>> callbacks;

		private XmlData(Map<String, Set<String>> callbacks)
		{
			this.callbacks = callbacks;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException
		{
			// See if there's an id attribute
			String id = attributes.getValue("id");
			if(id != null)
			{
				availableIds.add(id);
			}

			// See if there's any required callback attributes
			Set<String> callbackAttributes = callbacks.get(qName);
			if(callbackAttributes != null)
			{
				for(String callbackAttribute : callbackAttributes)
				{
					String callback = attributes.getValue(callbackAttribute);
					if(callback != null)
					{
						requiredCallbacks.add(callback);
					}
				}
			}
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
		RoundEnvironment roundEnv)
	{
		// Initialise data
		Map<String, Set<String>> callbacks = new HashMap<String, Set<String>>();
		List<Handler> handlers = new LinkedList<Handler>();
		Set<Element> actions = new HashSet<Element>();

		// Loop around, gathering all data
		for(TypeElement annotation : annotations)
		{
			String name = annotation.getSimpleName().toString();
			if(name.equals("UICallback"))
			{
				processCallback(annotation, roundEnv, callbacks);
			}
			else if(name.equals("UIHandler"))
			{
				processHandler(annotation, roundEnv, handlers);
			}
			else if(name.equals("UIAction"))
			{
				processAction(annotation, roundEnv, actions);
			}
			else
			{
				throw new Error("wtf: " + name);
			}
		}

		finalCheck(callbacks, handlers, actions);
		return true;
	}

	/**
	 * @param annotation Annotation
	 * @param roundEnv Environment
	 * @param callbacks List of callbacks (to add to)
	 */
	private void processCallback(TypeElement annotation,
		RoundEnvironment roundEnv, Map<String, Set<String>> callbacks)
	{
		// Loop through all elements annotated with @UICallback
		for(Element element : roundEnv.getElementsAnnotatedWith(annotation))
		{
			// Get the method name
			String methodName = element.getSimpleName().toString();
			if(!methodName.startsWith("set"))
			{
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
					"@UICallback can only be applied to set methods",
					element);
				continue;
			}

			// Get the interface name
			Element parent = element.getEnclosingElement();
			if(parent.getKind() != ElementKind.INTERFACE)
			{
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
					"@UICallback can only be applied to method within an interface",
					element);
				continue;
			}
			String interfaceName = parent.getSimpleName().toString();

			// Store in map (without the 'set')
			Set<String> set = callbacks.get(interfaceName);
			if(set == null)
			{
				set = new HashSet<String>();
				callbacks.put(interfaceName, set);
			}
			set.add(methodName.substring(3));
		}
	}

	/**
	 * @param annotation Annotation
	 * @param roundEnv Environment
	 * @param handlers List of handlers (to add to)
	 */
	private void processHandler(TypeElement annotation, RoundEnvironment roundEnv,
		List<Handler> handlers)
	{
		// Loop through all elements annotated with @UIHandler
		for(Element element : roundEnv.getElementsAnnotatedWith(annotation))
		{
			// Get the list of xml files and add
			handlers.add(new Handler((TypeElement)element,
				element.getAnnotation(UIHandler.class).value()));
		}
	}

	/**
	 * @param annotation Annotation
	 * @param roundEnv Environment
	 * @param actions Set of actions (to add to)
	 */
	private void processAction(TypeElement annotation,
		RoundEnvironment roundEnv, Set<Element> actions)
	{
		for(Element element : roundEnv.getElementsAnnotatedWith(annotation))
		{
			// Add all actions to the list except runtime ones, because we don't
			// check those ones
			if(!element.getAnnotation(UIAction.class).runtime())
			{
				actions.add(element);
			}
		}
	}

	/**
	 * Now do the final check, loading all the xml files and so on.
	 * @param callbacks List of known callback methods
	 * @param handlers List of required handlers
	 * @param actions List of action methods that must be used
	 */
	private void finalCheck(Map<String, Set<String>> callbacks,
		List<Handler> handlers, Set<Element> actions)
	{
		Trees trees = null;
		try
		{
			// This is Sun-specific API. Only works in javac, not in Eclipse or
			// other builder.
			trees = Trees.instance(processingEnv);
		}
		catch(Exception e)
		{
			// Leave it null (check later)
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();

		// Track IDs that are required and found. This has to be stored up for
		// the end because there are some parts of the code where the id value
		// is only set by certain subclasses and not others.
		Map<Element, String> requiredIds = new HashMap<Element, String>();
		Set<Element> foundIds = new HashSet<Element>();

		// Track methods that don't have @UIAction so we only warn once
		Set<Element> warnedMethods = new HashSet<Element>();

		for(Handler handler : handlers)
		{
			// Get information about elements of the type (fields, methods)
			Map<String, Element> uiFields = new HashMap<String, Element>();
			Map<String, Element> methods = new HashMap<String, Element>();
			fillClassData(handler.element, uiFields, methods);

			// Get file location of Java source and get its folder
			File folder;
			if(trees != null)
			{
				// Sun-specific version
				TreePath tp = trees.getPath(handler.element);
				URI fileUri;
				try
				{
					fileUri = new URI("file:///").resolve(
						tp.getCompilationUnit().getSourceFile().toUri());
				}
				catch(URISyntaxException ex)
				{
					throw new Error("wtf: " + ex.getMessage());
				}
				folder = (new File(fileUri)).getParentFile();
			}
			else
			{
				// Generic version requires source path option
				String path = processingEnv.getOptions().get("src");
				if(path == null)
				{
					throw new Error("Annotation processor requires option 'src' (set it"
						+ " to absolute path of source folder) when not using Sun javac"
						+ " compiler");
				}
				folder = new File(path);
				if(!new File(folder, "com").exists())
				{
					throw new Error("Annotation processor option 'src' appears to be "
						+ " incorrect (set to absolute path of source folder)");
				}

				// Find outer class
				TypeElement reference = handler.element;
				while(reference.getEnclosingElement().getKind() == ElementKind.CLASS)
				{
					reference = (TypeElement)reference.getEnclosingElement();
				}

				// Get absolute path of class
				String name = reference.getQualifiedName().toString();
				// Remove class name
				name = name.replaceAll("\\.[^.]+$", "");
				// Turn . into /
				name = name.replace('.', '/');
				folder = new File(folder, name);
			}

			// Find XML files and make a list of required callbacks, available ids
			XmlData xmlData = new XmlData(callbacks);
			boolean fileErrors = false;
			for(String xmlFile : handler.xmlFiles)
			{
				File xml = new File(folder, xmlFile + ".xml");
				if(!xml.exists())
				{
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"XML file '" + xmlFile + ".xml' not found", handler.element);
					fileErrors = true;
					continue;
				}

				try
				{
					SAXParser parser = factory.newSAXParser();
					parser.parse(xml, xmlData);
				}
				catch(Exception e)
				{
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"Error parsing XML file '" + xmlFile + ".xml': " + e.getMessage(),
						handler.element);
					fileErrors = true;
				}
			}

			// If there's a file error, stop because other errors are likely
			// caused by it
			if(fileErrors)
			{
				continue;
			}

			// Check all the fields are present in xml
			for(Map.Entry<String, Element> entry : uiFields.entrySet())
			{
				String requiredId = entry.getKey();
				Element idElement = entry.getValue();

				// If the id is not in this current xml file
				if(!xmlData.availableIds.contains(requiredId))
				{
					// If we haven't already found an id for this field...
					if (!foundIds.contains(idElement))
					{
						// ...then add it to the required list
						requiredIds.put(idElement, requiredId);
					}
				}
				else
				{
					// It is in the current xml file, so let's remember that we found it
					// and also note that it's not required any more
					foundIds.add(idElement);
					requiredIds.remove(idElement);
				}
			}

			// Check all the callbacks are present in code
			for(String requiredCallback : xmlData.requiredCallbacks)
			{
				if(methods.containsKey(requiredCallback))
				{
					Element methodElement = methods.get(requiredCallback);

					// This method was used, so remove it from the required action list
					actions.remove(methodElement);

					// Check if it has the annotation
					if(methodElement.getAnnotation(UIAction.class) == null &&
						!warnedMethods.contains(methodElement))
					{
						processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
							"Callback referenced in XML should be marked with @UIAction",
							methodElement);
						warnedMethods.add(methodElement);
					}
				}
				else
				{
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"Missing callback '" + requiredCallback + "'", handler.element);
				}
			}
		}

		for(Map.Entry<Element, String> entry : requiredIds.entrySet())
		{
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
				"No element with id '" + entry.getValue() + "' in attached XML files",
				entry.getKey());
		}

		for(Element element : actions)
		{
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
				"No callback for method in attached XML files"
				+ " (remove @UIAction or set runtime=true)",
				element);
		}
	}

	/**
	 * Fulls all the method and field data from a class. Recursively includes
	 * superclasses.
	 * @param element Element of class
	 * @param uiFields UI field map
	 * @param methods Method map
	 */
	private void fillClassData(TypeElement element,
		Map<String, Element> uiFields, Map<String, Element> methods)
	{
		TypeMirror parent = element.getSuperclass();
		if(parent.getKind() != TypeKind.NONE)
		{
			fillClassData(
				(TypeElement)processingEnv.getTypeUtils().asElement(parent),
				uiFields, methods);
		}

		for(Element e : element.getEnclosedElements())
		{
			// Check UI fields
			String name = e.getSimpleName().toString();
			if(e.getKind() == ElementKind.FIELD && name.endsWith("UI"))
			{
				if(!e.getModifiers().contains(Modifier.PUBLIC))
				{
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"UI field must be declared as public", e);
				}
				else
				{
					// Store name without UI suffix
					uiFields.put(name.substring(0, name.length()-2), e);
				}
			}
			else if(e.getKind() == ElementKind.METHOD)
			{
				if(e.getModifiers().contains(Modifier.PUBLIC))
				{
					// Store name
					methods.put(name, e);
				}
			}
		}
	}
}
