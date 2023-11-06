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
                "desdichado sea ***",
            ),
            fillers = listOf(
                "carreras" to listOf(
                    "el diseño multimedial",
                    "las artes plásticas",
                    "la música",
                    "la historia del arte",
                    "el diseño industrial",
                    "el cine",
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
                    "el arrepentirse",
                    "el sabotaje",
                    "atacar",
                    "defender",
                    "ofenderse",
                    "defenderse",
                ),
                "objetos" to listOf(
                    "las sillas",
                    "los bancos",
                    "el alumnado",
                    "la autoridad",
                    "la comida",
                    "los útiles",
                    "la ropa",
                    "el jabón",
                    "la maquinaria",
                    "el café",
                    "el té",
                    "la paloma",
                    "los animales",
                    "el gato",
                    "el perro",
                    "las aves",
                    "los insectos",
                    "el pasto",
                    "el aire",
                    "el fuego",
                    "la tierra",
                    "el agua",
                    "la botella",
                    "el mate",
                ),
                "geografía" to listOf(
                    "el patio",
                    "el baño",
                    "el salón de clase",
                    "las escaleras",
                    "el ascensor",
                    "diagonal 78",
                    "la salida principal",
                    "la izquierda",
                    "la derecha",
                    "el ascenso",
                    "el descenso",
                    "la ventana",
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
                    "la mentira",
                    "el fraude",
                    "la ley",
                    "la ingenuidad",
                    "lo colectivo",
                    "lo individual",
                    "el tiempo",
                    "lo concreto",
                    "lo aprendido",
                    "el presente",
                    "el pasado",
                    "el futuro",
                    "el bullicio",
                    "lo superior",
                    "lo inferior",
                    "la mujer",
                    "el hombre",
                    "las agrupaciones",
                    "les nerds",
                    "les frikis",
                    "las promesas",
                    "la luz",
                    "la oscuridad",
                    "la obra",
                    "lo incómodo",
                    "lo cómodo",
                    "lo verdadero",
                    "lo falso",
                    "lo elegido",
                    "lo rechazado",
                    "el trabajo",
                    "el cansancio",
                    "lo estúpido",
                    "lo inútil",
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