package org.folio.edge.core.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

@RunWith(VertxUnitRunner.class)
public class EdgeClientTest {
  private static final String tenant = "testlib";

  private MockOkapi mockOkapi;
  private WebClient webClient;
  private int okapiPort;
  private TokenCache tokenCache = TokenCache.initialize(100, 100, 10);

  @Before
  public void setUp(TestContext context) {
    okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new MockOkapi(okapiPort, knownTenants);
    mockOkapi.start().onComplete(context.asyncAssertSuccess(res ->
      webClient = WebClient.create(mockOkapi.vertx)
    ));
 }

  @After
  public void after(TestContext context) {
    webClient.close();
    mockOkapi.close(context);
  }

  @Test
  public void testEcho(TestContext context) {
    String okapiUrl = "http://localhost:" + okapiPort;
    EdgeClient edgeClient = new EdgeClient(okapiUrl, webClient, TokenCache.getInstance(), tenant, "tclient",
      "admin", () -> Future.succeededFuture("password"));

    edgeClient.getToken(webClient.getAbs(okapiUrl + "/echo")
        .putHeader(X_OKAPI_TENANT, tenant))
      .compose(x -> x.send())
      .compose(res -> {
        context.assertEquals(200, res.statusCode());
        return edgeClient.getToken(webClient.getAbs(okapiUrl + "/echo")
            .putHeader(X_OKAPI_TENANT, tenant)
            .putHeader(X_OKAPI_TOKEN, MockOkapi.MOCK_TOKEN)
          )
          .compose(x -> x.send()).compose(r2 -> {
            context.assertEquals(200, r2.statusCode());
            return Future.succeededFuture();
          });
      });
  }
}
