# 005 — Data privacy: the threat model

**Status:** Accepted (2026-08-15)

## Context

`docs/specs/01-android-conversion.md` specifies privacy as a *partner-vs-partner* problem:
every item private by default, explicit opt-in sharing, granular cycle-sharing permissions,
and a test matrix (§69) asserting the husband cannot read the wife's private cycle data.

Implementing that literally requires a server that can read the data, because field-level
sharing decisions ("share period dates but not symptoms") have to be evaluated somewhere
the partner's query reaches. That rules out end-to-end encryption.

Before building it, the user corrected the requirement:

> the data between both of us will be shared, but it will have security and privacy to
> others — it's sensitive information and I don't want it to leak

## Decision

The privacy boundary is **the couple versus the outside world**, not partner versus partner.

- Both users see everything in the workspace. There is no `visibility` field, no
  `sharedWith`, no `cycle_sharing_permissions`, and no per-item private toggle anywhere.
- `owner_id` is retained purely as attribution ("created by") and is never an input to an
  authorization decision.
- The protection target is Supabase, an attacker with database access, a stolen phone, and
  anyone who is not one of the two users.

## Consequences

**Good.** With no server-side sharing logic to evaluate, the server never needs to read the
data — which makes genuine end-to-end encryption practical (see `007-encryption.md`). The
data model loses an entire dimension: no visibility checks scattered across queries, sync,
search, calendar and notifications, and therefore no class of bug where one of them forgets.

**Superseded spec sections.** §7, §12, §33–35, §40, §42, §50, §58–59 and §62 of the
conversion spec collapse to "both members, always". The §69 test matrix is replaced by the
outsider-focused acceptance tests in `docs/specs` planning notes: ciphertext-only storage,
outsider access denied, unauthenticated access denied, tamper detection, invitation
expiry/reuse/revocation, workspace member cap, and no plaintext in the local database.

**Cost.** If the users later want something hidden from each other — a surprise gift, a
private note — it is not a config change. It needs a second key per user and a re-think of
what the server stores. Recorded here so that trade-off is a conscious one.
