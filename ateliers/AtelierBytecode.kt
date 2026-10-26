// Atelier du module 1. À placer dans une cible JVM ou Android, par exemple
// shared/src/androidMain/kotlin/com/advisely/call/AtelierBytecode.kt : le menu Show Kotlin Bytecode
// est grisé sur le code commun. Puis Tools > Kotlin > Show Kotlin Bytecode > Decompile.
package com.advisely.call

import kotlin.time.TimeSource

// Sans inline : le bloc passe en paramètre, un objet de fonction est créé à l'appel (invokedynamic depuis Kotlin 2.0)
fun <T> measuredNoInline(tag: String, block: () -> T): T {
    val mark = TimeSource.Monotonic.markNow()
    try {
        return block()
    } finally {
        println("[$tag] ${mark.elapsedNow()}")
    }
}

// Avec inline : le corps est recopié sur place, aucun objet créé
inline fun <T> measured(tag: String, block: () -> T): T {
    val mark = TimeSource.Monotonic.markNow()
    try {
        return block()
    } finally {
        println("[$tag] ${mark.elapsedNow()}")
    }
}

// Décompiler ces deux fonctions et comparer leurs appels
fun appelNonInline(): Int {
    val base = 40
    return measuredNoInline("non-inline") { base + 2 }   // capture `base` : allocation à chaque appel
}

fun appelInline(): Int {
    val base = 40
    return measured("inline") { base + 2 }               // plus d'objet de fonction dans le décompilé
}

// Étape 4, fausse piste : passer cette grosse fonction en inline et regarder grossir chaque appel décompilé
fun <T> grosseFonctionUtilitaire(tag: String, block: () -> T): T {
    val mark = TimeSource.Monotonic.markNow()
    val lignes = mutableListOf<String>()
    repeat(3) { i -> lignes += "[$tag] préparation $i" }
    lignes.forEach(::println)
    val resultat = block()
    if (mark.elapsedNow().inWholeMilliseconds > 100) println("[$tag] lent : ${mark.elapsedNow()}")
    lignes.clear()
    return resultat
}
