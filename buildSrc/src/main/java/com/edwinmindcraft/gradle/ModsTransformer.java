package com.edwinmindcraft.gradle;

import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer;
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.file.FileTreeElement;
import shadow.org.apache.tools.zip.ZipEntry;
import shadow.org.apache.tools.zip.ZipOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@CacheableTransformer
public class ModsTransformer implements Transformer {

	private final List<String> lines = new ArrayList<>();

	@Override
	public boolean canTransformResource(FileTreeElement element) {
		return element.getRelativePath().getPathString().equalsIgnoreCase("META-INF/mods.toml");
	}

	@Override
	public void transform(TransformerContext context) {
		List<String> result = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getIs()))) {
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				result.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!this.lines.isEmpty())
			result.removeIf(x -> StringGroovyMethods.startsWithAny(x, "modLoader=", "loaderVersion=", "license=", "issueTrackerURL=", "showAsResourcePack="));
		this.lines.addAll(result);
	}

	@Override
	public boolean hasTransformedResource() {
		return !this.lines.isEmpty();
	}

	@Override
	public void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
		ZipEntry entry = new ZipEntry("META-INF/mods.toml");
		try {
			entry.setTime(TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.getTime()));
			os.putNextEntry(entry);
			os.write(this.lines.stream().reduce((x, y) -> x + "\n" + y).orElse("").getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.lines.clear();
	}

	public String getName() {
		return "mods.toml";
	}
}