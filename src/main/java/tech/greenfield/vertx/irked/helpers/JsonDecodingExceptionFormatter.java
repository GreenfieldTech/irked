package tech.greenfield.vertx.irked.helpers;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.vertx.core.json.DecodeException;

public class JsonDecodingExceptionFormatter {
	
	public static String formatFriendlyErrorMessage(DecodeException e) {
		var cause = e.getCause();
		if (cause instanceof UnrecognizedPropertyException)
			return formatUnrecognizedPropertyMessage((UnrecognizedPropertyException)cause);
		if (cause instanceof JsonMappingException)
			return formatJsonMappingMessage((JsonMappingException)cause);
		return "Unexpected JSON decoding problem: " + e.getMessage();
	}

	public static String formatJsonMappingMessage(JsonMappingException e) {
		return String.format("%s %s", e.getOriginalMessage(), describeLocation(e.getPath(), e.getLocation()));

	}

	public static String formatUnrecognizedPropertyMessage(UnrecognizedPropertyException e) {
		return String.format("Unrecognized request property: '%s' %s", e.getPropertyName(), describeLocation(e.getPath(), e.getLocation()));
	}
	
	public static String describeLocation(List<Reference> path, JsonLocation loc) {
		if (loc == null)
			return String.format("[path: %s]", describePath(path));
		return String.format("[path: %s, %s]", describePath(path), loc.offsetDescription());
	}

	public static String describePath(List<Reference> path) {
		return "." + path.subList(0, path.size() - 1).stream()
		.map(r -> r.getFieldName())
		.collect(Collectors.joining("."));
	}
}
