# Shared port code

`src/main/java` contains the unchanged `ThermalVisionState` record and `ThermalVisionConfig` constants from the 1.20.1 release. Both standalone Gradle builds compile them. Place other pure Java rules shared by the 1.21.1 and 26.3 ports here. Keep Minecraft, Forge, client rendering, item data components, and networking APIs in their version-specific projects.

The working Forge 1.20.1 source remains in the repository root and does not depend on this directory.
