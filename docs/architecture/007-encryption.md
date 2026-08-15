# 007 — Encryption

**Status:** Accepted (2026-08-15)

## Context

Per `005-data-privacy.md`, the server never needs to read workspace content. The data is
sensitive (menstrual cycle records, medical documents, personal notes), and the requirement
is that it must not leak to anyone outside the couple — including the hosting provider.

The spec (§45) demands established cryptographic libraries, forbids inventing algorithms,
and forbids claiming end-to-end encryption unless it is genuinely implemented.

## Decision

**Bouncy Castle lightweight API**, not libsodium.

libsodium's Android binding (`lazysodium-android`) is JNI-bound, which would force every
crypto test onto an emulator and make the crypto module Android-only. Bouncy Castle is pure
Java, behaves identically on the JVM and on Android, and ships RFC 9180 HPKE — so key
wrapping follows a published standard rather than a hand-assembled sealed box. `:core:crypto`
is therefore a plain Kotlin JVM module with ordinary, fast unit tests.

**Record encryption.** ChaCha20-Poly1305. Each write derives a fresh key with
HKDF-SHA256 over a random 16-byte salt, which is what makes the all-zero nonce safe: the
(key, nonce) pair can only repeat if the same salt is drawn twice. Envelope layout:

```
[version:1][salt:16][ciphertext || poly1305 tag:16]
```

The leading version byte is the forward-compatibility seam — a future build can change the
construction and still read old payloads. Associated data binds each ciphertext to its
record id and version, so a ciphertext cannot be moved between records.

**Keys.**

| Key | Purpose | Where it lives |
|---|---|---|
| Workspace key (32 B) | Encrypts every record and file | Device only, never sent unwrapped |
| Device X25519 keypair | Receives the workspace key at pairing | Private half sealed by an Android Keystore AES-GCM key, blob in DataStore |
| Recovery phrase | Last-resort unwrap of the workspace key | Written down by the user, never stored |

The Keystore cannot hold raw X25519 material usable by HPKE, hence the wrap-the-private-key
indirection rather than a Keystore-native key.

**Pairing.** The inviting device seals the workspace key to the joining device's X25519
public key using HPKE base mode. The server relays an opaque blob it cannot open.

## Consequences

- No server-side search, filtering, sorting or validation. Search becomes Room FTS5 over
  locally decrypted data.
- No server-generated notification content. Reminders are scheduled locally.
- Metadata still leaks: row counts and `updated_at` reveal *that* something was logged and
  when, not what. Removing that needs padding and decoy traffic; out of scope, and stated
  rather than glossed over.
- Losing both devices and the recovery phrase means the data is unrecoverable. Inherent to
  the design; the setup flow must say so plainly.

**What may be claimed.** Content is end-to-end encrypted between the two devices.
Account metadata, timestamps and record counts are not. Documentation must say exactly
that and no more.
