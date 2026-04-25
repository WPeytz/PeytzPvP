# Hardcore Games plugin project instructions

You are working on a Minecraft Paper plugin written in Java 21.

## Project goals
Build a modern Hardcore Games style PvP plugin inspired by old-school survival games servers.
Priorities:
1. Clean architecture
2. Fast iteration
3. Playable MVP first
4. Config-driven balancing
5. Good developer ergonomics

## Stack
- Paper plugin
- Java 21
- Gradle Kotlin DSL
- SQLite for MVP
- PostgreSQL later
- Adventure components for text output

## Architecture rules
- Keep listeners thin
- Put game logic in services
- Put persistence in storage classes
- Put arena/loot/kit definitions in config files where possible
- Avoid giant god classes
- Prefer composition over inheritance
- Use enums for phases and chest tiers
- Each service should have a single responsibility

## Main modules
- game
- arena
- loot
- kits
- player
- combat
- stats
- storage
- command
- listener

## Coding rules
- Use Java 21 features when they improve readability
- No unnecessary abstraction
- No reflection
- No NMS unless explicitly required
- Keep methods short
- Add comments only where intent is not obvious
- Ask before introducing a large dependency

## Workflow rules
When implementing a feature:
1. Explain the plan briefly
2. List files to create or edit
3. Implement in small steps
4. Keep the server playable after each step
5. Prefer incremental commits

## Current MVP roadmap
1. Game phase state machine
2. Arena loading
3. Lobby and countdown
4. Chest loot
5. Grace period
6. Elimination and spectator mode
7. Win condition
8. Match reset
9. Four starter kits
10. SQLite stats

## Definition of done
A feature is only done when:
- it compiles
- plugin loads without errors
- basic happy-path test is possible in local Paper server
- config/messages are not hardcoded unnecessarily