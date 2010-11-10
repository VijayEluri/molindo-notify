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

import java.io.NotSerializableException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;

import at.molindo.utils.data.HexUtils;
import at.molindo.utils.data.SerializationUtils;
import at.molindo.utils.data.StringUtils;

public abstract class Param<T> {

	private String _name;
	private Class<T> _type;

	public static Param<String> pString(String name) {
		return new Param<String>(name, String.class) {

			@Override
			protected String string(String object) {
				return object;
			}

			@Override
			protected String object(String string) {
				return string;
			}

			@Override
			protected Type type() {
				return Type.STRING;
			}

		};
	}

	public static Param<Integer> pInteger(String name) {
		return new Param<Integer>(name, Integer.class) {

			@Override
			protected Integer object(String string) {
				return Integer.parseInt(string);
			}

			@Override
			protected Type type() {
				return Type.INTEGER;
			}
		};
	}

	public static Param<Long> pLong(String name) {
		return new Param<Long>(name, Long.class) {

			@Override
			protected Long object(String string) {
				return Long.parseLong(string);
			}

			@Override
			protected Type type() {
				return Type.LONG;
			}
		};
	}

	public static Param<Double> pDouble(String name) {
		return new Param<Double>(name, Double.class) {

			@Override
			protected Double object(String string) {
				return Double.parseDouble(string);
			}

			@Override
			protected Type type() {
				return Type.DOUBLE;
			}
		};
	}

	public static Param<Float> pFloat(String name) {
		return new Param<Float>(name, Float.class) {

			@Override
			protected Float object(String string) {
				return Float.parseFloat(string);
			}

			@Override
			protected Type type() {
				return Type.FLOAT;
			}
		};
	}

	public static Param<Boolean> pBoolean(String name) {
		return new Param<Boolean>(name, Boolean.class) {

			@Override
			protected Boolean object(String string) {
				return Boolean.parseBoolean(string);
			}

			@Override
			protected Type type() {
				return Type.BOOLEAN;
			}
		};
	}

	public static Param<Character> pCharacter(String name) {
		return new Param<Character>(name, Character.class) {

			@Override
			protected Character object(String string) {
				return string.charAt(0);
			}

			@Override
			protected Type type() {
				return Type.CHARACTER;
			}
		};
	}

	public static Param<URL> pURL(String name) {
		return new Param<URL>(name, URL.class) {

			@Override
			protected URL object(String string) {
				try {
					return new URL(string);
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected Type type() {
				return Type.URL;
			}
		};
	}

	public static Param<Object> pSerializable(String name) {
		return new Param<Object>(name, Object.class) {

			@Override
			protected Object object(String string) {
				try {
					return SerializationUtils.deserialize(HexUtils.bytes(string));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected String string(Object object) {
				try {
					return HexUtils.string(SerializationUtils.serialize(object));
				} catch (NotSerializableException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected Type type() {
				return Type.SERIALIZABLE;
			}
		};
	}

	public static Param<Object> pObject(String name) {
		return new Param<Object>(name, Object.class) {

			@Override
			protected String string(Object object) {
				throw new RuntimeException("can't convert from unknown type to string " + object);
			}

			@Override
			protected Object object(String string) {
				throw new RuntimeException("can't convert to unknown type from " + string);
			}

			@Override
			protected Type type() {
				return Type.OBJECT;
			}
		};
	}

	private Param() {
	}

	private Param(String name, Class<T> cls) {
		this();
		setName(name);
		setType(cls);
	}

	public String getName() {
		return _name;
	}

	protected void setName(String name) {
		if (name == null) {
			throw new NullPointerException("name");
		}
		if (StringUtils.empty(name)) {
			throw new IllegalArgumentException("name must not be empty");
		}
		_name = name;
	}

	public Class<T> getType() {
		return _type;
	}

	protected void setType(Class<T> type) {
		if (type == null) {
			throw new NullPointerException("type");
		}
		_type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (getName() == null ? 0 : getName().hashCode());
		result = prime * result + (getType() == null ? 0 : getType().getName().hashCode());
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
		if (!(obj instanceof Param)) {
			return false;
		}
		Param<?> other = (Param<?>) obj;
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		if (getType() == null) {
			if (other.getType() != null) {
				return false;
			}
		} else if (!getType().getName().equals(other.getType().getName())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Param [name=" + _name + ", type=" + _type == null ? null : _type.getSimpleName() + "]";
	}

	public ParamValue value(T value) {
		return new ParamValue(this, value);
	}

	public final String toString(Object o) {
		if (o == null) {
			return null;
		}
		return string(getType().cast(o));
	}

	public final T toObject(String string) {
		if (string == null) {
			return null;
		}
		return object(string);
	}

	protected String string(@Nonnull T object) {
		return object.toString();
	}

	@Nonnull
	protected abstract T object(@Nonnull String string);

	protected abstract Type type();

	public enum Type {
		STRING {

			@Override
			Param<String> p(String name) {
				return pString(name);
			}

		},
		INTEGER {

			@Override
			Param<Integer> p(String name) {
				return pInteger(name);
			}

		},
		LONG {

			@Override
			Param<Long> p(String name) {
				return pLong(name);
			}

		},
		DOUBLE {

			@Override
			Param<Double> p(String name) {
				return pDouble(name);
			}

		},
		FLOAT {

			@Override
			Param<Float> p(String name) {
				return pFloat(name);
			}

		},
		BOOLEAN {

			@Override
			Param<Boolean> p(String name) {
				return pBoolean(name);
			}

		},
		CHARACTER {

			@Override
			Param<Character> p(String name) {
				return pCharacter(name);
			}

		},
		URL {

			@Override
			Param<URL> p(String name) {
				return pURL(name);
			}

		},
		SERIALIZABLE {

			@Override
			Param<Object> p(String name) {
				return pSerializable(name);
			}
		},
		OBJECT {
			@Override
			Param<Object> p(String name) {
				return pObject(name);
			}
		};

		abstract Param<?> p(String name);
	}
}
