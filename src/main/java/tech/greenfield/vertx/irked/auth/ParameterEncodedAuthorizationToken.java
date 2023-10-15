package tech.greenfield.vertx.irked.auth;

import java.util.*;
import java.util.Map.Entry;

/**
 * Base implementation for authentication schemes where the value is a white-space separated list
 * of parameters in the format <code>key=value</code> or <code>key="long value"</code>
 * @author odeda
 */
public class ParameterEncodedAuthorizationToken extends AuthorizationToken {

	Map<String,String> parameters = new HashMap<>();
	
	/* (non-Javadoc)
	 * @see tech.greenfield.vertx.irked.auth.AuthorizationToken#update(java.lang.String, java.lang.String)
	 */
	@Override
	protected AuthorizationToken update(String type, String token) {
		super.update(type, token);
		for (Map.Entry<String,String> e : parseParameters(token)) {
			parameters.put(e.getKey(), e.getValue());
		}
		return this;
	}
	
	/**
	 * Utility method to parse a list of parameters
	 * @param text parameters text
	 * @return a list of parsed parameters
	 */
	public static Iterable<Map.Entry<String,String>> parseParameters(String text) {
		StringTokenizer t = new StringTokenizer(text, " \t\r\n,");
		return new Iterable<Map.Entry<String,String>>() {
			@Override
			public Iterator<Entry<String, String>> iterator() {
				return new Iterator<Map.Entry<String,String>>() {
					
					@Override
					public Entry<String, String> next() {
						String next = t.nextToken();
						String[] parts = next.split("=",2);
						if (parts.length != 2)
							return new AbstractMap.SimpleImmutableEntry<String,String>(parts[0], null);
						while (parts[1].startsWith("\"") && !parts[1].endsWith("\"") && t.hasMoreTokens())
							parts[1] += " " + t.nextToken();
						if (parts[1].startsWith("\""))
							parts[1] = parts[1].substring(1, parts[1].length()-1);
						return new AbstractMap.SimpleImmutableEntry<String,String>(parts[0], parts[1]);
					}
					
					@Override
					public boolean hasNext() {
						return t.hasMoreTokens();
					}
				};
			}
		};
	}

	/**
	 * Retrieve a specific parameter's value
	 * @param param name of the parameter to retrieve
	 * @return parameter value, if available, {@code null} otherwise.
	 */
	public String getParameter(String param) {
		return parameters.get(param);
	}
	
	/**
	 * Utility method to encode binary data in hexadecimal text encoding
	 * @param data binary data to encode
	 * @return a hexadecimal string representation of the binary data
	 */
	public static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		for(byte b: data)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}
	
}
