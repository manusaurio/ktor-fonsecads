package ar.pelotude.ktorfds.msgs.impl

import ar.pelotude.ktorfds.msgs.Category
import ar.pelotude.ktorfds.msgs.Conjunction
import ar.pelotude.ktorfds.msgs.Filler
import ar.pelotude.ktorfds.msgs.Message
import ar.pelotude.ktorfds.msgs.Messenger
import ar.pelotude.ktorfds.msgs.Template

/**
 * Management class for setting up message rules.
 *
 * Each message takes up to 48 bits, which represent 2 chosen templates,
 * 2 chosen fillers for those templates and 1 conjunction.
 *
 * The options for each of these pieces must be provided here.
 *
 * @param templates A list of incomplete sentences, each with three asterisks that must be
 *                  filled in with a template to make sense. 255 options at most.
 * @param fillers Options to replace the templates' asterisks with. These are organized
 *                in categories. The provided list must contain pairs where the first
 *                element is the name of the category, and the second element must contain
 *                its fillers.
 * @param conjunctions A conjunction or phrase to connect two sentences.
 *                     There can be only 254 conjunction at most: one value
 *                     must be available to represent a nil conjunction.
 * @param fillersCategoryStep Upper limit of fillers for each category.
 */
class BasicMessenger(
    templates: List<String>,
    fillers: List<Pair<String, List<String>>>,
    conjunctions: List<String>,
    private val fillersCategoryStep: Int = 64,
) : Messenger<Long, Int> {
    private val nullConjunction: Int = 0xff

    private val templates: List<Template<Int>> = templates.mapIndexed { i, text ->
        object : Template<Int> {
            override val value = i
            override val text = text
        }
    }

    private val categories: Map<Int, Category<Int>> = fillers.mapIndexed { i, (categoryName, _) ->
        i * fillersCategoryStep to object : Category<Int> {
            override val id = i * fillersCategoryStep
            override val name = categoryName
        }
    }.toMap()

    private val fillers: Map<Int, Filler<Int>> = fillers.flatMapIndexed { i, (_, choices) ->
        val categoryId = i * fillersCategoryStep

        choices.mapIndexed { j, text ->
            categoryId + j to object : Filler<Int> {
                override val category = categories[categoryId]!!
                override val value = categoryId + j
                override val text = text
            }
        }
    }.toMap()

    private val conjunctions: List<Conjunction<Int>> = conjunctions.mapIndexed { i, text ->
        object : Conjunction<Int> {
            override val value = i
            override val text = text
        }
    }

    override fun create(value: Long): Message<Long, Int> = Bit64Message(value)

    /**
     * Helper function to quickly validate a value without the overhead of instantiating a [Message]
     */
    override fun quickCheck(value: Long): Boolean = arePartsValid(
        (value and 0xff).toInt(),
        (value shr 8 and 0xfff).toInt(),
        (value shr 20 and 0xff).toInt(),
        (value shr 28 and 0xff).toInt(),
        (value shr 36 and 0xfff).toInt()
    ) && isWithinBounds(value)

    private inline fun arePartsValid(
        template1Value: Int,
        filler1Value: Int,
        conjunctionValue: Int,
        template2Value: Int,
        filler2Value: Int,
    ): Boolean =
        template1Value in templates.indices
                && filler1Value in fillers
                && (conjunctionValue == nullConjunction || conjunctionValue in conjunctions.indices)
                && template2Value in templates.indices
                && filler2Value in fillers

    private inline fun isWithinBounds(value: Long) = (value and (0xffffffffffffL).inv() == 0L)

    private inner class Bit64Message(override val value: Long): Message<Long, Int> {
        private val template1Value = (value and 0xff).toInt()
        private val filler1Value = (value shr 8 and 0xfff).toInt()
        private val conjunctionValue = (value shr 20 and 0xff).toInt()
        private val template2Value = (value shr 28 and 0xff).toInt()
        private val filler2Value = (value shr 36 and 0xfff).toInt()

        private val messageText by lazy {
            templates[template1Value].text.replace("***", fillers[filler1Value]!!.text) +
                    (conjunctionValue.takeIf { it != nullConjunction }?.let {
                        " ${conjunctions[conjunctionValue].text} " +
                                templates[template2Value].text
                                    .replace("***", fillers[filler2Value]!!.text)
                    } ?: "")
        }

        init {
            require(
                arePartsValid(template1Value, filler1Value, conjunctionValue, template2Value, filler2Value)
                        && isWithinBounds((value))
            ) {
                "Invalid message value $value"
            }
        }

        override val template1 = templates[template1Value]

        override val filler1 = fillers[filler1Value]!!

        override val conjunction = conjunctions[conjunctionValue]

        override val template2 = templates[template1Value]

        override val filler2 = fillers[filler2Value]!!

        override fun toString() = messageText
    }
}