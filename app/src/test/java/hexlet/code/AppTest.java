package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import kong.unirest.Empty;

import static org.assertj.core.api.Assertions.assertThat;

import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }

    @Nested
    class RootTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц",
                    "Бесплатно проверяйте сайты на SEO пригодность");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testShowAll() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://example.com");
            assertThat(body).contains("https://github.com");
            assertThat(body).contains("Сайты");
        }

        @ParameterizedTest
        @CsvSource({
            "https://www.youtube.com",
            "https://www.youtube.com:8080"
        })
        void testCreateValid(String url) {
            HttpResponse<Empty> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(url);
            assertThat(body).contains("Страница успешно добавлена");

            Url newUrl = new QUrl().name.equalTo(url).findOne();

            assertThat(newUrl).isNotNull();
            assertThat(newUrl.getName()).isEqualTo(url);
            assertThat(newUrl.getId()).isEqualTo(3);

            response = Unirest
                    .get(baseUrl + "/urls/3")
                    .asString();
            body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("Сайт " + url);
        }

        @ParameterizedTest
        @CsvSource({
            "https://, Некорректный URL, /",
            "https://example.com, Страница уже существует, /urls",
        })
        void testCreateInvalid(String url, String flashMessage, String path) {
            HttpResponse<Empty> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo(path);

            HttpResponse<String> response = Unirest
                    .get(baseUrl)
                    .asString();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains(flashMessage);
        }

        @ParameterizedTest
        @CsvSource({
            "1, 200, Сайт https://example.com",
            "100, 404, Not Found",
        })
        void testShowSpecific(String id, String code, String expected) {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/" + id)
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(Integer.parseInt(code));
            assertThat(body).contains(expected);
        }
    }
}
