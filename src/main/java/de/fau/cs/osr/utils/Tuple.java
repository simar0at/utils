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

public final class Tuple
{
	public static <T1, T2> Tuple2<T1, T2> from(T1 _1, T2 _2)
	{
		return new Tuple2<T1, T2>(_1, _2);
	}
	
	public static <T1, T2, T3> Tuple3<T1, T2, T3> from(T1 _1, T2 _2, T3 _3)
	{
		return new Tuple3<T1, T2, T3>(_1, _2, _3);
	}
}
