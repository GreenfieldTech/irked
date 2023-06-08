package io.vertx.ext.web.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.vertx.ext.web.Route;
import tech.greenfield.vertx.irked.Router;

public class RouteImplHelper {

	private RouteImpl impl;

	public RouteImplHelper(Route r) {
		this.impl = (RouteImpl) r;
	}
	
	public RouteImplHelper(Router router, String path) {
		this(new RouteImpl((RouterImpl)router.vertxWebRouter(), 0, path));
	}

	public RouteState getState() {
		return impl.state();
	}
	
	public Set<String> listParameters() {
		Set<String> allIds = new HashSet<>();
		allIds.addAll(Objects.requireNonNullElseGet(impl.state().getGroups(), Collections::emptySet));
		allIds.addAll(Objects.requireNonNullElseGet(impl.state().getNamedGroupsInRegex(), Collections::emptySet));
		return allIds;
	}
}
