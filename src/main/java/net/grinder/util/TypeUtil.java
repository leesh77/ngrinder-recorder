/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package net.grinder.util;

/**
 * Set of type related utility methods.
 * 
 * @author JunHo Yoon
 * @since 1.0
 */
public abstract class TypeUtil {
	/**
	 * Convert the type of parameter to returned type.
	 * 
	 * @param t
	 *            instance to be converted
	 * @param <T>
	 *            converted type.
	 * @return converted type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object t) {
		return (T) t;
	}
}
