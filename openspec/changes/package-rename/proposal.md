# Proposal

## Why

The code lives under two old roots, `de.schnorrenbergers` and `de.hems`. The owner wants one root under his own name.

## What Changes

- Every Java package under `de.schnorrenbergers` and `de.hems` moves to `com.benschnorr`, by script, with files moved by `git mv`.
- `plugin.yml` mains, the launcher's `mainClass`, `install.sh`, `admin-passwort.sh`, test probes and the Maven groupId follow.
- README, `testing/`, knowledge concepts and the current specs name the new packages and paths.
- No behavior changes, so no spec deltas (`skip_specs: true`).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None.

## Impact

All modules. Network events are Java-serialized, so the launcher and every server must run the same build; a normal update restart does that. No stored data names a class.
