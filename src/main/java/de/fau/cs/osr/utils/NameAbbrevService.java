/**
 * Copyright 2011 The Open Source Research Group,
 *                University of Erlangen-Nürnberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fau.cs.osr.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import de.fau.cs.osr.utils.ReflectionUtils.ArrayInfo;

/**
 * If one has a known subset of classes and therefore a known set of class
 * names, one can abbreviate or even leave out the package name without
 * introducing ambiguities.
 * 
 * Given a set of package names, this class can abbreviate names of classes from
 * one of those packages and also retrieve the proper Class<?> object for
 * abbreviated names.
 */
public class NameAbbrevService
{
	private static final String PTK_NS = "http://sweble.org/doc/site/tooling/parser-toolkit/ptk-xml-tools";	
	private static final String PTK = "ptk";
	
	private final List<String[]> packages = new ArrayList<String[]>();
	
	private final boolean strict;
	
	/** Maps Class<?> (Key) -> String (Value) */
	private BidiMap cache;
	/** Maps prefix -> namespace URI */
	private Map<String, String> usedPrefixes = new HashMap<String, String>();
	 
	
	// =========================================================================
	
	public Map<String, String> getUsedPrefixes() {
		return Collections.unmodifiableMap(usedPrefixes);
	}

	/**
	 * The order of packages is vitally important to the process. If resolve()
	 * is called with a different order of package names than abbrev(), some
	 * abbreviated might get resolved to the wrong Class<?>!
	 */
	public NameAbbrevService(String[]... packageNames)
	{
		this(true, packageNames);
	}
	
	/**
	 * The order of packages is vitally important to the process. If resolve()
	 * is called with a different order of package names than abbrev(), some
	 * abbreviated might get resolved to the wrong Class<?>!
	 */
	public NameAbbrevService(boolean strict, String[]... packageNames)
	{
		this.strict = strict;
		
		packages.add(new String[]{"java.lang", PTK, PTK_NS});
		packages.addAll(Arrays.asList(packageNames));
		
		cache = new DualHashBidiMap();
		cache.put(byte.class, PTK + ":byte");
		cache.put(short.class, PTK + ":short");
		cache.put(int.class, PTK + ":int");
		cache.put(long.class, PTK + ":long");
		cache.put(float.class, PTK + ":float");
		cache.put(double.class, PTK + ":double");
		cache.put(boolean.class, PTK + ":boolean");
		cache.put(char.class, PTK + ":char");
	
		for (Iterator<String[]> iter = packages.iterator(); iter.hasNext(); ) {
			String[] namePrefixURI = iter.next();;
			if (namePrefixURI.length > 2) {
				usedPrefixes.put(namePrefixURI[1], namePrefixURI[2]);
			}
		}
	}
	
	// =========================================================================
	
