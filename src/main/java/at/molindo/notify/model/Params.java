/**
 * Copyright 2010 Molindo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.molindo.notify.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Params implements Cloneable {

	private Map<String, ParamValue> _params = Maps.newHashMap();

	private final Params _defaults;

	public Params() {
		this(null);
	}

	public Params(Params defaults) {
		if (defaults != null) {
			_defaults = defaults.clone();
			_params.putAll(_defaults._params);
		} else {
			_defaults = null;
		}
	}

	public <T> Params set(Param<T> param, T value) {
		if (value == null) {
			if (_params.remove(param.getName()) != null && _defaults != null) {
				T def = _defaults.get(param);
				if (def != null) {
					_params.put(param.getName(), param.value(def));
				}
			}
		} else {
			_params.put(param.getName(), param.value(value));
		}
		return this;
	}

	public <T> T get(Param<T> param) {
		ParamValue v = _params.get(param.getName());
		if (v == null || v.getValue() == null) {
			return null;
		}

		if (param.getType().isAssignableFrom(v.getValue().getClass())) {
			// compatible types
			return param.getType().cast(v.getValue());
		} else {
			// convert to string and back to object
			Param<?> valueParam = v.getType().p(param.getName());
			String valueStr = valueParam.toString(v.getValue());
			return param.toObject(valueStr);
		}
	}

	public boolean containsAll(Param<?>... params) {
		for (Param<?> p : params) {
			if (!_params.containsKey(p.getName())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * set all params from passed object, overwriting current mappings
	 * 
	 * @param params
	 * @return
	 */
	public Params setAll(Params params) {
		if (params != null) {
			_params.putAll(params._params);
		}
		return this;
	}

	public Map<String, Object> newMap() {
		Map<String, Object> map = Maps.newHashMap();
		for (Map.Entry<String, ParamValue> e : _params.entrySet()) {
			if (e.getValue() != null && e.getValue().getValue() != null) {
				map.put(e.getKey(), e.getValue().getValue());
			}
		}
		return map;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (_params == null ? 0 : _params.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Params)) {
			return false;
		}
		Params other = (Params) obj;
		if (_params == null) {
			if (other._params != null) {
				return false;
			}
		} else if (!_params.equals(other._params)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Params [params=" + _params + "]";
	}

	@Override
	protected Params clone() {
		Params p;
		try {
			p = (Params) super.clone();
			p._params = Maps.newHashMap(_params);
			return p;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("can't clone object?", e);
		}
	}

	public List<ParamValue> getValues() {
		return Lists.newArrayList(_params.values());
	}

	public Params setValues(List<ParamValue> values) {
		_params.clear();
		for (ParamValue v : values) {
			_params.put(v.getName(), v);
		}
		return this;
	}
}
