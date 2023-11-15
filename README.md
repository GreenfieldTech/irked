# Irked Vert.x Web Framework 4.x

Irked is a very opinionated framework for configuring Vert.x-web routing and call dispatch.

It allows you to write your REST API code without writing routing boiler plate by leveraging
annotations, auto-discovery through reflection and optionally (if you're into that as well)
dependency injection.

This version supports Vert.x 4. To use with earlier Vert.x versions, try Irked 2 for Vert.x 3.9
support or Irked 1 for earlier versions (Vert.x 3.9 has changed its API in a non-backward compatible
way - method return types were changed - which required that 1 + 2 split).

Other than different backward compatibility, Irked versions are essentially the same with bug fixes
ported to all releases.

## Installation

Irked is available from the [Maven Central Repository](https://central.sonatype.com/artifact/tech.greenfield/irked-vertx/4.5.0).

If using Maven, add Irked as a dependency in your `pom.xml` file:

```xml
<dependency>
	<groupId>tech.greenfield</groupId>
	<artifactId>irked-vertx</artifactId>
	<version>4.5.0</version>
</dependency>
```

For other build tools, see the Maven Central website for the syntax, but it generally
boils down to just using `tech.greenfield:irked-vertx:4.5.0` as the dependency string.

## Quick Start

You may want to take a look at the example application at [`src/example/java/tech/greenfield/vertx/irked/example/App.java`](src/example/java/tech/greenfield/vertx/irked/example/App.java) which shows how to create a new Vert.x Verticle using an Irked `Router` and a few very simple APIs. Then you may want to read the rest of this document for explanations, rationale and more complex API examples.

To run the example application, after compiling (for example, using `mvn compile`) run it with your full Vert.x 4.5.0 installation:

```
vertx run -cp target/classes/ tech.greenfield.vertx.irked.example.App
```

Or, alternatively, using the Vert.x JAR dependencies in the Irked maven project:

```
mvn exec:exec -Dexec.executable=java -Dexec.args="-cp %classpath io.vertx.core.Launcher run tech.greenfield.vertx.irked.example.App"
```

## Usage

Under Irked we use the concept of a "Controller" - a class whose fields and methods are used as
handlers for routes and that will handle incoming HTTP requests from Vert.x-web.

A "master controller" is created to define the root of the URI hierarchy - all configured routes
on that controller will be parsed relative to the root of the host.

### Setup and Simple Routing

To publish routes to the server's "Request Handler", create your controller class by extending the
irked `Controller` class, define fields or methods to handle HTTP requests and annotate them with
the route matching logic that you want Irked to configure for each handler.

- Most often you'd want to just match on HTTP method and URI, for which Irked offers 
annotations of the form `@Method("/path")`, such as `@Get("/foo")`.
- You can also use the annotation `@Endpoint("/path")` to match on all methods
(this very is useful to mount sub-controllers, as detailed below).
- The path argument for the both method-specific and `@Endpoint` annotations is optional 
and omitting it will match requests for all URIs.

#### An Example Controller

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
a `Verticle` like you would do for a [Vert.x-web Router](https://vertx.io/docs/vertx-web/java/#_basic_vert_x_web_concepts),
but use Irked to create a router from your root controller - and set that as the request handler.

#### A Vert.x Web HTTP Server Example

```java
Router router = Irked.router(vertx).with(new com.example.api.Root());
vertx.createHttpServer().requestHandler(router).listen(8080);
```

### Configuration Errors

Sometimes humans make mistakes, it happens. When Irked is asked to setup your controllers
(when calling `with()` or one of the other setup methods), it scans your configuration
(the controller classes annotations) and if it detects a configuration that cannot be
executed - for example, handlers with too few or too many parameters - it will throw
an `InvalidRouteConfiguration` exception with as much details as appropriate. This
happens during the Vert.x setup stage and before the HTTP server request handler is set
up. We make every attempt to not cause exceptions to be thrown during actual request
processing, and any such generated by the Irked implementation will be consideed bugs and
will be fixed.

### Sub Controllers

Complex routing topologies can be implemented by "mounting" sub-controllers under
the main controller - by setting fields to additional `Controller` implementations and annotating
them with the `@Endpoint` annotation with the URI set to the endpoint you want your sub-controller
to be accessible under.

### Main and Sub Controllers Example

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
        // irked supports Vert.x-web path parameters 
        r.send(loadBlock(r.pathParam("id")));
    };
}
```

### Request Context Re-programming

As hinted above, irked supports path parameters using Vert.x-web, but 
[unlike Vert.x-web's sub-router](https://github.com/vert-x3/vertx-web/blob/b778f450bc4e0e928f8b6c761a376b5ab9f24151/vertx-web/src/main/java/io/vertx/ext/web/impl/RouteImpl.java#L156),
irked controllers support path parameters everywhere, including as base paths for mounting sub-controllers.

As a result, a sub-controller might be interested in reading data from a path parameter defined in
a parent controller, such that the sub-controller has no control over the definition. To promote
object oriented programming with good encapsulation, irked allows parent controllers to provide
access to parameter (and other) data by "re-programming" the routing context that is passed to
sub-controllers.

A parent controller can define path parameters and then extract the data and hand it down to local
handlers and sub-controllers through a well defined API, by overriding the
`Controller.getRequestContext()` method.

#### Route Context Re-programming Example

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.annotations.*;

class MyRequest extends Request {

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

    @Endpoint("/:id/blocks") // ":id" is read by the MyRequest class
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
        r.send(getAllBlocksFor(r.getId())); // read the identifier field defined by Root and MyRequest
    };
}
```

### Cascading Request Handling

Sometimes its useful to have the multiple handlers handle the same request - for example you
may have a REST API that supports both PUT requests to update data and GET requests on the same
URI to retrieve such data. Supposed the response to the PUT request looks identical to the response
for a GET request - it just shows how the data looks after the update - so wouldn't it be better
if the same handler that handles the GET request also handles the output for the PUT request?

For example:

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*; 

class Root extends Controller {

    @Endpoint // set up body reading middle-ware for all requests (see more below)
    BodyHandler bodyHandler = BodyHandler.create();

    @Put("/:id")
    WebHandler update = r -> {
        // start an async operation to store the new data
        store(r.pathParam("id"), r.getBodyAsJson())
        .onSuccess(v -> { // Instead of sending the response
            r.next(); // let the next handler do it
        })
        .recover(err -> { // if storing failed
            r.sendError(new InternalServerError(err)); // send an 500 error
        });
    };

    @Put("/:id")
    @Get("/:id")
    WebHandler retrieve = r -> {
        load(r.pathParam("id"))
        .compose(data -> r.send(data))
        .recover(err -> r.sendError(new InternalServerError(err)))
    };

}
```

You can, of course, pass data between handlers using the `RoutingContext`'s `put()`, `get()` and
`data()` methods as you can normally do with Vert.x-web.

This way works well when configuring routing using controller fields - Irked uses Java
reflection to read the order the fields are defined in the source code and configure
the handlers in the correct order taking, advantage of implicit Vert.x-web route order -
but this naive "definition order" doesn't work for controller methods due to limitations
of Java reflection. For explicit ordering, useful for both methods and fields, see
[Explicit handler ordering](#explicit-handler-ordering) below.

### Routing methods with dynamic parameters

When using methods for routing handling, Irked offers automatic conversion of path parameters into method parameters:
by creating handler methods that accept - in addition to the request context - a set of trivially text convertible
parameters, Irked will locate appropriately labeled path parameters, converted them to the desired Java types and provide
the values in the method invocation:

```java
@Get("/catalog/:producer/:id")
public void getCatalogItem(Request r, String producer, Integer id) {
    dao.findCatalog(producer).compose(cat -> cat.findItem(id))
            .compose(r::send)
            .recover(err -> r.sendError(new InternalServerError(err)));
}
```

Please keep in mind the following limitations:

 - Irked knows how to convert the following parameter types: `Boolean`, `String`, `Integer`, `Long`, `Float`, `Double`,
   `BigDecimal` and `Instant`. If the value cannot be parsed correctly to the required type, Irked will set that
   parameter value to `null`. In any case, the raw value can be retrieved using the regular path parameter lookup methods.
 - Primitive parameter types are not supported - i.e. `Integer` is supported but `int` is not, otherwise Irked cannot
   report parse failures. If an unsupported parameter type is found during configuration, it is an error and Irked will
   throw an `InvalidRouteConfiguration` exception. Regardless, Irked will not cause exceptions to be thrown during
   actual request processing.
 - Handlers can be annotated with multiple annotations with different paths and path parameters (it is not recommended,
   but is supported), and as such the parameters can match to any path parameter on any path annotation set on the method.
   Irked will pass `null` for any parameter that can't be matched on the current active route. If during configuration
   Irked cannot locate the requested parameter in any of the configured path annotations, it is an error and Irked will
   throw an `InvalidRouteConfiguration` exception.
 - Irked has two main strategies for matching parameters - either by matching the parameter name as reported by Java
   reflection, or by matching a parameter annotation called `Name` or `Named`. Irked is not bound to a specific annotation
   implementation, it will use whatever annotation type you are already using, or you can use Irked own
   `tech.greenfield.vertx.irked.annotations.Name`. The parameter name strategy only works if the Java class was compiled
   with debug symbols including parameter names - you can do that by adding the `-parameters` option to the `javac`
   command line, or - if using Maven's Java compiler plugin, by setting its configuration to
   `<configuration><parameters>true</parameters></configuration>`.

When building without parameter name debug symbols, the above example would need to have the method declared
like so:

```java
@Get("/catalog/:producer/:id")
public void getCatalogItem(Request r, @Name("producer") String producer, @Name("id") Integer id) {
```

### Handle Failures

It is often useful to move failure handling away from the request handler - to keep the code clean
and unify error handling which is often very repetitive. Irked supports 
[Vert.x-web's error handling](http://vertx.io/docs/vertx-web/js/#_error_handling) using the `@OnFail` annotation that you can assign to a request handler, which makes it so that the handler is only called for requests for which `fail()` has been called. 

Note: the request failure handler still needs to be configured properly for a URI and HTTP method. 
We often find it useful to use the default (no path) `@Endpoint` annotation to configure a default
failure handler (this is equivalent to using the Vert.x-web `Router.route()` API to create an any-method/any-path
handler) - though multiple and specific failure handlers would work fine.

#### A Failure Handler Example

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*; 

class Root extends Controller {

    @Get("/foo")
    WebHandler fooAPI = r -> {
        loadFoo().compose(r::send).onFailure(r::fail);
    };

    @Post("/foo")
    WebHandler fooAPI = r -> {
        r.fail(new UnsupportedOperationException("Not implemented yet"));
    };

    @OnFail
    @Endpoint // catch all failures that haven't been dealt with yet
    void failureHandler(Request r) {
        r.sendError(new InternalServerError(r.failure()));
    }
}
```

Irked `Request.sendError()` works with HTTP status codes classes and will create an HTTP response with the appropriate error status and an `application/json` body with a JSON object containing the fields "`status`" set to `false` and "`message`" set to the exception's detail message. The response's content can be further controlled by instead using one of the `Request.sendJSON()` or `Request.sendContent()` methods that take an `HttpError` parameter.

Also see the tips section below for a more complex failure handler that may be useful.

#### Status Code Specific and Exception Specific Failure Handlers

The `@OnFail` annotation allows customizing the failure handler to only handle specific HTTP status codes (when propagated using the `RoutingContext.fail(int)` or `RoutingContext.fail(int, Throwable)` methods) or only handle specific exceptions (when propagated using the `RoutingContext.fail(int, Throwable)` or `RoutingContext.fail(Throwable)`). This allows for creating smaller failure handlers, that - like `catch` clauses - only handle a specific exception or status.

##### A Specific Exception Failure Handler Example

```java
class Root extends Controller {
    @Post("/foo")
    WebHandler fooAPI = r -> {
        if (database == null)
            throw new ConfigurationException("Database is not ready");
        database.store(r.body().asJsonObject());
    };

    @OnFail(exception = ConfigurationException.class)
    @Post("/foo")
    WebHandler fooDecodeError = r -> r.sendContent("Please wait until the database is ready", new BadRequest());

    @OnFail(exception = DataAccessException.class)
    @Post("/foo")
    WebHandler fooDecodeError = r -> r.sendContent("Error storing data: " + r.failure().getMessage(), new InternalServerError());
}
```

It is also possible to extract the caught exception instance in the handler using Irked
`Request.findFailure()` method:

```java
@OnFail(exception = DataAccessException.class)
@Endpoint
WebHandler blobDaoError = r -> r.sendContent("Failed to insert blob into " +
        r.findFailure(DataAccessException.class).getColumn(), new BadRequest());
```

Another useful way to get access to the caught failure is by using a method with dynamic parameters (see
[Routing methods with dynamic parameters](#routing-methods-with-dynamic-parameters) above):

```java
@OnFail(exception = DataAccessException.class)
@Endpoint
public blobDaoError(Request r, DataAccessException err) {
    r.sendContent("Failed to insert blob into " + err.getColumn(), new BadRequest());
}
```

The exception type can be any of the exception types specified for the `@OnFail(exception)` annotations on the method - there can
be more than one and there can be more exception parameters, or you can request a super type of several registered
`@OnFail(exception)` annotations - Irked will deliver the handled exception to the correct parameter and send `null` to all
other exception type parameters, if the method has more than one. It is a configuration error to request a dynamic
parameter with an exception type that is not a registered `@OnFail(exception)` or a super class of one - and Irked
will throw an `InvalidRouteConfiguration` exception during router setup, if this situation occurs.

### Request Content-Type Specific Handlers

Irked supports the Vert.x-web `consumes()` filter to specify request handlers
that only handle specific request content-types, using the `@Consumes`
annotation. Specify it like URI annotations with the expected request content type. Like the `consumes()` method, it supports wild cards.

#### A Request Content-Type Handler Example

```java
@Post("/upload")
@Consumes("multipart/form-data")
WebHandler fileUpload = r -> {
    for (FileUpload f : r.fileUploads()) saveFile(f);
    r.send("Uploaded!");
};
```

### Async Processing

Irked contains a few helpers for using Promise-style APIs, such as Vert.x `Promise`/`Future` or Java 8's
`CompletableFuture`, to asynchronously process requests using friendly method references, most notably
`Request.send()` to send responses and `Request.handleFailure` to forward "exceptional completion"
Exceptions to the failure handlers, or `Request.next()` which makes it easier to attach `RoutingContext.next()`
to `Future.onSuccess()` handlers.

Please note that helper methods that can fail will return a `Future` (for example, `Request.send()` uses
`io.vertx.core.json` to encode JSON responses and that can call into user code that can fail). These should be used
in a Vert.x Future chain, in a `Future.compose()` step, so that if there was a failure - it will becaught by a downstream
error handler. For example:

```java
@Get("/")
WebHandler getSomething = r -> loadSomething()
    .compose(r::send)
    .onFailure(r::handleFailure);
```

In this case, `Request.send()` may return a failed future (if `loadSomething()` loaded an object that cannot be safely
serialized to JSON) that will be handled by `Request.handleFailure()`. On the other hand, `Reqeuest.handleFailure()`
does not return a `Future` as it cannot fail (or at least, it internally calls `RoutingContext.fail()` to handle the
failure, and it can also do that to handle its own internal failures).

#### An Asynchronous Processing Example

```java
Future<List<PojoType>> loadSomeRecords() {
    // … access a database asynchronously to load some POJOs
}

@Get("/")
WebHandler catalog = r -> // we don't even need curly braces
    loadSomeRecords() // fetch records
    .map(l -> l.stream() // stream loaded records as POJOs
            .map(JsonObject::mapFrom) // bean map each record to a JSON object
            .collect(JsonArray::new, JsonArray::add, JsonArray::addAll)) // collect everything to a JSON array
    .compose(r::send) // send the list to the client
    .onFailure(r::handleFailure); // capture any exceptions and forward to the failure handler
```

You can review the Irked unit test [`TestAsyncSending.java`](src/test/java/tech/greenfield/vertx/irked/TestAsyncSending.java) for more examples.

### WebSockets and SockJS

There are several implementation strategies to handle WebSockets under Irked controllers. Irked offers an "opinionated" API that gets out of the way of the developer and allows them to leverage other Irked facilities, such as cascading requests and custom request contexts - as detailed below.

In the case that the developer would prefer lower level access to the WebSocket upgrade protocol, or use the more complex Sock.JS implementation offered by Vert.x-web - this document includes examples on how to achieve both.

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

If you are interested in a [Sock.JS](http://sockjs.org) server implementation, Vert.x-web offers `SockJSHandler` that
can be mounted directly in an Irked controller as any other Vert.x middle-ware (see below for more about middle-ware):

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

Under Vert.x its often useful to have a "middle-ware" that processes requests before passing
control back to your application, such as the Vert.x-web [`BodyHandler`](https://vertx.io/docs/apidocs/io/vertx/ext/web/handler/BodyHandler.html)
that reads the HTTP request body and handles all kinds of body formats for you, the
[`LoggerHandler`](https://vertx.io/docs/apidocs/io/vertx/ext/web/handler/LoggerHandler.html)
that automatically logs web requests to an Apache style log,
the [`CorsHandler`](https://vertx.io/docs/apidocs/io/vertx/ext/web/handler/CorsHandler.html)
that lets you easily configure [CORS](https://fetch.spec.whatwg.org/#http-cors-protocol) rules,
[and many others](https://vertx.io/docs/apidocs/io/vertx/ext/web/handler/package-summary.html).

This type of middle-ware can be easily used in irked by registering it on a catch all end-point,
very similar to how you set it up using the Vert.x-web's `Router` implementation. In your root
controller, add a field - at the top of the class definition - like this:

```java
@Endpoint // match all methods and all paths
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
handling and built-in Vert.x-web failure handling functionality to make this a breeze:

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
@Endpoint
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
@Endpoint
WebHandler failureHandler = Request.failureHandler();
```

##### "OK" Exceptions

By the way, it is possible to use the throwable `HttpError` types to `throw` any kind of HTTP status,
including a "200 OK", like this: `throw new OK().unchecked()`.

##### Declare Thrown Exceptions

If your controllers uses method handlers, and you prefer not to use "unchecked" `HttpError`s, you can also declare thrown exceptions generally (declare throwing `HttpError`) or specific errors:

```java
@Get("/:id")
void getTheStuff(MyRequest r) throws Unauthorized {
    if (!r.authorizedToGetStuff())
        throw new Unauthorized("You are not authorized to get our stuff");
    r.send(r.theStuff());
}
```

Irked will gladly setup such method handlers and will forward any exceptions they throw to the failure handlers.

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

#### Explicit handler ordering

As discussed above, while Irked supports both using fields to route requests to, as well as
methods, when annotating methods to handle incoming requests - due to the limitations
of Java reflection, Irked cannot identify the correct order to setup the Vert.x-web
routes. You can use the Irked `@Order` annotation to specify an explicit order for
handler methods (Using the [Route.order() method](https://vertx.io/docs/vertx-web/java/#_route_order))
so that Irked can guarantee that when you call `RoutingContext.next()`, the correct handler
will be called:

```java
package com.example.api;

import tech.greenfield.vertx.irked.*;
import tech.greenfield.vertx.irked.status.*;
import tech.greenfield.vertx.irked.annotations.*;

class Example extends Controller {

    @Order(1000) // make sure sending the response is the last thing that happens
    public void helloWorld(Request r) {
        r.sendContent(r.get("message"), new OK());
    }

    public void thisComesFirst(Request r) {
        r.put("message","Hello World!");
        r.next();
    }
}
```
