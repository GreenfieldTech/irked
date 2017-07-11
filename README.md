# Irked Vert.X Web Framework

Irked is a very opinionated framework for configuring Vert.X-web routing and call dispatch.

It allows you to write your REST API code without writing routing boiler plate by leveraging
annotations, auto-discovery through reflection and optionally (if you're into that as well)
dependency injection.

## Installation

In your `pom.xml` file, add the repository for Irked (we are currently not hosted
in the public Maven repository) as an element under `<project>`:

```
<repositories>
  <repository>
    <id>GreenfieldTech-oss</id>
    <url>https://packagecloud.io/GreenfieldTech/oss/maven2</url>
  </repository>
</repositories>
```

Then add Irked as a dependency:

```
<dependency>
	<groupId>tech.greenfield.vertx</groupId>
	<artifactId>irked</artifactId>
	<version>[0,)</version>
</dependency>
```

## Usage

Under Irked we use the concept of a "Controller" - a class whose fields and methods are used as
handlers for routes and that will handle incoming HTTP requests from `vertx-web`.

A "master controller" is created to define the root of the URI hierarchy - all configured routes
on that controller will be parsed relative to the root of the host.

### Simple Routing

To publish routes to the server's "Request Handler", create your controller class by extending the
irked `Controller` class, define fields or methods to handle HTTP requests and annotate them with
the relevant method annotations and URIs that those handles should receive requests for.

#### A Sample Controller

```
package com.example.api;

import tech.greenfield.vertx.irked.status.*;

class Root extends Controller {

	@Get("/")
	Handler<RoutingContext> index = r -> {
		r.response().setStatusCode(200).end("Hello World!");
	};
	
	@Post("/")
	void create(Request r) {
		// the irked Request object offers some useful helper methods over the
		// standard Vert.x RoutingContext
		r.sendError(new BadRequest("Creating resources is not yet implemented"));
	}
}
```

### Sub Controllers

Additionally, complex routing topologies can be implemented by "mounting" sub-controllers under
the main controller - by setting fields to additional `Controller` implementations and annotating
them with the `@Endpoint` annotation with the URI set to the endpoint you want your sub-controller
to be accessible under.

### A Sample Main and Sub Controllers

```
package com.example.api;

class Root extends Controller {

	@Endpoint("/blocks")
	BlockApi blocks = new BlockApi();
	
}
```

```
package com.example.api;

class BlockApi extends Controller {

	@Get("/:id")
	Handler<Request> retrieve = r -> {
		// irked supports vertx-web path parameters 
		r.sendJSON(loadBlock(r.pathParam("id")));
	};
}
```

### Request Context Re-programming

As hinted above, irked supports path parameters using Vert.X web, but 
[unlike Vert.x web's sub-router](https://github.com/vert-x3/vertx-web/blob/master/vertx-web/src/main/java/io/vertx/ext/web/impl/RouterImpl.java#L269),
irked controllers support path parameters everywhere, including as base paths for mounting sub-controllers.

As a result, a sub-controller might be interested in reading data from a path parameter defined in
a parent controller, such that the sub-controller has no control over the definition. To promote
object oriented programming and with good encapsulation, irked allows parent controllers to provide
access to parameter (and other) data by "re-programming" the routing context that is passed to
sub-controllers.

A parent controller can define path parameters and then extract the data and hand it down to local
handlers and sub-controllers through a well defined API, by overriding the `Controller.getRequest()`
method.

#### A Sample Route Context Re-programming

```
package com.example.api;

class MyRequest extend Request {

	String id;

	public MyRequest(Request req) {
		super(req);
		id = req.pathParam("id");
	}
	
	public String getId() {
		return id;
	}
	
}
```

```
package com.example.api;

import tech.greenfield.vertx.irked.status.*;

class Root extends Controller {

	@Get("/:id")
	void report(MyRequest r) {
		r.response(new OK()).end(createReport(r.getId()));
	}

	@Endpoint("/:id/blocks")
	BlockApi blocks = new BlockApi();
	
	@Override
	protected Request getRequestContext(Request req) {
		return new MyRequest(req);
	}
}
```

```
package com.example.api;

class BlockApi extends Controller {

	@Get("/")
	Handler<MyRequest> retrieve = r -> {
		r.sendJSON(getAllBlocksFor(r.getId()));
	};
}
```

### Initializing

After creating your set of `Controller` implementations, deploy them to Vert.x by setting up
a `Verticle` in the standard way, and set the HTTP request handler for the HTTP server by
asking Irked to set up the request handler.

#### Sample Vert.x HTTP Server

```
Future<HttpServer> async = Future.future();
vertx.createHttpServer()
		.requestHandler(new Irked(vertx).setupRequestHandler(new com.example.api.Root()))
		.listen(port, async);
```


### Tips

#### Mounting Middle-Ware

Under Vert.x its often useful to have a "middle-ware" that parse all your requests, for example:
the Vert.x Web `BodyHandler` implementation reads the HTTP request body and handles all kinds of
body formats for you.

This type of middle-ware can be easily used in irked by registering it on a catch all end-point,
very similar to how you set it up using the Vert.X web's `Router` implementation. In your root
controller, add a field like this:

```
@Endpoint("/*")
BodyHandler bodyHandler = BodyHandler.create();
```

This will cause all requests to first be captured by the `BodyHandler` before being passed to
other handlers.
