package com.urlshortener;

import com.urlshortener.analytics.ClickTracker;
import com.urlshortener.cache.RedisClient;
import com.urlshortener.cache.RedisUrlCache;
import com.urlshortener.config.ShortenerConfig;
import com.urlshortener.database.DatabaseModule;
import com.urlshortener.database.UrlMappingRepository;
import com.urlshortener.errors.ExceptionMappers;
import com.urlshortener.health.RedisHealthCheck;
import com.urlshortener.resources.RedirectResource;
import com.urlshortener.resources.UrlResource;
import com.urlshortener.service.AliasSuggester;
import com.urlshortener.service.ShortCodeGenerator;
import com.urlshortener.service.UrlShortenerService;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.jdbi.v3.core.Jdbi;

import java.util.Set;

/** Entry point: {@code java -jar url-shortener.jar server config.yml}. */
public class UrlShortenerApplication extends Application<UrlShortenerConfiguration> {

    static final String SWAGGER_UI_VERSION = "5.35.1";

    public static void main(String[] args) throws Exception {
        new UrlShortenerApplication().run(args);
    }

    @Override
    public String getName() {
        return "url-shortener";
    }

    @Override
    public void initialize(Bootstrap<UrlShortenerConfiguration> bootstrap) {
        // Allow ${ENV_VAR:-default} in config.yml
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

        // Swagger UI: static files from the webjar plus our index page that points at /openapi.json
        bootstrap.addBundle(new AssetsBundle(
                "/META-INF/resources/webjars/swagger-ui/" + SWAGGER_UI_VERSION, "/swagger-ui", null, "swagger-ui"));
        bootstrap.addBundle(new AssetsBundle("/swagger", "/docs", "index.html", "docs"));
    }

    @Override
    public void run(UrlShortenerConfiguration config, Environment environment) {
        // Database (connection pool, migrations, "mysql" health check)
        Jdbi jdbi = DatabaseModule.connect(config.getDatabase(), environment);
        UrlMappingRepository repository = jdbi.onDemand(UrlMappingRepository.class);

        // Cache (Redis pool tied to the app lifecycle, "redis" health check)
        RedisClient redis = new RedisClient(config.getRedis());
        environment.lifecycle().manage(redis);
        environment.healthChecks().register("redis", new RedisHealthCheck(redis));

        // Conversion service
        ShortenerConfig shortener = config.getShortener();
        UrlShortenerService service = new UrlShortenerService(
                repository,
                new RedisUrlCache(redis),
                new ShortCodeGenerator(shortener),
                new AliasSuggester(shortener.getAliasSuggestionCount()),
                ClickTracker.NO_OP);

        // HTTP API
        environment.jersey().register(new UrlResource(service));
        environment.jersey().register(new RedirectResource(service));
        ExceptionMappers.all().forEach(environment.jersey()::register);

        // OpenAPI spec at /openapi.json and /openapi.yaml
        OpenAPI openApi = new OpenAPI().info(new Info()
                .title("URL Shortener API")
                .version("0.1.0")
                .description("Shorten URLs and redirect short codes to the original URL."));
        environment.jersey().register(new OpenApiResource().openApiConfiguration(new SwaggerConfiguration()
                .openAPI(openApi)
                .prettyPrint(true)
                .resourcePackages(Set.of("com.urlshortener.resources"))));
    }
}
