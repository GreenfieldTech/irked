package tech.greenfield.vertx.irked;


import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import tech.greenfield.vertx.irked.websocket.WebSocketConnection;
import tech.greenfield.vertx.irked.websocket.WebSocketMessage;

public class WebSocketUpgradeRequestWrapper extends RequestWrapper {

	private Handler<? super WebSocketMessage> msghandler;

	public WebSocketUpgradeRequestWrapper(Handler<? super WebSocketMessage> handler, RequestWrapper parent) {
		super(parent);
		this.msghandler = handler;
	}

	@Override
	public void handle(RoutingContext r) {
		Request req = wrapper.apply(r);
		if (req.needUpgrade("websocket"))
			new WebSocketConnection(req, msghandler);
		else
			req.next();
	}

}
