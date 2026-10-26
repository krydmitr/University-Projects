# University Projects

Selected coursework from my BA/MSc in Integrated Computer Science at Trinity College Dublin.

Both projects in this repository are distributed systems written in Java on raw UDP sockets, with every node packaged as its own Docker container and the full topology brought up through Docker Compose. Traffic was captured with tcpdump and analysed in Wireshark during testing.

---

## Media Streaming Protocol

A publish/subscribe message broker and streaming system built from scratch on UDP.

Producers register their available streams with a central broker. Consumers subscribe to a stream, and the broker matches the subscription to the producer that owns it. The producer then sends the stream as a sequence of frames, with matching audio, which the broker forwards to every subscribed consumer. A stream starts when its first consumer subscribes and pauses when the last one unsubscribes.

**Design notes**

- Custom binary wire format with a 5-byte header: a message type, a 3-byte producer identifier and a stream number
- Seven message types covering registration, subscribe, unsubscribe, stream metadata, content transfer, acknowledgement and a not-found response
- The broker keeps two registries, one of the streams each producer publishes and one of each consumer's active subscriptions, so a single producer can serve many consumers
- Acknowledgements are relayed from consumers back to the producer through the broker
- An event-driven `Node` base class handles the socket lifecycle and passes received datagrams to per-role handlers

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

Endpoints address each other by a 4-byte ID rather than by IP address. Each router records the source of the traffic it sees. When a packet arrives for a destination the router doesn't know yet, it holds the packet and broadcasts a path request onto its other networks. The request spreads until it reaches a router that knows the destination, or the destination endpoint itself. The acknowledgement then travels back along the path, so every router on the way records the route, and the held packets are forwarded. Later traffic to the same destination is forwarded directly.

When an endpoint leaves, it sends a removal request. Its router deletes the entry and broadcasts the request so every other router removes it too.

**Design notes**

- 9-byte header: a packet type, a 4-byte source ID and a 4-byte destination ID
- Forwarding table implemented as a hash map from endpoint ID to the address it was last seen at, with a timestamp recorded for each entry
- Packets for an unknown destination are buffered and replayed once a path is established, rather than dropped
- Path requests are not broadcast back onto the network they arrived from
- Routers discover their own network interfaces at runtime, so the same code works at any position in the topology

**Topology**

Seven containers across four Docker networks: three routers and four endpoints. Each router connects the central network to one edge network, so traffic between endpoints on different networks has to be routed rather than delivered directly. Sample traffic definitions are included under `traffics/`.

**Run it**

```bash
cd "Flow forwarding"
docker compose up --build
```

---

## Notes

These were academic projects, and there are things I would do differently now:

- **Reliability:** the streaming protocol acknowledges data but does not retransmit lost packets, so it is not reliable over a lossy network.
- **Path selection:** there is no shortest-path logic. The first router to answer a path request wins, and loops in the topology are not handled.
- **Bootstrapping:** each endpoint is given its first router's address rather than discovering it.
- **Route expiry:** forwarding-table entries carry a timestamp, but nothing uses it yet. Routes are only removed when an endpoint explicitly asks.
- **Broker lookups:** the broker stores registrations in lists, so lookups slow down as producers and consumers grow. A hash map would fix this, as the flow-forwarding router already does.
- **Duplicated code:** the shared protocol classes are copied into each node's build context, because each container needs a self-contained build. A shared library with a multi-stage build would be cleaner.
- **Hardcoded topologies:** the network topologies are written directly in Compose rather than generated.
- **No tests:** neither project has an automated test suite.
