package io.github.censodev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.VALIDATE)
public class AppStructGenerateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Override
    public void execute() throws MojoFailureException {
        Path basePath = Paths.get(project.getBasedir().getPath())
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve(project.getGroupId().replace(".", File.separator))
                .resolve(project.getArtifactId().replace("-", "").toLowerCase());
        try {
            generate(basePath, loadStruct());
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }

    private List<Struct> loadStruct() throws IOException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        CollectionType typeRef = TypeFactory.defaultInstance()
                .constructCollectionType(ArrayList.class, Struct.class);
        return om.readValue(getClass().getClassLoader().getResource("struct.yaml"), typeRef);
    }

    private void generate(Path basePath, List<Struct> structs) throws IOException {
        if (structs == null)
            return;
        for (Struct struct : structs) {
            Path path = basePath.resolve(struct.getName());
            Files.createDirectories(path);
            Files.createFile(path.resolve(".gitkeep"));
            generate(path, struct.getChildren());
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Struct {
        private String name;
        private List<Struct> children;
    }
}
