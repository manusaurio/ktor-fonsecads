package ar.pelotude.ktorfds

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
class Messenger(
    private val templates: List<String>,
    fillers: List<Pair<String, List<String>>>,
    private val conjunctions: List<String>,
    private val fillersCategoryStep: Int = 64,
) {
    private val nullConjunction: Int = 0xff

    private val fillers = fillers.flatMapIndexed { i, (_, choices) ->
        val categoryId = i * fillersCategoryStep

        choices.mapIndexed { j, text ->
            categoryId + j to Filler(categoryId + j, text)
        }
    }.toMap()

    private val categories = fillers.mapIndexed { i, (categoryName, _) ->
        i * fillersCategoryStep to categoryName
    }.toMap()

    inner class Filler(val id: Int, val text: String) {
        val categoryId: Int
            get() = id / fillersCategoryStep

        val categoryName: String
            get() = categories[categoryId]!!
    }

    fun isValidMessage(n: Long): Boolean {
        val template1 = n and 0xff
        val filler1 = n shr 8 and 0xfff
        val conjunction = n shr 20 and 0xff
        val template2 = n shr 28 and 0xff
        val filler2 = n shr 36 and 0xfff

        return template1 in templates.indices
                && filler1.toInt() in fillers
                && conjunction.toInt().let { conj ->
                    conj == nullConjunction || conj in conjunctions.indices
                }
                && template2 in templates.indices
                && filler2.toInt() in fillers
    }

    fun toString(n: Long): String = if (isValidMessage(n)) {
        val template1 = (n and 0xff).toInt()
        val filler1 = (n shr 8 and 0xfff).toInt()
        val conjunction = (n shr 20 and 0xff).toInt().takeIf { it != nullConjunction }
        val template2 = (n shr 28 and 0xff).toInt()
        val filler2 = (n shr 36 and 0xfff).toInt()

        templates[template1].replace("***", fillers[filler1]!!.text) +
                (conjunction?.let {
                    " ${conjunctions[conjunction]} " +
                            templates[template2]
                                .replace("***", fillers[filler2]!!.text)
                } ?: "")
    } else "Invalid message value"
}