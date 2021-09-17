# Irked Vert.X Web Framework 4.x

Irked is a very opinionated framework for configuring Vert.X-web routing and call dispatch.

It allows you to write your REST API code without writing routing boiler plate by leveraging
annotations, auto-discovery through reflection and optionally (if you're into that as well)
dependency injection.

This version supports Vert.X 4. To use with earlier Vert.X versions, try Irked 2 for Vert.x 3.9
support or Irked 1 for earlier versions (Vert.X 3.9 has changed its API in a non-backward compatible
way - method return types were changed - which required that 1 + 2 split).

Other than different backward compatibility, Irked versions are essentially the same with bug fixes
ported to all releases.

## Installation

In your `pom.xml` file, add Irked as a dependency:

```xml
<dependency>
	<groupId>tech.greenfield</groupId>
	<artifactId>irked-vertx</artifactId>
	<version>4.1.4</version>
</dependency>
```

## Quick Start

You may want to take a look at the example application at [`src/example/java/tech/greenfield/vertx/irked/example/App.java`](src/example/java/tech/greenfield/vertx/irked/example/App.java) which shows how to create a new Vert.x Verticle using an Irked `Router` and a few very simple APIs. Then you may want to read the rest of this document for explanations, rationale and more complex API examples.

To run the example application, after compiling (for example, using `mvn compile`) run it with your full Vert.x 4.1.4 installation:

```
vertx run -cp target/classes/ tech.greenfield.vertx.irked.example.App
```

Or, alternatively, using the Vert.x JAR dependencies in the Irked maven project:

```
mvn exec:exec -Dexec.executable=java -Dexec.args="-cp %classpath io.vertx.core.Launcher run tech.greenfield.vertx.irked.example.App"
```

## Usage

Under Irked we use the concept of a "Controller" - a class whose fields and methods are used as
handlers for routes and that will handle incoming HTTP requests from `vertx-web`.

A "master controller" is created to define the root of the URI hierarchy - all configured routes
on that controller will be parsed relative to the root of the host.

### Setup and Simple Routing

To publish routes to the server's "Request Handler", create your controller class by extending the
irked `Controller` class, define fields or methods to handle HTTP requests and annotate them with
the relevant method annotations and URIs that those handlers should receive requests for.

#### A Sample Controller

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*;

class Root extends Controller {

	@Get("/")
	Handler<RoutingContext> index = r -> {
		// classic Vert.x RoutingContext usage
		r.response().setStatusCode(200).end("Hello World!");
	};
	
	@Post("/")
	void create(Request r) {
		// the irked Request object offers some useful helper methods over the
		// standard Vert.x RoutingContext
		r.send(new BadRequest("Creating resources is not yet implemented"));
	}
}
```

#### Initializing

After creating your set of `Controller` implementations, deploy them to Vert.x by setting up
a `Verticle` like you would do for a [`vertx-web` Router](https://vertx.io/docs/vertx-web/java/#_basic_vert_x_web_concepts),
but use Irked to create a router from your root controller - and set that as the request handler.

#### Sample Vert.x Web HTTP Server

```java
Router router = Irked.router(vertx).with(new com.example.api.Root());
vertx.createHttpServer().requestHandler(router).listen(8080);
```

### Sub Controllers

Complex routing topologies can be implemented by "mounting" sub-controllers under
the main controller - by setting fields to additional `Controller` implementations and annotating
them with the `@Endpoint` annotation with the URI set to the endpoint you want your sub-controller
to be accessible under.

### A Sample Main and Sub Controllers

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.annotations.*;

class Root extends Controller {

	@Endpoint("/blocks") // the block API handles all requests that start with "/blocks/"
	BlockApi blocks = new BlockApi();
	
}
```

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.annotations.*;

class BlockApi extends Controller {

	@Get("/:id") // handle requests like "GET /blocks/identifier"
	Handler<Request> retrieve = r -> {
		// irked supports vertx-web path parameters 
		r.send(loadBlock(r.pathParam("id")));
	};
}
```

### Request Context Re-programming

As hinted above, irked supports path parameters using Vert.X web, but 
[unlike Vert.x web's sub-router](https://github.com/vert-x3/vertx-web/blob/b778f450bc4e0e928f8b6c761a376b5ab9f24151/vertx-web/src/main/java/io/vertx/ext/web/impl/RouteImpl.java#L156),
irked controllers support path parameters everywhere, including as base paths for mounting sub-controllers.

As a result, a sub-controller might be interested in reading data from a path parameter defined in
a parent controller, such that the sub-controller has no control over the definition. To promote
object oriented programming and with good encapsulation, irked allows parent controllers to provide
access to parameter (and other) data by "re-programming" the routing context that is passed to
sub-controllers.

A parent controller can define path parameters and then extract the data and hand it down to local
handlers and sub-controllers through a well defined API, by overriding the
`Controller.getRequestContext()` method.

#### A Sample Route Context Re-programming

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.annotations.*;

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

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*; 

class Root extends Controller {

	@Get("/:id")
	void report(MyRequest r) {
		r.response(new OK()).end(createReport(r.getId()));
	}

	@Endpoint("/:id/blocks")
	BlockApi blocks = new BlockApi();
	
	@Override
	protected MyRequest getRequestContext(Request req) {
		return new MyRequest(req);
	}
}
```

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.annotations.*;

class BlockApi extends Controller {

	@Get("/")
	Handler<MyRequest> retrieve = r -> {
		r.send(getAllBlocksFor(r.getId()));
	};
}
```

### Cascading Request Handling

Sometimes its useful to have the multiple handlers handle the same request - for example you
may have a REST API that supports both PUT requests to update data and GET requests on the same
URI to retrieve such data. Supposed the response to the PUT request looks identical to the response
for a GET request - it just shows how the data looks after the update - so wouldn't it be better
if the same handler that handles the GET request also handles the output for the PUT request?

This is not an irked feature, but irked allows you to use all the power of Vert.X web, though there
is a small "gotcha" here that we should note - the order of the handlers definition is important,
and is the order they will be called:

#### A Sample Cascading Request Handlers

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*; 

class Root extends Controller {

	@Endpoint("/*")
	BodyHandler bodyHandler = BodyHandler.create();

	@Put("/:id")
	WebHandler update = r -> {
		// start an async operation to store the new data
		store(r.pathParam("id"), r.getBodyAsJson())
		.onSuccess(v -> { // Instead of sending the response
			r.next(); // let the next handler do it
		})
		.onFailure(err -> { // if storing failed
			r.sendError(new InternalServerError(err)); // send an 500 error
		});
	};
	
	@Put("/:id")
	@Get("/:id")
	WebHandler retrieve = r -> {
		load(r.pathParam("id"))
		.onSuccess(data -> r.send(data))
		.onFailure(err -> r.sendError(new InternalServerError(err)))
	};

}
```

You can of course pass data between handlers using the `RoutingContext`'s `put()`, `get()` and
`data()` methods as you do normally in Vert.x.

**Important note**: request cascading only works when defining handlers as handler _fields_. Using
methods is not supported because the JVM reflection API doesn't keep the order of methods, while it
does keep the order for fields. Trying to cascade between methods will execute handlers in undefined order.

### Handle Failures

It is often useful to move failure handling away from the request handler - to keep the code clean
and unify error handling which is often very repetitive. Irked supports 
[Vert.X web's error handling](http://vertx.io/docs/vertx-web/js/#_error_handling) using the `@OnFail`
annotation that you can assign a request handler.

Note: the request failure handler still needs to be configured properly for a URI and HTTP method -
so we often find it useful to use the catch all `@Endpoint` annotation with a wild card URI to
configure a main failure handler, though multiple and specific failure handlers would work fine.

#### A Failure Handler Sample

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*; 

class Root extends Controller {

	@Get("/foo")
	WebHandler fooAPI = r -> {
		loadFoo().onSuccess(r::send).onFailure(r::fail);
	};

	@Post("/foo")
	WebHandler fooAPI = r -> {
		r.fail(new UnsupportedOperationException("Not implemented yet"));
	};

	@OnFail
	@Endpoint("/*")
	void failureHandler(Request r) {
		r.sendError(new InternalServerError(r.failure()));
	}
}
```

Irked's `Request.sendError()` works with HTTP status codes classes and will create an HTTP response with the appropriate error status and an `application/json` body with a JSON object containing the fields "`status`" set to `false` and "`message`" set to the exception's detail message. The response's content can be further controlled by instead using one of the `Request.sendJSON()` or `Request.sendContent()` methods that take an `HttpError` parameter.

Also see the tips section below for a more complex failure handler that may be useful.

### Request Content-Type Specific Handlers

Irked supports the Vert.x Web `consumes()` filter to specify request handlers
that only handle specific request content-types, using the `@Consumes`
annotation. Specify it like URI annotations with the expected request content type. Like the `consumes()` method, it supports wild cards.

#### A Request Content-Type Handler Sample

```java
@Post("/upload")
@Consumes("multipart/form-data")
WebHandler fileUpload = r -> {
	for (FileUpload f : r.fileUploads()) saveFile(f);
	r.send("Uploaded!");
};
```

### Async Processing

Irked contains a few helpers for using Java 8's Promise API (`CompletableFuture`) to asynchronously process
requests, most notable `Request.send()` to send responses and `Request.handleFailure` to forward
"exceptional completion" Exceptions to the failure handlers.

#### An Async Processing Sample

```java
CompletableFuture<List<PojoType>> loadSomeRecords() {
	// ... access a database asynchronously to load some POJOs
}

@Get("/")
WebHandler catalog = r -> // we don't even need curly braces
	loadSomeRecords() // fetch records
	.thenApply(l -> l.stream() // stream loaded records as POJOs
			.map(JsonObject::mapFrom) // bean map each record to a JSON object
			.collect(JsonArray::new, JsonArray::add, JsonArray::addAll)) // collect everything to a JSON array
	.thenAccept(r::send) // send the list to the client
	.exceptionally(r::handleFailure); // capture any exceptions and forward to the failure handler
```

You can review the Irked unit test [`TestAsyncSending.java`](src/test/java/tech/greenfield/vertx/irked/TestAsyncSending.java) for more examples.

### WebSockets and SockJS

There are several implementation strategies to handle WebSockets under Irked controllers. Irked offers an "opinionated" API that gets out of the way of the developer and allows them to leverage other Irked facilities, such as cascading requests and custom request contexts - as detailed below.

In the case that the developer would prefer lower level access to the WebSocket upgrade protocol, or use the more complex Sock.JS implementation offered by Vert.x Web - this document includes examples on how to achieve both.

#### Handle individual WebSocket messages

By adding the `@WebSocket(path)` annotation to a handler, the handler will be called once for each message received on a WebSocket connected on the specified path. The WebSocket message handler will receive a `WebSocketMessage` instance provided by Irked.

Instances of this class implement all the method from the `Buffer` class to access the message content, a set of `reply()` method to easily send responses to the received message as well as access to both the original `ServerWebSocket` as well as the original `Request` objects that handle this message - these can be used to track state and communicate information from the WebSocket handshake process
(for example as generated by a `getRequestContext()` implementation).

##### Example

This example shows how an incoming WebSocket request can be checked for authorization before handing the request to the WebSocket handler, using cascading requests. The `WebHandler` will be called once for each WebSocket handshake. It can also add data to the request using Vert.x `put()` facility that can be accessed from the `MessageHandler` using `WebSocketMessage.request()`.

```java

@Get("/websocket")
WebHandler authIncoming = r -> { // called once for each new WebSocket connection
  if (!isAuthorized(r.request().getHeader("Authorization"))) r.fail(new Unauthorized());
  else r.next();
};

@WebSocket("/websocket")
MessageHandler handler = m -> { // called for each client message sent in a connection
  m.reply("Hi " + m.request().session().get("name") + ", you said: " + m.toString());
};
```

When using the method handlers, you can also ask for a specific `Request` implementation, to take advantage of typed request contexts, as generated by an appropriate `getRequestContext()` implementation. A custom request context can maintain application state such as the current user.

```java
@WebSocket("/websocket")
void messageHandler(UserContextRequest req, WebSocketMessage msg) {
  msg.reply(req.doUserInteraction(msg));
}
```

#### SockJS service

If you are interested in a [Sock.JS](http://sockjs.org) server implementation, Vert.x Web offers `SockJSHandler` that
can be mounted directly in an Irked controller as any other Vert.x middle-ware:

```java
@Get("/listener")
SockJSHandler listener = SockJSHandler.create(vertx).socketHandler(sock -> {
	sock.handler(buf -> {
		sock.write("You said: " + buf.toString().trim() + "\n");
	});
});
```

#### Vert.x Core WebSocket API

There are two main implementations for WebSocket handling under Vert.x Core, as documented in the [WebSocket section of the core manual](http://vertx.io/docs/vertx-core/java/#_websockets):

1. Adding a handler for the server's `websocketStream()`: Irked doesn't support this method directly but
you can always add the handler when initializing the server, before or after setting up Irked. Please note that using 
this method blocks any other WebSocket implementation strategy suggested in this document - the WebSocket Stream handler
will consume all incoming connection upgrade requests regardless of the path these target.
2. Upgrading a request: this can be done in an Irked controller and Irked offers some minimal support to help you
manage the process, with the `Request.needUpgrade()` method to help you detect upgrade requests. 

The following example shows how to handle the WebSocket upgrade request and register event handlers to the created Vert.x WebSocket implementation class.
As you can see there is quite a bit of heavy lifting and you need to work harder to maintain context and state, compared to Irked `WebSocketMessage` API
and programmable request contexts.

```java

@Get("/websocket")
WebHandler websocketStart = r -> {
  if (!r.needUpgrade("websocket"))
    throw new BadRequest("This URL only accepts WebSocket connections").unchecked();
  r.request().toWebSocket()
    .onFailure(t -> r.fail(new InternalServerError("Failed to upgrade to WebSocket")))
    .onSuccess(ws -> {
      ws.binaryMessageHandler(msg -> handleBinaryMessage(ws, msg));
      ws.textMessageHandler(msg -> handleTextMessage(ws, msg));
      ws.exceptionHandler(err -> handleErrors(ws, err));
    });
};

private handleBinaryMessage(ServerWebSocket ws, Buffer message) {
  // ...
}

private handleTextMessage(ServerWebSocket ws, String message) {
  // ...
}

private handleErrors(ServerWebSocket ws, Throwable error) {
  // ...
}
```

### Tips

#### Mounting Middle-Ware

Under Vert.x its often useful to have a "middle-ware" that parses all your requests, for example:
the [Vert.x Web BodyHandler](https://github.com/vert-x3/vertx-examples/blob/master/web-examples/src/main/java/io/vertx/example/web/rest/SimpleREST.java#L50) implementation reads the HTTP request body and handles all kinds of body formats for you.

This type of middle-ware can be easily used in irked by registering it on a catch all end-point,
very similar to how you set it up using the Vert.x web's `Router` implementation. In your root
controller, add a field like this:

```java
@Endpoint("/*")
BodyHandler bodyHandler = BodyHandler.create();
```

This will cause all requests to first be captured by the `BodyHandler` before being passed to
other handlers.

#### Easily Pass Business Logic Errors To Clients

Sometimes it is necessary for the REST API to actually generate error responses to communicate
erroneous conditions to the client - such as missing authentication, invalid input, etc - using
the HTTP semantics by returning a response with some standard non-OK HTTP status.

In such cases, instead of hand crafting a response and doing a collection of `if...else`s to
make sure processing doesn't continue, irked allows you to take advantage of Java exception 
handling and built-in Vert.x web failure handling functionality to make this a breeze:

For example a handler might want to to signal that the expected content isn't there by returning
a 404 Not Found error:

```java
@Get("/:id")
Handler<Request> retrieve = r -> {
	if (!existsItem(r.pathParam("id")))
		throw new NotFound("No such item!").unchecked();
	r.send(load(r.pathParam("id")));
}
```

The `HttpError.unchecked()` method wraps the business logic's HTTP status exception with a
`RuntimeException` so it can jump out of lambdas and other non-declaring code easily without
boiler-plate exception handling. The Irked request handling glue will pick up the exception and 
deliver it to an appropriate `@OnFail` handler.

Then the `@OnFail` handler can be configured to automatically forward this status to the client:

```java
@OnFail
@Endpoint("/*")
Handler<Request> failureHandler = r -> {
	r.sendError(HttpError.toHttpError(r));
};
```

The `HttpError.toHttpError()` helper method detects `RuntimeException`s and unwrap their content
automatically. If the underlying cause is an `HttpError`, it will deliver it to the client using
`Request.sendError()`, otherwise it will create an `InternalServerError` exception (HTTP status 500)
that contains the unexpected exception, which will also be reported using `Request.sendError()`.

Because this use case is so useful, there's even a short-hand for this:

```
@OnFail
@Endpoint("/*")
WebHandler failureHandler = Request.failureHandler();
```

##### "OK" Exceptions

By the way, it is possible to use the throwable `HttpError` types to `throw` any kind of HTTP status,
including a "200 OK", like this: `throw new OK().unchecked()`.

#### Specify Custom Headers

Sometimes a response needs to include custom headers, for example setting the `Location` header for
a "302 Found" response. Irked `Request` objects offer the same support as the standard `RoutingContext`
Vert.x implementation it derives from using the regular `r.response().putHeader(name,value)` method,
but in addition - to augment the ability of deep business logic execution to quickly return results by
throwing an `HttpError` type - instead of sending a `Request` or `HttpServerResponse` instance deep
into business logic code, your business logic can attach the needed headers to the `HttpError` status
object they throw. 

For example:

```java
throw new Found().addHeader("Location", "https://example.com");
```

Because of the usefulness of such patterns, Irked supplies the helpers `Redirect` and `PermanentRedirect`
that implement adding `Location` headers to `Found` and `MovedPermanently` respectively, so the above
example is equivalent to:

```java
throw new Redirect("https://example.com");
```

#### Using classic Java methods for handlers, while keeping handler order

As discussed above, while Irked supports both using fields to route requests too as well as
methods, when annotating methods to handle incoming requests, the order of registration in the
Vert.x Web router is not guaranteed - and as a result the `Request.next()` calls may not go
where you expected them to.

If you still want to order your requests logically (which is useful, for example, as detailed
under "Cascading Request Handling"), but you really want to write your complex business logic
using classic Java methods, it is simple to separate the logic and the registration order, in
a similar fashion to how it can be done with the Vert.X Web Router, except using Irked
annotations. A simple example might look like this:

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*;

class Sample extends Controller {
	
	public void helloWorld(Request r) {
		r.sendContent(r.get("message"), new OK());
	}
	
	public void thisComesFirst(Request r) {
		r.put("message","Hello World!");
		r.next();
	}
	
	@Get("/") WebHandler messageMiddleware = this::thisComesFirst;
	@Get("/") WebHandler helloHandler = this::helloWorld;
}
```

