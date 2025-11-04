# BitTorrent Client (Java)

A simplified BitTorrent client implemented in Java to explore networking, multithreading, and distributed file transfer concepts.

This repository contains two main development branches:
- **`sumedh`** — single-peer model / baseline client.  
- **`multipeer`** — multi-peer, concurrent downloader using multiple simultaneous peer connections.  

---

## Project summary

This project implements the core pieces of a BitTorrent-style client:
- `.torrent` parsing (bencode → torrent metadata)
- tracker contact (announce) and peer list retrieval
- BitTorrent handshake, bitfield, and message handling
- Piece/block request pipeline, integrity verification (SHA-1 per piece)
- Single-peer model (baseline) and multi-peer concurrent downloader (multithreaded)
- Currently extending the system with advanced algorithms for peer selection, piece prioritization, and fault tolerance.

High-level goals: learn socket programming, protocol implementation, concurrency and fault tolerance in a P2P transfer workflow. :contentReference[oaicite:2]{index=2}

---

## Branches & what they contain

- **`sumedh`** — focuses on the single-peer implementation and torrent parsing library. Good starting point to understand how a single connection and piece verification works.

- **`multipeer`** — extends the single-peer work to coordinate multiple simultaneous peer connections, piece scheduling, and concurrency primitives to improve throughput and resilience. Use this branch to study thread / connection management and basic piece re-assignment logic.

---

## How to Run

For now no Maven or JAR packaging required — just run the main Java file directly.

1. Clone the repo:
   ```bash
   git clone https://github.com/Sumedh-Chavan/bittorrent-client.git
   cd bittorrent-client
  
2. Switch to the branch you want:
    ```bash
    git checkout sumedh      # single-peer version
    # OR
    git checkout multipeer   # multi-peer version
3. Run the client:
    ```bash
    cd src
    java Application.java
