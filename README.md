verdant
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.verdant/com.io7m.verdant.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.verdant%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.io7m.verdant/com.io7m.verdant.svg?style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/verdant/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m/verdant.svg?style=flat-square)](https://codecov.io/gh/io7m/verdant)

![verdant](./src/site/resources/verdant.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m/verdant/workflows/main.linux.temurin.current.yml)](https://github.com/io7m/verdant/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m/verdant/workflows/main.linux.temurin.lts.yml)](https://github.com/io7m/verdant/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m/verdant/workflows/main.windows.temurin.current.yml)](https://github.com/io7m/verdant/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m/verdant/workflows/main.windows.temurin.lts.yml)](https://github.com/io7m/verdant/actions?query=workflow%3Amain.windows.temurin.lts)|


## Description

`verdant` is a trivial protocol for announcing a set of protocols
exposed by a set of HTTP endpoints.

A client connecting to a root endpoint reads a binary structure delivered with
the content type `application/verdant+cedarbridge`.

The protocol is specified using the [cedarbridge](https://github.com/io7m/cedarbridge)
specification language using the canonical binary encoding. Briefly: All values
are delivered in [big endian](https://en.wikipedia.org/wiki/Endianness)
byte order. Strings are encoded as a 32-bit unsigned integer representing the
number of bytes the string data requires, followed by the UTF-8 encoded bytes of
the string. Lists are encoded as a 32-bit unsigned integer representing the
number of list elements, followed by the elements of the list.

The structure consists of a sequence of messages, with the first message
being a fixed-size version header `VContainerVersion` specifying the version of
the `verdant` protocol. The one and only defined version, at the time of
writing, is version `1`.

```
[record VContainerVersion
  [field version IntegerUnsigned32]
]
```

In version `1` of the protocol, this message will immediately be followed by
a `V1ProtocolsSupported` message.

```
[record VProtocolId
  [field msb IntegerUnsigned64]
  [field lsb IntegerUnsigned64]
]

[record VProtocolSupported
  [field id VProtocolId]
  [field versionMajor IntegerUnsigned32]
  [field versionMinor IntegerUnsigned32]
  [field endpointPath String]
]

[record V1ProtocolsSupported
  [field supported [List VProtocolSupported]]
]
```

Each element of type `VProtocolSupported` specifies a protocol uniquely identified
with a [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier)
and a major and minor version. Clients can then navigate to the associated
endpoint path (relative to the current URL) and can expect to converse using
whatever protocol was specified.

