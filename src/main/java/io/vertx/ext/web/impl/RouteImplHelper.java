package io.vertx.ext.web.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import tech.greenfield.vertx.irked.Router;

/**
 * A helper to access the results of the Vert.x route parser, for use in Irked support for dynamic runtime parameter
 * configuration for controller methods, to circumvent Vert.x limits on access to the immutable RouteState.
 * 
 * The parser results frankly should be accessible to the application for various reasons, but instead they are
 * "secured" as "package private", which does nothing to prevent access and only serves to hinder useful implementations.
 * Furthermore, the {@code RouteState} class is immutable and public access cannot even be abused for unsupported operations,
 * so there really isn't any need to actively prevent access to it. 
 * 
 * This class is used by the Irked configuration stage (before the HTTP server is started, and before the real
 * {@code Route} Objects are created) to parse the route annotations on Controller methods, in a compatible way to the
 * Vert.x-web {@code Router}.
 * 
 * Because of the intentional limitations put on the Vert.x-web {@code Route} public API, this implementation relies on
 * specifics of the Vert.x-web internal implementation, i.e. the {@code RouteImpl} class, but it also requires "package
 * private" access to the {@code RouterImpl#state()} method. All access is read-only to an immutable value using the class
 * API. Generally, we just need the a {@code RouteState} instance that contains the parsed results, but unfortunately
 * not only does {@code RouteState} not have a public constructor - most of the parsing code whose results are presented
 * by {@code RouteState} is actually in {@code RouteImpl}.
 * 
 * @author odeda
 */
public class RouteImplHelper {

	private RouteImpl impl;

	/**
	 * Parse the specified route path for the specified Irked router, by creating a standalone {@code RouteImpl}
	 * @param router Irked router that needs this path parsed
	 * @param path route path to parse
	 */
	public RouteImplHelper(Router router, String path) {
		this.impl = new RouteImpl((RouterImpl)router.vertxWebRouter(), 0, path);
	}

	/**
	 * Expose the parsed {@link RouteState} result.
	 * This is the method that Vert.x-web {@code RouteImpl} is so protective of, and that we had to replace.
	 * @return a {@code RouteState} instance containing the result of parsing the path that was submitted.
	 */
	public RouteState getState() {
		return impl.state();
	}
	
	/**
	 * Retrieve the list of named path parameters that are specified by the submitted path
	 * @return a set of names for named path parameters
	 */
	public Set<String> listParameters() {
		Set<String> allIds = new HashSet<>();
		allIds.addAll(Objects.requireNonNullElseGet(impl.state().getGroups(), Collections::emptySet));
		allIds.addAll(Objects.requireNonNullElseGet(impl.state().getNamedGroupsInRegex(), Collections::emptySet));
		return allIds;
	}
}
