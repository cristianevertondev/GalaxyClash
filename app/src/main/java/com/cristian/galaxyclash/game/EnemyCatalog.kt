package com.cristian.galaxyclash.game

/**
 * Central table of enemy type configurations. Difficulty scaling (extra HP,
 * speed, spawn weight) is layered on top by [DifficultySystem] later; these
 * values are the *base* per type for the current build.
 */
object EnemyCatalog {

    private val specs: Map<EnemyKind, EnemySpec> = mapOf(
        EnemyKind.BASIC to EnemySpec(
            kind = EnemyKind.BASIC,
            hp = 2,
            width = 44f, height = 44f,
            baseSpeed = 120f, lateralSpeed = 40f,
            shootCooldown = 0f, projectileSpeed = 0f,
            scoreValue = 100, contactDamage = 1,
            aggression = 0f,
        ),
        EnemyKind.ZIGZAG to EnemySpec(
            kind = EnemyKind.ZIGZAG,
            hp = 1,
            width = 42f, height = 42f,
            baseSpeed = 110f, lateralSpeed = 160f,
            shootCooldown = 0f, projectileSpeed = 0f,
            scoreValue = 150, contactDamage = 1,
            aggression = 0f,
        ),
        EnemyKind.RANDOM to EnemySpec(
            kind = EnemyKind.RANDOM,
            hp = 1,
            width = 40f, height = 40f,
            baseSpeed = 130f, lateralSpeed = 110f,
            shootCooldown = 0f, projectileSpeed = 0f,
            scoreValue = 150, contactDamage = 1,
            aggression = 0f,
        ),
        EnemyKind.SHOOTER to EnemySpec(
            kind = EnemyKind.SHOOTER,
            hp = 2,
            width = 46f, height = 46f,
            baseSpeed = 80f, lateralSpeed = 70f,
            shootCooldown = 2.2f, projectileSpeed = 260f,
            scoreValue = 250, contactDamage = 1,
            aggression = 0f,
        ),
        EnemyKind.AGGRESSIVE to EnemySpec(
            kind = EnemyKind.AGGRESSIVE,
            hp = 2,
            width = 42f, height = 42f,
            baseSpeed = 140f, lateralSpeed = 120f,
            shootCooldown = 0f, projectileSpeed = 0f,
            scoreValue = 200, contactDamage = 1,
            aggression = 0.5f,
        ),
        EnemyKind.ELITE to EnemySpec(
            kind = EnemyKind.ELITE,
            hp = 6,
            width = 56f, height = 56f,
            baseSpeed = 90f, lateralSpeed = 130f,
            shootCooldown = 1.8f, projectileSpeed = 300f,
            scoreValue = 400, contactDamage = 1,
            aggression = 0.15f,
        ),
    )

    fun spec(kind: EnemyKind): EnemySpec =
        specs[kind] ?: specs.getValue(EnemyKind.BASIC)
}