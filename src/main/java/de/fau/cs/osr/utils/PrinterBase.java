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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

public class PrinterBase
{
	public PrinterBase(Writer writer)
	{
		this.out = new PrintWriter(writer);
	}
	
	// =========================================================================
	
	private final Stack<State> stateStack = new Stack<State>();
	
	private static final class State
	{
		public PrintWriter out;
		
		public int indent;
		
		public int hadNewlines;
		
		public int needNewlines;
		
		public State(
				PrintWriter out,
				int indent,
				int hadNewlines,
				int needNewlines)
		{
			this.out = out;
			this.indent = indent;
			this.hadNewlines = hadNewlines;
			this.needNewlines = needNewlines;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + hadNewlines;
			result = prime * result + indent;
			result = prime * result + needNewlines;
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			State other = (State) obj;
			if (hadNewlines != other.hadNewlines)
				return false;
			if (indent != other.indent)
				return false;
			if (needNewlines != other.needNewlines)
				return false;
			return true;
		}
	}
	
	public State push()
	{
		State state = getState();
		stateStack.push(state);
		return state;
	}
	
	public State pop()
	{
		return stateStack.pop();
	}
	
	public State restore()
	{
		State state = stateStack.pop();
		setState(state);
		return state;
	}
	
	private State getState()
	{
		return new State(out, indent, hadNewlines, needNewlines);
	}
	
	public void setState(State state)
	{
		this.out = state.out;
		this.indent = state.indent;
		this.hadNewlines = state.hadNewlines;
		this.needNewlines = state.needNewlines;
	}
	
	public void setStateNotOut(State state)
	{
		this.indent = state.indent;
		this.hadNewlines = state.hadNewlines;
		this.needNewlines = state.needNewlines;
	}
	
	// =========================================================================
	
	public final class OutputBuffer
	{
		private StringWriter w = new StringWriter();
		
		private State stateOnStart;
		
		private State stateOnStop;
		
		public OutputBuffer()
		{
			stateOnStart = push();
			out = new PrintWriter(w);
		}
		
		public void stop()
		{
			if (isStopped())
				throw new UnsupportedOperationException("Already stopped!");
			stateOnStop = getState();
			restore();
		}
		
		public boolean isStopped()
		{
			return stateOnStop != null;
		}
		
		public String getBuffer()
		{
			return w.toString();
		}
		
		public void flush()
		{
			if (!isStopped())
				stop();
			setStateNotOut(stateOnStop);
			out.append(getBuffer());
		}
		
		public State getStateOnStart()
		{
			return stateOnStart;
		}
		
		public State getStateOnStop()
		{
			return stateOnStop;
		}
	}
	
	public OutputBuffer outputBufferStart()
	{
		return new OutputBuffer();
	}
	
	// =========================================================================
	
	private final ArrayList<String> indentStrings =
			new ArrayList<String>(Arrays.asList(""));
	
	private PrintWriter out;
	
	private int indent = 0;
	
	private int hadNewlines = 1;
	
	private int needNewlines = 0;
	
	// =========================================================================
	
	public void incIndent()
	{
		++indent;
		while (indentStrings.size() <= indent)
			indentStrings.add(indentStrings.get(indentStrings.size() - 1) + "  ");
	}
	
	public void decIndent()
	{
		assert indent > 0;
		--indent;
	}
	
	public void indent()
	{
		needNewlines(1);
		if (indent > 0)
			print(indentStrings.get(indent));
	}
	
	public void indent(String text)
	{
		indent();
		print(text);
	}
	
	public void indentln(char ch)
	{
		indent();
		println(ch);
	}
	
	public void indentln(String text)
	{
		indent();
		println(text);
	}
	
	public void ignoreNewlines()
	{
		needNewlines = 0;
	}
	
	public void print(char ch)
	{
		flush();
		out.print(ch);
		hadNewlines = 0;
	}
	
	public void print(String text)
	{
		flush();
		out.print(text);
		hadNewlines = 0;
	}
	
	public void println()
	{
		needNewlines(1);
	}
	
	public void println(char ch)
	{
		print(ch);
		needNewlines(1);
	}
	
	public void println(String text)
	{
		print(text);
		needNewlines(1);
	}
	
	public void println(Object o)
	{
		print(o.toString());
		needNewlines(1);
	}
	
	public void needNewlines(int i)
	{
		if (i > needNewlines)
			needNewlines = i;
	}
	
	public void forceln()
	{
		int newlines = needNewlines - hadNewlines;
		while (newlines-- > 0)
			out.println();
		hadNewlines += newlines;
		needNewlines = 0;
	}
	
	public void flush()
	{
		forceln();
	}
	
	// =========================================================================
	
	private final HashMap<Memoize, Memoize> cache = new HashMap<Memoize, Memoize>();
	
	private int reuse = 0;
	
	private boolean memoize = true;
	
	// =========================================================================
	
	public void setMemoize(boolean memoize)
	{
		this.memoize = memoize;
	}
	
	public boolean isMemoize()
	{
		return memoize;
	}
	
	// =========================================================================
	
	public Memoize memoizeStart(Object node)
	{
		if (!memoize)
		{
			return new Memoize((Object) null, (State) null);
		}
		else
		{
			Memoize m = cache.get(new Memoize(node, getState()));
			if (m == null)
			{
				return new Memoize(node, outputBufferStart());
			}
			else
			{
				++reuse;
				m.getOutputBuffer().flush();
				return null;
			}
		}
	}
	
	public void memoizeStop(Memoize m)
	{
		if (memoize)
		{
			m.getOutputBuffer().flush();
			cache.put(m, m);
		}
	}
	
	public void printMemoizationStats()
	{
		System.out.format(
				"% 6d / % 6d / %2.2f\n",
				cache.size(),
				reuse,
				(float) reuse / (float) cache.size());
	}
	
	// =========================================================================
	
	public static final class Memoize
	{
		private final Object node;
		
		private final State state;
		
		private final OutputBuffer outputBuffer;
		
		public Memoize(Object node, State state)
		{
			this.node = node;
			this.state = state;
			this.outputBuffer = null;
		}
		
		public Memoize(Object node, OutputBuffer outputBuffer)
		{
			this.node = node;
			this.state = outputBuffer.getStateOnStart();
			this.outputBuffer = outputBuffer;
		}
		
		public OutputBuffer getOutputBuffer()
		{
			return outputBuffer;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + ((state == null) ? 0 : state.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Memoize other = (Memoize) obj;
			if (node == null)
			{
				if (other.node != null)
					return false;
			}
			else if (!node.equals(other.node))
				return false;
			if (state == null)
			{
				if (other.state != null)
					return false;
			}
			else if (!state.equals(other.state))
				return false;
			return true;
		}
	}
}
