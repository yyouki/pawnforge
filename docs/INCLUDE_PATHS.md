# Pawn include handling in PawnForge

PawnForge passes explicit include directories to the native Pawn compiler instead of relying on its working directory or install location.

## Recommended project layout

    MyGamemode/
      main.pwn
      gamemode/
        player.inc
        vehicles.inc
      pawno/
        include/
          a_samp.inc
      qawno/
        include/
          a_samp.inc
      include/
        project_shared.inc

## Quoted include

Use a path relative to the file containing the directive:

    // main.pwn is at the project root
    #include "gamemode/player.inc"

Quoted includes are resolved relative to the file that contains the directive. This also works for nested include files.

## Angle-bracket include

Angle-bracket includes are searched through the compiler include directories:

    #include <gamemode/player.inc>

PawnForge includes the project root as an include directory, so the path gamemode/player.inc can be found. It also includes the gamemode directory itself, so #include <player.inc> can be found there.

Default search directory order:

1. pawno/include
2. qawno/include
3. include
4. includes
5. gamemode
6. project root (.)

Pawn compiler supports repeating -i<path> for multiple include directories.

## Error and warning handling

An include is not automatically valid just because its folder exists. The filename, path, and file contents must be valid. The actual Pawn compiler must report missing files, syntax errors, duplicate definitions, and warnings. PawnForge must display these diagnostics rather than suppress them.

The current Android editor milestone stores the workspace and include-path configuration. Real compile-time validation becomes active after the native Pawn compiler is integrated.