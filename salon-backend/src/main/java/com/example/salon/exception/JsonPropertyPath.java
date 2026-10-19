package com.example.salon.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * Turns the path of a request body field that failed validation, which Bean Validation gives in Java names
 * such as {@code addresses[0].postalCode}, into the names the client sent, such as
 * {@code addresses[0].postal_code}. A request body is a record, and Jackson names each of its components
 * by its {@link JsonProperty}, or else by its Java name. A part of the path that can't be followed keeps its
 * Java name, except a last part that is a rule over several fields rather than a field, such as
 * {@code AddressRequest.isCoordinatePair()}: it is left out, as the rule's message names the fields.
 */
final class JsonPropertyPath
{
	private JsonPropertyPath()
	{
	}

	/** The path in JSON names; empty for a rule over the whole body, such as an address's coordinate pair. */
	static String of(Class<?> body, String javaPath)
	{
		StringBuilder json = new StringBuilder();
		Type type = body;
		String[] segments = javaPath.split("\\.");
		for (int s = 0; s < segments.length; s++) {
			String segment = segments[s];
			int bracket = segment.indexOf('[');
			String name = bracket < 0 ? segment : segment.substring(0, bracket);
			String indexes = bracket < 0 ? "" : segment.substring(bracket);

			Class<?> owner = rawClass(type);
			Field field = findField(owner, name);
			if (field == null && owner != null && indexes.isEmpty() && s == segments.length - 1)
				break;
			JsonProperty property = field == null ? null : field.getAnnotation(JsonProperty.class);
			if (!json.isEmpty())
				json.append('.');
			json.append(property != null && !property.value().isEmpty() ? property.value() : name).append(indexes);

			type = field == null ? null : field.getGenericType();
			// Each [i] or [key] steps into a list's, array's or map's elements.
			for (int i = 0; i < indexes.length(); i++) {
				if (indexes.charAt(i) == '[')
					type = elementType(type);
			}
		}
		return json.toString();
	}

	private static Field findField(Class<?> owner, String name)
	{
		for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				// Try the superclass.
			}
		}
		return null;
	}

	private static Class<?> rawClass(Type type)
	{
		if (type instanceof Class<?> c)
			return c;
		if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> c)
			return c;
		return null;
	}

	private static Type elementType(Type type)
	{
		if (type instanceof Class<?> c && c.isArray())
			return c.getComponentType();
		if (type instanceof GenericArrayType a)
			return a.getGenericComponentType();
		if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> raw) {
			Type[] arguments = p.getActualTypeArguments();
			if (Collection.class.isAssignableFrom(raw) && arguments.length == 1)
				return arguments[0];
			if (Map.class.isAssignableFrom(raw) && arguments.length == 2)
				return arguments[1];
		}
		return null;
	}
}
