# University Projects

Selected coursework from my BA/MSc in Integrated Computer Science at Trinity College Dublin.

Both projects in this repository are distributed systems written in Java on raw UDP sockets, with every node packaged as its own Docker container and the full topology brought up through Docker Compose.

---

## Media Streaming Protocol

A publish/subscribe message broker and streaming system built from scratch on UDP.

Producers register their available streams with a central broker. Consumers subscribe to a stream by name, and the broker resolves the subscription to the producer that owns it and wires the two together. Content is then chunked and streamed to every subscribed consumer, with acknowledgements and retransmission handling loss on an unreliable transport.

**Design notes**

- Custom binary wire format with a 5 byte header carrying a message type, a stream identifier and a producer identifier
- Seven message types covering registration, subscribe, unsubscribe, stream metadata, content transfer, acknowledgement and a not-found response
- The broker holds two registries, one mapping producers to the streams they publish and one mapping consumers to their active subscriptions, so a single producer can serve many consumers
- Reliability implemented at the application layer, since UDP provides none
- An event-driven `Node` base class handles socket lifecycle and dispatches received datagrams to per-role handlers

**Topology**

Five containers on a shared Docker network: one broker, two producers and two consumers, so the fan-out and multi-stream cases are exercised rather than just a single happy path.

**Run it**

```bash
cd "Media streaming protocol"
docker compose up --build
```

Consumers run with an interactive TTY, so attach to a consumer container to subscribe to a stream.

---

## Flow Forwarding

A flow-forwarding network where routers learn paths from observed traffic instead of being given a static routing table.

Endpoints address each other by name rather than by IP. When a router receives a packet for a destination it has not seen before, it floods to discover a route, buffers the packet in the meantime, and records the return path so that later traffic to the same destination is forwarded directly.

**Design notes**

- Forwarding table implemented as a map from destination name to next hop, with a timestamp on each entry so stale routes expire and can be relearned when the topology changes
- Packets arriving for an unknown destination are buffered and replayed once a route is established, rather than dropped
- Source-address filtering prevents a router from echoing traffic back onto the interface it arrived on
- Routers discover their own interfaces at runtime, so the same image works at any position in the topology

**Topology**

Seven containers across four Docker networks: three routers and four endpoints. Each router bridges the central network and one edge network, so traffic between endpoints on different edges has to be routed rather than delivered directly. Sample traffic definitions for each router are included under `traffics/`.

**Run it**

```bash
cd "Flow forwarding"
docker compose up --build
```

---

## Notes

These were academic projects, and there are things I would do differently now. The shared protocol classes are duplicated into each node's build context rather than living in one module, the network topologies are hardcoded in Compose rather than generated, and neither project has an automated test suite. The duplication in particular is an artefact of each container needing a self-contained build context, and would be better solved with a shared library and a multi-stage build.
