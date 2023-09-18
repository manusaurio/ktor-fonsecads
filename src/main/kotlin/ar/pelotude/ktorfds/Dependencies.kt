package ar.pelotude.ktorfds

import ar.pelotude.ktorfds.db.Database
import ar.pelotude.ktorfds.db.MessagesDatabase
import ar.pelotude.ktorfds.msgs.Messenger
import ar.pelotude.ktorfds.msgs.impl.BasicMessenger
import org.koin.dsl.module

val dependencies = module {

    single<MessagesDatabase<Long>> {
        Database(
            System.getenv("FONSECADS_DB") ?: "jdbc:sqlite:messages.db",
        )
    }

    single<Messenger<Long, *>> {
        // TODO: the strings should be stored in the db and taken from there
        BasicMessenger(
            templates = listOf(
                "ten cautela con ***",
                "que viva ***",
                "hay visiones de ***",
                "*** es requisito acá",
                "abunda ***",
                "*** escasea",
            ),
            fillers = listOf(
                "carreras" to listOf(
                    "el diseño multimedial",
                    "las artes plásticas",
                    "la música",
                    "la historia del arte",
                ),
                "acciones" to listOf(
                    "ordenar",
                    "desordenar todo",
                    "saltarse las clases",
                    "asistir a clase",
                    "estudiar",
                    "conseguir apuntes",
                    "tener todo al día",
                    "entrar al salón",
                    "salir del salón",
                ),
                "objetos" to listOf(
                    "las sillas",
                    "los bancos",
                    "el alumnado",
                    "la autoridad",
                    "la comida",
                    "los útiles",
                ),
                "geografía" to listOf(
                    "el patio",
                    "el baño",
                    "el salón de clase",
                    "las escaleras",
                    "el ascensor",
                    "diagonal 78",
                    "la salida principal",
                ),
                "conceptos" to listOf(
                    "la cantidad de gente",
                    "la suciedad",
                    "la limpieza",
                    "la velocidad",
                    "el tiempo de clase",
                    "la felicidad",
                    "la tristeza",
                    "las votaciones",
                    "el día de entrega",
                )
            ),
            conjunctions = listOf(
                "por lo tanto",
                "por sobre todo",
                "sin embargo",
                "a pesar de eso",
                "y además",
                "específicamente",
            ),
        )
    }
}