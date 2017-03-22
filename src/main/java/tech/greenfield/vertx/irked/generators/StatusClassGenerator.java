package tech.greenfield.vertx.irked.generators;

import java.io.*;
import java.net.URL;
import java.security.SecureRandom;
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
			new JsonArray(text).forEach(e -> {
						JsonObject o = (JsonObject)e;
						try {
							int code = Integer.parseInt(o.getString("code"));
							String phrase = o.getString("phrase");
							generateClass(code, phrase);
						} catch (NumberFormatException err) {
							// ignore "class codes"
						} catch (IOException err) {
							throw new RuntimeException(err);
						}
					});
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

	private void generateClass(int code, String phrase) throws IOException {
		String className = phrase.replaceAll("[^a-zA-Z]+", "");
		String fullyQualified = packageName + "." + className;
		new File(destDir + "/" + packageName.replace(".", "/")).mkdirs();
		File file = new File(destDir + "/" + fullyQualified.replace(".", "/") + ".java");
//		System.err.println("Generating in " + file);
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		writer.format("package %s;\n\n", packageName);
		writer.format("import tech.greenfield.vertx.irked.HttpError;\n\n");
		writer.format("public class %s extends HttpError {\n\n", className);
		writer.format("\tprivate static final long serialVersionUID = %dL;\n\n", new SecureRandom(fullyQualified.getBytes()).nextLong());
		writer.format("\tpublic %s() {\nsuper(%d,\"%s\");\n}\n\n", className, code, phrase);
		writer.format("\tpublic %s(Throwable t) {\nsuper(%d,\"%s\", t);\n}\n\n", className, code, phrase);
		writer.format("\tpublic %s(String m) {\nsuper(%d,\"%s\", m);\n}\n\n", className, code, phrase);
		writer.format("\tpublic %s(String m, Throwable t) {\nsuper(%d,\"%s\", m, t);\n}\n\n", className, code, phrase);
		writer.format("}\n");
		writer.close();
	}

	public static void main(String...args) throws IOException {
		if (args.length != 2)
			return;
		new StatusClassGenerator().generate(args[0], args[1]);
	}
}
