import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class GetCurrentConversionRatesTests extends BaseCase {

    private static final String ERROR_MESSAGE_INVALID_CURRENCY = "invalid Currency Codes";
    private static Response response;

    private static final String ERROR_MESSAGE_INVALID_CREDS = "Invalid authentication credentials";
    private static final String ERROR_MESSAGE_NO_CREDS = "No API key found in request";
    private static final String DEFAULT_SOURCE = "USD";
    private static final String ERROR_MESSAGE_INVALID_SOURCE = "invalid Source Currency";
    private final String URL = properties.getProperty("base_url") + properties.getProperty("live_url");

    private void basicFieldsCheck(Response response) {
        response.then().statusCode(200);
        response.then().assertThat().body("success", Matchers.equalTo(true));
        response.then().assertThat().body("timestamp", notNullValue());
        checkTimestamp(response);
    }

    private void checkTimestamp(Response response) {
        String expected = LocalDate.now().toString();
        Integer actualMs = response.path("timestamp");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date2 = new Date((long) actualMs * 1000);
        String actual = format.format(date2.getTime());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void exceededRequestsApiKey() {
        response = given()
                .queryParam("apikey", properties.getProperty("exceeded_requests_apikey"))
                .get(URL);
        response.then().statusCode(429);
        response.then().body("message", containsString("You have exceeded your daily/monthly API rate limit"));
    }

    @Test
    public void withoutApiKey() {
        response = given().get(URL);
        response.then().statusCode(401);
        response.then().body("message", containsString(ERROR_MESSAGE_NO_CREDS));
    }

    @Test
    public void withIncorrectApiKey() {
        response = given()
                .queryParam("apikey", "QuDPaIT8HjOGelzpvDXl0ZCwq87opwR5")
                .get(URL);
        response.then().statusCode(401);
        response.then().body("message", containsString(ERROR_MESSAGE_INVALID_CREDS));
    }

    @Test
    public void withApiKeyWithoutExtraParameters() {
        response = given()
                .queryParam("apikey", API_KEY)
                .get(URL);
        basicFieldsCheck(response);
        response.then().assertThat().body("source", Matchers.equalTo(DEFAULT_SOURCE));
        response.then().assertThat().body("quotes.USDCAD", notNullValue());
        response.then().assertThat().body("quotes.USDEUR", notNullValue());
        response.then().assertThat().body("quotes.USDRUB", notNullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"all", "AMD", "aRs"})
    public void withApiKeyWithValidSource(String source) {
        response = given()
                .queryParam("apikey", API_KEY)
                .queryParam("source", source)
                .get(URL);
        response.then().assertThat().body("source", Matchers.equalTo(source.toUpperCase()));
        basicFieldsCheck(response);
        response.then().assertThat().body("quotes." + source.toUpperCase() + "CAD", notNullValue());
        response.then().assertThat().body("quotes." + source.toUpperCase() + "EUR", notNullValue());
        response.then().assertThat().body("quotes." + source.toUpperCase() + "RUB", notNullValue());
    }

    @Test
    public void withApiKeyWithInvalidSource() {
        response = given()
                .queryParam("apikey", API_KEY)
                .queryParam("source", "WERW")
                .get(URL);
        response.then().statusCode(200);
        response.then().assertThat().body("success", Matchers.equalTo(false));
        response.then().assertThat().body("error", notNullValue());
        response.then().assertThat().body("error.info", containsString(ERROR_MESSAGE_INVALID_SOURCE));
        response.then().assertThat().body("error.code", Matchers.equalTo(201));
    }


    @ValueSource(strings = {"CAD", "cad,RUB,EuR", "CAD,RUB,WER"})
    @ParameterizedTest
    public void withApiKeyWithValidCurrencies(String currencies) {
        response = given()
                .queryParam("apikey", API_KEY)
                .queryParam("currencies", currencies)
                .get(URL);
        basicFieldsCheck(response);
        response.then().assertThat().body("source", Matchers.equalTo(DEFAULT_SOURCE));
        List<String> currenciesParams = List.of(currencies.split(","));
        for (String currency : currenciesParams) {
            if (!currency.equals("WER")) {
                response.then().assertThat().body("quotes.USD" + currency.toUpperCase(), notNullValue());
            }
        }
        response.then().assertThat().body("quotes.USDWER", nullValue());
    }

    @ValueSource(strings = {"ASD", "CADE,RUSB,WER"})
    @ParameterizedTest
    public void withApiKeyWithInvalidCurrencies(String currencies) {
        response = given()
                .queryParam("apikey", API_KEY)
                .queryParam("currencies", currencies)
                .get(URL);
        response.then().statusCode(200);
        response.then().assertThat().body("success", Matchers.equalTo(false));
        response.then().assertThat().body("error", notNullValue());
        response.then().assertThat().body("error.info", containsString(ERROR_MESSAGE_INVALID_CURRENCY));
        response.then().assertThat().body("error.code", Matchers.equalTo(202));
    }

    @Test
    public void withApiKeyWithValidSourceAndCurrencies() {
        String source = "EUR";
        String currencies = "CAD,RUB,USD";
        response = given()
                .queryParam("apikey", API_KEY)
                .queryParam("currencies", currencies)
                .queryParam("source", source)
                .get(URL);
        basicFieldsCheck(response);
        response.then().assertThat().body("source", equalTo(source));
        List<String> currenciesList = List.of(currencies.split(","));
        for (String currency : currenciesList) {
            response.then().assertThat().body("quotes." + source + currency, notNullValue());
        }
    }
}
