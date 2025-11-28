package fi.natroutter.foxlib.config;

import fi.natroutter.foxlib.files.FileManager;
import fi.natroutter.foxlib.files.ReadResponse;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.util.function.Consumer;

public class ConfigProvider<T> {

    public static class Builder<V> {
        private String name = "config.yaml";
        private FoxLogger logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("ConfigProvider")
                .build();;
        private FileManager.Builder fileManager;

        public Builder<V> setName(String name) {
            this.name = name;
            return this;
        }
        public Builder<V> setLogger(FoxLogger logger) {
            this.logger = logger;
            return this;
        }
        public Builder<V> setFileBuilder(FileManager.Builder builder) {
            this.fileManager = builder;
            return this;
        }

        public ConfigProvider<V> build(Class<V> clazz) {
            if (!this.name.endsWith(".yaml")) {
                this.name = this.name + ".yaml";
            }
            File file = new File(this.name);

            this.fileManager = new FileManager.Builder(file)
                    .setUseAppDirectory(true)
                    .setLogger(this.logger);

            return new ConfigProvider<>(this, clazz);
        }
    }

    private T config;

    public T get() {return config;}

    @Getter
    private boolean initialized = false;

    private ConfigProvider(Builder<T> builder, Class<T> clazz) {
        Consumer<ReadResponse> resp = builder.fileManager.getOnInitialized();
        builder.fileManager.onInitialized(file -> {
            if (file.success()) {
                DumperOptions options = new DumperOptions();
                Representer representer = new Representer(options);
                representer.getPropertyUtils().setSkipMissingProperties(true);

                Constructor constructor = new Constructor(clazz, new LoaderOptions());
                Yaml yaml = new Yaml(constructor, representer);

                config = yaml.loadAs(file.content(),clazz);
                resp.accept(file);
            }
            initialized = file.success();
        });
        builder.fileManager.build();
    }

}
