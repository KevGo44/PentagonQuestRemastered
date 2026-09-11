# Runtime models

`characters/hero.glb`, `goblin.glb`, `orc.glb`, `warden.glb`, `shaman.glb`, `king.glb`, `mira.glb`, `eren.glb` override the animated procedural rigs.

`props/shrine.glb`, `portal.glb`, `chest.glb`, `lore.glb`, `rune.glb`, `seal.glb`, `crystal.glb`, `throne.glb` override procedural props. GLTF, J3O and OBJ alternatives are resolved after GLB. Characters must satisfy the armature/animation contract; OBJ is for static props only.

`props/crystal.gltf` is a first-party glTF reference asset used in the actual caverns. Do not rename its referenced `.bin` buffer without updating the JSON.

Full contract and Blender commands: `docs/ASSETS.md`.
