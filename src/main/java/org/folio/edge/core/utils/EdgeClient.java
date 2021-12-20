package org.folio.edge.core.utils;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;

import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

public class EdgeClient {
  private static final Logger log = LogManager.getLogger(EdgeClient.class);

  private final TokenCache cache;
  private final WebClient client;
  private final String clientId;
  private final String okapiUrl;
  private final String tenant;
  private final String username;
  private final Supplier<Future<String>> getPasswordSupplier;

  EdgeClient(String okapiUrl, WebClient client, TokenCache cache, String tenant,
             String clientId, String username, Supplier<Future<String>> getPasswordSupplier) {
    this.cache = cache;
    this.client = client;
    this.clientId = clientId;
    this.okapiUrl = okapiUrl;
    this.tenant = tenant;
    this.username = username;
    this.getPasswordSupplier = getPasswordSupplier;
  }

  WebClient getClient() {
    return client;
  }

  Future<HttpRequest<Buffer>> getToken(HttpRequest<Buffer> request) {
    String token;
    try {
      token = cache.get(clientId, tenant, username);
    } catch (Exception e) {
      log.warn("Failed to access TokenCache {}", e.getMessage(), e);
      return Future.failedFuture("Failed to access TokenCache");
    }
    if (token != null) {
      request.putHeader(X_OKAPI_TOKEN, token);
      return Future.succeededFuture(request);
    }
    return getPasswordSupplier.get().compose(password -> {
      JsonObject payload = new JsonObject();
      payload.put("username", username);
      payload.put("password", password);
      return client.postAbs(okapiUrl + "/authn/login")
          .expect(ResponsePredicate.SC_CREATED)
          .putHeader("Accept", "*/*") // to be safe
          .putHeader(X_OKAPI_TENANT, tenant)
          .sendJsonObject(payload); // also sets Content-Type to application/json
    }).map(res -> {
      String newToken = res.getHeader(X_OKAPI_TOKEN);
      cache.put(clientId, tenant, username, newToken);
      request.putHeader(X_OKAPI_TOKEN, newToken);
      return request;
    });
  }
}
