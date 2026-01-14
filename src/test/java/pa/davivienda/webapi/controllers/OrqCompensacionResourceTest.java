package pa.davivienda.webapi.controllers;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests de integración para {@link OrqCompensacionResource}.
 * 
 * <p>Usa @QuarkusTest para levantar el servidor y probar los endpoints REST.</p>
 */
@QuarkusTest
class OrqCompensacionResourceTest {

    @Test
    void testOpenAPI_Returns200() {
        given()
        .when()
            .get("/q/openapi")
        .then()
            .statusCode(200)
            .contentType(anyOf(
                containsString("application/json"),
                containsString("text/plain"),
                containsString("application/yaml")
            ));
    }

    @Test
    void testSwaggerUI_Returns200() {
        given()
        .when()
            .get("/q/swagger-ui")
        .then()
            .statusCode(anyOf(is(200), is(303))); // 200 or redirect to index.html
    }
}
