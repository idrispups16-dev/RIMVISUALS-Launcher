# RIMVISUALS 0.4.6 — helpers, waypoints, mutual friends, Graphite

## Controls

- Open/close menu: the configured menu key (Right Shift by default), Escape or X.
- Utilities > HW/FT Settings > Binds opens a filtered Keybind Manager for that helper.
- HW and FT each expose Use Trapka, Use Plast and Use custom item actions. Configure
  the corresponding item-name fragments in that helper. Each action requires the
  enabled helper on its matching host. It uses one matching hotbar item and restores
  the selected slot; no item is used if a match is missing. These actions are unbound
  initially. The common Keybinds page exposes them too.
- `.way name X Y Z` creates a permanent personal coordinate marker for the current
  world and dimension. The command is handled locally, not sent to the game server.
- N (rebindable as Friends / Share marker) raycasts a block up to 128 blocks away
  and sends an explicit, 60-second marker to accepted account friends.

## Friends

Both people need this version and an active site-confirmed RIMVISUALS mod session.
Open Friends, copy your account code, exchange codes, send a request and have the
recipient accept it. Adding a local nickname in old versions was not a friendship;
the old local data file remains intact but does not grant sharing permissions.

Friends can see the RIMVISUALS online status and [RV] next to the vanilla name label
of an online friend in the same server scope/dimension. It identifies the mutual
RIM account's reported in-game UUID, not ownership verified by Microsoft OAuth.

Coordinates are sent only on explicit ping, never as continuous player tracking.
Status and received pings can be disabled in Friends; name badges and an optional
server room channel are configured in Visuals > Friends & Markers. On networks
with separate anarchy instances, use the same channel on the same instance.
The default scope uses the exact server address, dimension and room. Different
aliases or ports may produce distinct scopes.

Personal and shared markers are shown as depth-tested beacons within 512 blocks
and as a distance list in Local Markers HUD. Shared markers expire; personal
markers can be deleted from Markers. At most 32 beacons and 12 HUD rows are drawn.

## Appearance

Graphite is the new black/gray theme. Legacy theme indices remain valid. Existing
CFGs retain their values; installation selects Graphite in the user's current CFG.
The menu fades/slides/scales on opening and closing. Reduced Motion removes the
animation. Launcher accent imports do not override an explicitly selected Graphite.

## Service and validation

`social-schema.sql` is deployed to project pixqnuuvpxxxwaxjntpl. Tables deny direct
anon/authenticated access via RLS and revoked privileges; the RPC validates an opaque
mod session, current account access, reciprocal acceptance, scope, payload bounds,
rate limits and expiration. No service key is included in the client.

Database tests used three temporary accounts inside a rolled-back transaction:
request/accept, sender cannot self-approve, unrelated-account isolation, server and
dimension isolation, and removal revocation. No test users remained. Public RPC
access without a valid token returns unauthorized.

Client validation includes parser/item matching, full configuration round trips,
gray palette, complete build, mixin audit, helper/N binding registration, and startup
screens. Actual use of custom server items and two real game clients together still
requires in-world verification; startup is not an end-to-end multiplayer test.

The website's public mod download remains closed.
