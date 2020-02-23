package tech.greenfield.vertx.irked.status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;

import tech.greenfield.vertx.irked.HttpError;

public class StatusTest {

	@Test
	public void testNotFound() {
		assertThat(new NotFound(), instanceOf(HttpError.class));
		assertThat(HttpError.class.isInstance(new NotFound()), equalTo(true));
	}

	@Test
	public void testLookup() throws Exception {
		assertThat(tech.greenfield.vertx.irked.status.HttpStatuses.create(404).getStatusCode(), equalTo(404));
	}

}
