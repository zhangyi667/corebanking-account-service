# Account Service

Owns Account CRUD. An Account is a financial identity — a currency-scoped
position with a status. Every posting leg references an `accountId`; this
service is the authority on which accountIds exist and are open for postings.

Balance tracking and posting application are **not** in this service today.
They belong to a future money-mutating consumer of `posting.transaction.received`.

## Language

**Account**: A financial identity distinct from the User who owns it. Identified by an opaque string (not a UUID — callers pick the id). Carries a `currency`, a `status`, and an `ownerId` back-reference.
_Avoid_: Wallet, ledger, position, subledger

**Owner Id**: Opaque back-reference to the party that owns the account. Managed by a separate `user-service` (not yet built). Account Service treats it as a string; does not validate.
_Avoid_: Customer id, user id, holder id

**Status**: One of `ACTIVE`, `FROZEN`, `CLOSED`. Only `ACTIVE` accounts accept new postings. `FROZEN` accepts reads but rejects mutations. `CLOSED` is terminal.
_Avoid_: State, lifecycle stage

**Account Check**: Bulk existence + validity lookup. Given a list of `accountId`s, returns which are known (with their currency + status) and which are missing. Used by posting-manager before publishing a transaction event.
_Avoid_: Account validation, account lookup

## Deferred

- Balance state (`available`, `ledger_balance`, `hold`)
- Consumption of `posting.transaction.received` → apply postings
- Emission of `account.balance.changed`
- User / owner CRUD (separate service)
- Authentication + authorization
