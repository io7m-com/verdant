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


package com.io7m.verdant.core.cb;

import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBSerializationContextType;
import com.io7m.cedarbridge.runtime.bssio.CBSerializationContextBSSIO;
import com.io7m.idstore.protocol.versions.cb.V1ProtocolsSupported;
import com.io7m.idstore.protocol.versions.cb.VContainerVersion;
import com.io7m.idstore.protocol.versions.cb.VProtocolId;
import com.io7m.jbssio.api.BSSReaderProviderType;
import com.io7m.jbssio.api.BSSWriterProviderType;
import com.io7m.verdant.core.VProtocolException;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.UUID;

import static com.io7m.cedarbridge.runtime.api.CBCore.string;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned32;
import static com.io7m.cedarbridge.runtime.api.CBCore.unsigned64;

/**
 * Functions to parse version protocol messages.
 */

public final class VProtocolMessages
{
  private final BSSReaderProviderType readers;
  private final BSSWriterProviderType writers;

  private VProtocolMessages(
    final BSSReaderProviderType inReaders,
    final BSSWriterProviderType inWriters)
  {
    this.readers =
      Objects.requireNonNull(inReaders, "readers");
    this.writers =
      Objects.requireNonNull(inWriters, "writers");
  }

  /**
   * Create a new version parser.
   *
   * @param inReaders The bssio readers.
   * @param inWriters The bssio writers
   *
   * @return A new version parser
   */

  public static VProtocolMessages create(
    final BSSReaderProviderType inReaders,
    final BSSWriterProviderType inWriters)
  {
    return new VProtocolMessages(inReaders, inWriters);
  }

  /**
   * Create a new version parser.
   *
   * @return A new version parser
   */

  public static VProtocolMessages create()
  {
    return create(
      ServiceLoader.load(BSSReaderProviderType.class)
        .findFirst()
        .orElseThrow(() -> {
          return noAvailableService(BSSReaderProviderType.class);
        }),
      ServiceLoader.load(BSSWriterProviderType.class)
        .findFirst()
        .orElseThrow(() -> {
          return noAvailableService(BSSWriterProviderType.class);
        })
    );
  }

  private static IllegalStateException noAvailableService(
    final Class<?> clazz)
  {
    return new IllegalStateException(
      "No services available of type %s"
        .formatted(clazz)
    );
  }

  private static void serializeV1Supported(
    final CBSerializationContextType context,
    final VProtocols protocols)
    throws IOException
  {
    V1ProtocolsSupported.serialize(
      context,
      new V1ProtocolsSupported(
        new CBList<>(
          protocols.protocols()
            .stream()
            .map(VProtocolMessages::serializeSupported)
            .toList()
        )
      )
    );
  }

  private static com.io7m.idstore.protocol.versions.cb.VProtocolSupported
  serializeSupported(
    final VProtocolSupported v)
  {
    return new com.io7m.idstore.protocol.versions.cb.VProtocolSupported(
      serializeId(v.id()),
      unsigned32(v.versionMajor()),
      unsigned32(v.versionMinor()),
      string(v.endpointPath())
    );
  }

  private static VProtocolId serializeId(final UUID id)
  {
    return new VProtocolId(
      unsigned64(id.getMostSignificantBits()),
      unsigned64(id.getLeastSignificantBits())
    );
  }

  private static VProtocols convertSupported(
    final V1ProtocolsSupported supported)
  {
    final var input =
      supported.fieldSupported().values();
    final var versions =
      new ArrayList<VProtocolSupported>(input.size());
    for (final var version : input) {
      versions.add(convertVersion(version));
    }
    return new VProtocols(versions);
  }

  private static VProtocolSupported convertVersion(
    final com.io7m.idstore.protocol.versions.cb.VProtocolSupported version)
  {
    return new VProtocolSupported(
      convertUUID(version.fieldId()),
      version.fieldVersionMajor().value(),
      version.fieldVersionMinor().value(),
      version.fieldEndpointPath().value()
    );
  }

  private static UUID convertUUID(
    final VProtocolId fieldId)
  {
    return new UUID(
      fieldId.fieldMsb().value(),
      fieldId.fieldLsb().value()
    );
  }

  /**
   * Serialize the given protocols.
   *
   * @param protocols The protocols
   * @param version   The container version
   *
   * @return The serialized bytes
   *
   * @throws VProtocolException On errors
   */

  public byte[] serialize(
    final VProtocols protocols,
    final int version)
    throws VProtocolException
  {
    Objects.requireNonNull(protocols, "protocols");

    if (version != 1) {
      throw new VProtocolException(
        "error-container-unsupported",
        String.format(
          "Unsupported version container %s",
          Integer.toUnsignedString(version))
      );
    }

    return this.serializeV1(protocols);
  }

  private byte[] serializeV1(
    final VProtocols protocols)
    throws VProtocolException
  {
    try (var output = new ByteArrayOutputStream()) {
      final var context =
        CBSerializationContextBSSIO.createFromOutputStream(
          this.writers, output);

      VContainerVersion.serialize(
        context,
        new VContainerVersion(unsigned32(1))
      );

      serializeV1Supported(context, protocols);
      output.flush();
      return output.toByteArray();
    } catch (final IOException e) {
      throw new VProtocolException("error-io", e.getMessage(), e);
    }
  }

  /**
   * Parse versions from the given source.
   *
   * @param source The source
   * @param data   The read bytes
   *
   * @return A set of parsed versions
   *
   * @throws VProtocolException If the input cannot be parsed
   */

  public VProtocols parse(
    final URI source,
    final byte[] data)
    throws VProtocolException
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(data, "data");

    try {
      final var context =
        CBSerializationContextBSSIO.createFromByteArray(this.readers, data);
      final var container =
        VContainerVersion.deserialize(context);

      final var version = container.fieldVersion().value();
      if (version != 1L) {
        throw new VProtocolException(
          "error-container-unsupported",
          String.format(
            "Unsupported version container %s",
            Long.toUnsignedString(version))
        );
      }

      final var supported =
        V1ProtocolsSupported.deserialize(context);

      return convertSupported(supported);
    } catch (final IOException e) {
      throw new VProtocolException("error-io", e.getMessage(), e);
    }
  }
}
