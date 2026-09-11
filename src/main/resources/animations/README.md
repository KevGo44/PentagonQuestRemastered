# Character animation contract

Clips are embedded in character GLBs/J3Os. Names are case-sensitive:
Idle, Walk, Run, Attack1, Attack2, Attack3, Dodge, Block, Hit, Death, Cast.

Use in-place movement; Bullet owns world translation. Authored attacks align to the wind-up/active times in `combat/AttackTimeline.java`. The stock placeholder implements a 16-joint armature and weighted meshes, not UI-only animation labels.
