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
import org.apache.maven.plugin.MojoExecutionException;
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
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path basePath = Paths.get(project.getBasedir().getPath())
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve(project.getGroupId().replace("\\.", File.separator))
                .resolve(project.getArtifactId().replace("-", "").toLowerCase());
        try {
            generate(basePath, loadStruct());
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoFailureException(e);
        }
    }

    private List<Struct> loadStruct() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        CollectionType typeRef = TypeFactory.defaultInstance()
                .constructCollectionType(ArrayList.class, Struct.class);
        return om.readValue(classLoader.getResource("struct.yaml"), typeRef);
    }

    private void generate(Path basePath, List<Struct> structs) {
        structs.forEach(struct -> {
            Path path = basePath.resolve(struct.getName());
            try {
                Files.createDirectory(path);
                Files.createFile(path.resolve(".gitkeep"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (struct.getChildren() != null || !struct.getChildren().isEmpty()) {
                generate(path, struct.getChildren());
            }
        });
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
