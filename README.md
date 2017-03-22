# Irked Vert.X Web Framework

Irked is an opinionated framework for configuring Vert.X-web routing and call dispatch.

It allows you to write your REST API code without writing routing boiler plate by leveraging
annotations, auto-discovery through reflection and optionally (if you're into that as well)
dependency injection.

## Installation

In your `pom.xml` file, add the repository for Irked (we are currently not hosted
in the public Maven repository) as an element under `<project>`:

```
<repositories>
	<repository>
		<id>cloudonix-dist</id>
		<url>http://cloudonix-dist.s3-website-us-west-1.amazonaws.com/maven2/releases</url>
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

Under Irked we use the concept of a "Controller" - a class whose fields are used as handlers
for routes and that will handle requests. A "master controller" is then selected to define the
root of the URI heirarchy - this Controller doesn't need to actually define support for the "`/`"
route, but all annotated routes on that controller will be parsed relative to the root of the
host.

The master controller can then define handlers as fields of the type `io.vertx.core.Handler`
that will receive requests from clients, and annotate them with the specific URIs and methods
these handlers should handle. Alternatively, the master controll can define fields of type
`Controller` which will then be configured as sub-controllers mounted under the URIs they
are annotated with.

After creating your set of `Controller` implementations, deploy them to Vert.x by setting up
a `Verticle` in the standard way, and set the HTTP request handler for the HTTP server by
asking Irked to set up the request handler.

Example:

```
Future<HttpServer> async = Future.future();
vertx.createHttpServer()
		.requestHandler(new Irked(vertx).setupRequestHandler(rootController))
		.listen(port, async);
```

### Sample Controller

```
class com.example.api.Root extends Controller {

	@Get("/")
	Handler<Request> index = r -> {
		r.response().setStatusCode(200).end("Hello World!");
	};
}
```