	/**
	 * Return the abbreviated variant of the given class' full name.
	 * 
	 * @throws IllegalArgumentException
	 *             Thrown if the given class is not part of a packge from the
	 *             package list.
	 */
	public String[] abbrev(Class<?> clazz)
	{
		String suffix = "";
		if (clazz.isArray())
		{
			ArrayInfo info = ReflectionUtils.arrayDimension(clazz);
			clazz = info.elementClass;
			suffix = StringUtils.strrep("[]", info.dim);
		}
		// Exchange for the interface class if the name is xImpl
		Class<?> interfaceClazz = null;
		String[] splitName = {clazz.getName()};
		if (splitName[0].endsWith("Impl"))
		{
			try {
				splitName = splitName[0].split("\\$");
				interfaceClazz = Class.forName(splitName[0]);
			} 
			catch (ClassNotFoundException e) 
			{ // implementation my not be nested
				try {
					splitName = splitName[0].split("Impl");
					interfaceClazz = Class.forName(splitName[0]);
				}
				catch (ClassNotFoundException e2)
				{
					// no interface class? strange. whatever ...
				}
			}
			if (interfaceClazz != null && interfaceClazz.isAssignableFrom(clazz))
				clazz = interfaceClazz;
		}
		
		String[] shortName;
		String classWithPrefix = (String)cache.get(clazz);
		if (classWithPrefix != null)
		{
			String[] tmp = classWithPrefix.split(":"); 
			return new String[] {tmp[1] + suffix, tmp[0]}; 
		}
				
		// clazz.getSimpleName(); doesn't work for nested classes!
		String simpleName = clazz.getName();
		{
			int i = simpleName.lastIndexOf('.');
			if (i >= 0)
				simpleName = simpleName.substring(i + 1);
		}
		
		for (Iterator<String> iter = usedPrefixes.keySet().iterator(); iter.hasNext();)
		{
			String prefix = iter.next();
			// Maybe the abbreviated name was already used for another class of the 
			// same name.
			if (cache.containsValue(prefix + ":" + simpleName))
			{
				// Cannot abbreviate any more :(
				shortName = new String[] {clazz.getName(), prefix};
				cache.put(clazz, shortName[1] + ":" + shortName[0]);
				return new String[] {shortName[0] + suffix, prefix};
			}
		}
		
		final String dotSimpleName = "." + simpleName;
		for (String[] pkg : packages)
		{
			try
			{
				Class<?> otherClazz = Class.forName(pkg[0] + dotSimpleName);
				// At this point a class with this simple name has not been 
				// abbreviated. The first package that contains a class with 
				// this simple name will be the one we abbreviate. All others 
				// have to use the full name.
				cache.put(otherClazz, pkg[1] + ":" + simpleName);
				
				if (otherClazz != clazz && !otherClazz.isAssignableFrom(clazz))
				{
					// Cannot abbreviate any more :(
					shortName = new String[] {clazz.getName(), pkg[1]};
					cache.put(clazz, shortName[1] + ":" + shortName[0]);
					return new String[] {shortName[0] + suffix, shortName[1]};
				}
				else
				{
//					shortname never used ...
//					shortName = new String[] {simpleName, pkg[1]};
					return new String[] {simpleName + suffix, pkg[1]};
				}
			}
			catch (ClassNotFoundException e)
			{
			}
		}
		
		if (!strict)
			return new String[] {clazz.getName() + suffix, ""};
		
		throw new IllegalArgumentException("Given class is not part of the package list: " + clazz.getName());
	}
	
	/**
	 * Resolves an abbreviated class name to the corresponding Class<?> object.
	 * 
	 * @throws ClassNotFoundException
	 *             Thrown if the abbreviated class cannot be found in any
	 *             package of the package list.
	 */
	public Class<?> resolve(String abbrev) throws ClassNotFoundException
	{
		int dim = getArrayDim(abbrev);
		
		abbrev = abbrev.substring(0, abbrev.length() - dim * 2);

		for (Iterator<String> iter = usedPrefixes.keySet().iterator(); iter.hasNext();)
		{
			String prefix = iter.next();
			Class<?> clazz = (Class<?>) cache.getKey(prefix + ":" + abbrev);
			if (clazz != null)
				return arrayClassFor(clazz, dim);

			if (abbrev.indexOf('.') >= 0)
			{
				// Full name was given
				clazz = Class.forName(abbrev);
				cache.put(clazz, prefix + ":" + abbrev);
				return arrayClassFor(clazz, dim);
			}
		}
		final String dotSimpleName = "." + abbrev;
		for (String pkg[] : packages)
		{
			try
			{
				Class<?> clazz = Class.forName(pkg[0] + dotSimpleName);
				cache.put(clazz, pkg[1] + ":" + abbrev);
				return arrayClassFor(clazz, dim);
			}
			catch (ClassNotFoundException e)
			{
			}
		}
		
		throw new ClassNotFoundException("Given abbreviated class name was "
				+ "not found in any package of the package list: " + abbrev);
	}
	
	private static int getArrayDim(String abbrev)
	{
		int dim = 0;
		for (int i = abbrev.length() - 1; i >= 0;)
		{
			if (abbrev.charAt(i) == ']')
			{
				--i;
				if (abbrev.charAt(i) == '[')
				{
					--i;
					++dim;
					continue;
				}
			}
			
			break;
		}
		return dim;
	}
	
	private static Class<?> arrayClassFor(Class<?> clazz, int dim) throws ClassNotFoundException
	{
		// restore concrete class for an interface
		if (clazz.isInterface())
		{
			Class<?> concreteClazz = null;
			try {
				concreteClazz = Class.forName(clazz.getName() + "$" + clazz.getSimpleName() + "Impl");
			} 
			catch (ClassNotFoundException e) 
			{ // implementation my not be nested
				concreteClazz = Class.forName(clazz.getName() + "Impl");
			}
			clazz = concreteClazz;
		}
		if (dim == 0)
			return clazz;
		return ReflectionUtils.arrayClassFor(clazz, dim);
	}
}
