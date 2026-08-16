# 007 — Encryption

**Status:** Accepted (2026-08-15)

## Context

Per `005-data-privacy.md`, server never need read workspace content. Data sensitive (menstrual cycle records, medical documents, personal notes). Requirement: must not leak to anyone outside couple — including hosting provider.

Spec (§45) demand established cryptographic libraries, forbid inventing algorithms, forbid claiming end-to-end encryption unless genuinely implemented.

## Decision

**Bouncy Castle lightweight API**, not libsodium.

libsodium Android binding (`lazysodium-android`) JNI-bound — force every crypto test onto emulator, make crypto module Android-only. Bouncy Castle pure Java, behave identically on JVM and Android, ships RFC 9180 HPKE — key wrapping follow published standard rather than hand-assembled sealed box. `:core:crypto` therefore plain Kotlin JVM module, ordinary fast unit tests.

**Record encryption.** ChaCha20-Poly1305. Each write derive fresh key with HKDF-SHA256 over random 16-byte salt — this what make all-zero nonce safe: (key, nonce) pair only repeat if same salt drawn twice. Envelope layout:

```
[version:1][salt:16][ciphertext || poly1305 tag:16]
```

Leading version byte = forward-compatibility seam — future build can change construction, still read old payloads. Associated data binds each ciphertext to its record id and version, so ciphertext cannot move between records.

**Keys.**

| Key | Purpose | Where it lives |
|---|---|---|
| Workspace key (32 B) | Encrypts every record and file | Device only, never sent unwrapped |
| Device X25519 keypair | Receives workspace key at pairing | Private half sealed by Android Keystore AES-GCM key, blob in DataStore |
| Recovery phrase | Workspace key itself, encoded | Written down by user, never stored anywhere |

Keystore cannot hold raw X25519 material usable by HPKE — hence wrap-the-private-key indirection rather than Keystore-native key.

**Pairing.** Inviting device seals workspace key to joining device's X25519 public key using HPKE base mode. Server relays opaque blob it cannot open.

**Recovery phrase.** Workspace key rendered as 24-word BIP-39 mnemonic. Phrase *is* key encoded, not passphrase unlocking stored copy — 32-byte key exactly 256 bits BIP-39 entropy, exactly 24 words. Consequence: nothing extra stored on server, no KDF parameters that could drift between app versions, no wrapped blob to lose. BIP-39 checksum makes mistyped phrase fail loudly rather than silently yield key that decrypts nothing. Verified against official BIP-39 English test vectors.

Trade-off: rotating workspace key changes recovery phrase — user must be told record new one. Accepted — rotation rare, deliberate act.

## Consequences

- No server-side search, filtering, sorting or validation. Search becomes Room FTS4 over locally decrypted data (corrected from earlier "FTS5" — Room 2.8.4 has no `@Fts5` annotation, only `@Fts3`/`@Fts4`; found while building Phase 8 search).
- No server-generated notification content. Reminders scheduled locally.
- Metadata still leaks: row counts and `updated_at` reveal *that* something logged and when, not what. Removing that needs padding and decoy traffic; out of scope, stated rather than glossed over.
- Losing both devices and recovery phrase means data unrecoverable. Inherent to design; setup flow must say so plainly.

**What may be claimed.** Content end-to-end encrypted between two devices. Account metadata, timestamps and record counts not. Documentation must say exactly that and no more.