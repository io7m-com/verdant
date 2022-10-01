/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.verdant.tests;

import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class VProtocolsMessagesTest
{
  private static byte[] resource(
    final String name)
    throws IOException
  {
    final var path =
      "/com/io7m/verdant/tests/%s".formatted(name);

    try (var stream =
           VProtocolsMessagesTest.class.getResourceAsStream(path)) {
      return stream.readAllBytes();
    }
  }

  /**
   * Version 0 isn't supported.
   *
   * @throws Exception On errors
   */

  @Test
  public void testV0Unsupported0()
    throws Exception
  {
    final var ex =
      assertThrows(VProtocolException.class, () -> {
        parse("v0ContainerUnsupported.bin");
      });

    assertEquals("error-container-unsupported", ex.errorCode());
  }

  /**
   * Version 0 isn't supported.
   *
   * @throws Exception On errors
   */

  @Test
  public void testV0Unsupported0Write()
    throws Exception
  {
    final var ms =
      VProtocolMessages.create();

    final var ex =
      assertThrows(VProtocolException.class, () -> {
        ms.serialize(new VProtocols(
          List.of(
            new VProtocolSupported(UUID.randomUUID(), 1L, 2L, "/x/y/z")
          )
        ), 0);
      });

    assertEquals("error-container-unsupported", ex.errorCode());
  }

  /**
   * Version 1 messages have a minimum length.
   *
   * @throws Exception On errors
   */

  @Test
  public void testV1ErrorTooShort()
    throws Exception
  {
    final var ex =
      assertThrows(VProtocolException.class, () -> {
        parse("v1ErrorTooShort.bin");
      });

    assertEquals("error-io", ex.errorCode());
  }

  /**
   * The sample message is passed and serialized correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public void testV1Sample0()
    throws Exception
  {
    final var ps = parse("v1Sample0.bin");

    assertEquals(1, ps.protocols().size());

    {
      final var p = ps.protocols().get(0);
      assertEquals(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-bbbb-bbbbbbbbbbbb"),
        p.id()
      );
      assertEquals(32L, p.versionMajor());
      assertEquals(23L, p.versionMinor());
      assertEquals("/api/v1/", p.endpointPath());
    }

    roundTrip(ps);
  }

  /**
   * Ordering is correct.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCompare()
    throws Exception
  {
    final var u0 =
      UUID.fromString("00000000-0000-0000-0000-000000000000");
    final var u1 =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

    final var xs = new ArrayList<VProtocolSupported>();

    final var vs7 = new VProtocolSupported(u1, 1L, 1L, "/");
    xs.add(vs7);
    final var vs6 = new VProtocolSupported(u1, 1L, 0L, "/");
    xs.add(vs6);
    final var vs5 = new VProtocolSupported(u1, 0L, 1L, "/");
    xs.add(vs5);
    final var vs4 = new VProtocolSupported(u1, 0L, 0L, "/");
    xs.add(vs4);
    final var vs3 = new VProtocolSupported(u0, 1L, 1L, "/");
    xs.add(vs3);
    final var vs2 = new VProtocolSupported(u0, 1L, 0L, "/");
    xs.add(vs2);
    final var vs1 = new VProtocolSupported(u0, 0L, 1L, "/");
    xs.add(vs1);
    final var vs0 = new VProtocolSupported(u0, 0L, 0L, "/");
    xs.add(vs0);

    xs.sort(VProtocolSupported::compareTo);

    assertEquals(vs0, xs.get(0));
    assertEquals(vs1, xs.get(1));
    assertEquals(vs2, xs.get(2));
    assertEquals(vs3, xs.get(3));
    assertEquals(vs4, xs.get(4));
    assertEquals(vs5, xs.get(5));
    assertEquals(vs6, xs.get(6));
    assertEquals(vs7, xs.get(7));
  }

  private static void roundTrip(
    final VProtocols ps)
    throws VProtocolException
  {
    final var ms =
      VProtocolMessages.create();
    final var data =
      ms.serialize(ps, 1);
    final var rs =
      ms.parse(URI.create("urn:source"), data);

    assertEquals(ps, rs);
  }

  private static VProtocols parse(
    final String name)
    throws VProtocolException, IOException
  {
    return VProtocolMessages.create()
      .parse(URI.create("urn:input"), resource(name));
  }
}


