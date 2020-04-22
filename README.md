# PacketLimiter
Simple plugin to prevent individual players from spamming packets

## Original

[SpottedLeaf/PacketLimiter](https://github.com/Spottedleaf/PacketLimiter)

## Why Fork

There are seemingly minor changes in this fork which are designed to maximise performance.
The changes may seem as micro-optimisation, but when players are sending many packets very quickly,
every byte matters. Less done inside the lock is better.

Also, it's built against 1.8.8 to ensure compatibility, something upstream presumably wouldn't like.
