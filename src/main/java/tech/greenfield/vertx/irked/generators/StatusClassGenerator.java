package tech.greenfield.vertx.irked.generators;

import java.io.*;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Generator for the HTTP exceptions, because I can't be bothered to write them
 * manually.
 * 
 * @author odeda
 */
public class StatusClassGenerator {

	private String packageName;
	private String destDir;
	
	public void generate(String destDir, String destinationPackage) throws IOException {
		this.packageName = destinationPackage;
		this.destDir = destDir;
		String text = loadDefinisions();
		try {
			generateMapClass(new JsonArray(text).stream().map(e -> {
						JsonObject o = (JsonObject)e;
						try {
							int code = Integer.parseInt(o.getString("code"));
							String phrase = o.getString("phrase");
							return generateClass(code, phrase);
						} catch (NumberFormatException err) {
							return null; // ignore "class codes"
						} catch (IOException err) {
							throw new RuntimeException(err);
						}
					})
			.filter(e -> Objects.nonNull(e)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
			System.err.println("Generated HTTP status codes classes");
		} catch (io.vertx.core.json.DecodeException e) {
			int line = ((JsonParseException)e.getCause()).getLocation().getLineNr();
			System.err.println("Error in source: " + text.split("\n")[line]);
			throw e;
		}
	}
	
	private String loadDefinisions() throws IOException {
		URL definitions = new URL("https://raw.githubusercontent.com/for-GET/know-your-http-well/master/json/status-codes.json");
		return new BufferedReader(new InputStreamReader(definitions.openStream())).lines().collect(Collectors.joining("\n"));
	}

	private void generateMapClass(Map<Integer, String> statusClasses) throws IOException {
		String className = "HttpStatuses";
		String fullyQualified = packageName + "." + className;
		new File(destDir + "/" + packageName.replace(".", "/")).mkdirs();
		File file = new File(destDir + "/" + fullyQualified.replace(".", "/") + ".java");
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		writer.format("package %s;\n\n", packageName);
		for (String imp : new String[] {
				"java.util.TreeMap", "java.util.Map",
				"tech.greenfield.vertx.irked.HttpError"

		}) {
			writer.format("import %s;\n", imp);
		}
		writer.format("public class %s {\n\n", className);
		writer.format("public static Map<Integer, Class<? extends HttpError>> HTTP_STATUS_CODES = new TreeMap<Integer, Class<? extends HttpError>>();\n");
		writer.format("static {\n");
		statusClasses.forEach((code, errorclass) -> {
			writer.format("HTTP_STATUS_CODES.put(%d,%s.class);\n", code, errorclass);
		});
		writer.format("};\n");
		writer.format("public static HttpError create(int statusCode) throws InstantiationException, IllegalAccessException {\n");
		writer.format("return HTTP_STATUS_CODES.get(statusCode).newInstance();\n");
		writer.format("}\n");
		writer.format("}\n");
		writer.close();
	}
	
	private Map.Entry<Integer, String> generateClass(int code, String phrase) throws IOException {
		String className = phrase.replaceAll("[^a-zA-Z]+", "");
		String fullyQualified = packageName + "." + className;
		new File(destDir + "/" + packageName.replace(".", "/")).mkdirs();
		File file = new File(destDir + "/" + fullyQualified.replace(".", "/") + ".java");
//		System.err.println("Generating in " + file);
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		writer.format("package %s;\n\n", packageName);
		writer.format("import tech.greenfield.vertx.irked.HttpError;\n\n");
		writer.format("public class %s extends HttpError {\n\n", className);
		writer.format("private static final long serialVersionUID = %dL;\n\n", new SecureRandom(fullyQualified.getBytes()).nextLong());
		writer.format("public static final int code = %d;\n\n", code);
		writer.format("public %s() {\nsuper(%d,\"%s\");\n}\n\n", className, code, phrase);
		writer.format("public %s(Throwable t) {\nsuper(%d,\"%s\", t);\n}\n\n", className, code, phrase);
		writer.format("public %s(String m) {\nsuper(%d,\"%s\", m);\n}\n\n", className, code, phrase);
		writer.format("public %s(String m, Throwable t) {\nsuper(%d,\"%s\", m, t);\n}\n\n", className, code, phrase);
		writer.format("}\n");
		writer.close();
		return new Map.Entry<Integer, String>() {
			@Override
			public Integer getKey() {
				return code;
			}

			@Override
			public String getValue() {
				return fullyQualified;
			}

			@Override
			public String setValue(String value) {
				return getValue();
			}
		};
	}

	public static void main(String...args) throws IOException {
		if (args.length != 2)
			return;
		new StatusClassGenerator().generate(args[0], args[1]);
	}
}
