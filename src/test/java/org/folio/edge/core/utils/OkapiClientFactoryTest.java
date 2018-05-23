package org.folio.edge.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;

public class OkapiClientFactoryTest {

  private static final long reqTimeout = 5000L;

  private OkapiClientFactory ocf;

  @Before
  public void setUp() throws Exception {

    Vertx vertx = Vertx.vertx();
    ocf = new OkapiClientFactory(vertx, "http://mocked.okapi:9130", reqTimeout);
  }

  @Test
  public void testGetOkapiClient() {
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
    assertEquals(reqTimeout, client.reqTimeout);
  }

}
