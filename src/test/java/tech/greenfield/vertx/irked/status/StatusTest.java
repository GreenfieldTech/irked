package tech.greenfield.vertx.irked.status;

import static org.junit.Assert.*;

import org.junit.Test;

import tech.greenfield.vertx.irked.HttpError;

public class StatusTest {

	@Test
	public void testNotFound() {
		assertTrue(HttpError.class.isInstance(new NotFound()));
		assertTrue(new NotFound() instanceof HttpError);
	}
	
	@Test
	public void testLookup() throws Exception {
		assertEquals(404, tech.greenfield.vertx.irked.status.HttpStatuses.create(404).getStatusCode());
	}

}
