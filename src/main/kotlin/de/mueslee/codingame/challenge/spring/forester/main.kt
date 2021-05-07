package de.mueslee.codingame.challenge.spring.forester

import java.util.*

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val game = Game()
    val numberOfCells = input.nextInt()

    for (i in 0 until numberOfCells) {
        val index = input.nextInt()
        val richness = input.nextInt()
        val neigh0 = input.nextInt()
        val neigh1 = input.nextInt()
        val neigh2 = input.nextInt()
        val neigh3 = input.nextInt()
        val neigh4 = input.nextInt()
        val neigh5 = input.nextInt()
        val neighs = intArrayOf(neigh0, neigh1, neigh2, neigh3, neigh4, neigh5)
        val cell = Cell(index, richness, neighs)
        game.board.add(cell)
    }
    while (true) {
        game.day = input.nextInt()
        game.nutrients = input.nextInt()
        game.mySun = input.nextInt()
        game.myScore = input.nextInt()
        game.opponentSun = input.nextInt()
        game.opponentScore = input.nextInt()
        game.opponentIsWaiting = input.nextInt() != 0
        game.trees.clear()
        game.myTrees.clear()
        val numberOfTrees = input.nextInt()
        for (i in 0 until numberOfTrees) {
            val cellIndex = input.nextInt()
            val size = input.nextInt()
            val isMine = input.nextInt() != 0
            val isDormant = input.nextInt() != 0
            val tree = Tree(cellIndex, size, isMine, isDormant)
            game.trees.add(tree)
        }
        game.myTrees.addAll(game.trees.filter { it.isMine })
        game.possibleActions.clear()
        val numberOfPossibleActions = input.nextInt()
        input.nextLine()
        for (i in 0 until numberOfPossibleActions) {
            val possibleAction = input.nextLine()
            game.possibleActions.add(Action.parse(possibleAction))
        }
        val action = game.nextAction()

        println(action.toString())
    }
}

data class Action(
    var type: String,
    var sourceCellIdx: Int? = null,
    var targetCellIdx: Int? = null
) {
    constructor(type: String, targetCellIdx: Int?) : this(type, null, targetCellIdx) {}

    override fun toString(): String {
        if (WAIT.equals(type, ignoreCase = true)) {
            return WAIT
        }
        return if (SEED.equals(type, ignoreCase = true)) {
            String.format("%s %d %d", SEED, sourceCellIdx, targetCellIdx)
        } else String.format("%s %d", type, targetCellIdx)
    }

    companion object {
        const val WAIT = "WAIT"
        const val SEED = "SEED"
        const val GROW = "GROW"
        const val COMPLETE = "COMPLETE"
        fun parse(action: String): Action {
            val parts = action.split(" ").toTypedArray()
            return when (parts[0]) {
                WAIT -> Action(WAIT)
                SEED -> Action(
                    SEED,
                    Integer.valueOf(parts[1]),
                    Integer.valueOf(parts[2])
                )
                GROW, COMPLETE -> Action(parts[0], Integer.valueOf(parts[1]))
                else -> Action(parts[0], Integer.valueOf(parts[1]))
            }
        }
    }
}

data class Cell(val index: Int, val richness: Int, val neighbours: IntArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cell

        if (index != other.index) return false
        if (richness != other.richness) return false
        if (!neighbours.contentEquals(other.neighbours)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + richness
        result = 31 * result + neighbours.contentHashCode()
        return result
    }
}

data class Game(
    var day: Int = 0,
    var nutrients: Int = 0,
    val board: MutableList<Cell> = ArrayList(),
    val possibleActions: MutableList<Action> = ArrayList(),
    val trees: MutableList<Tree> = ArrayList(),
    val myTrees: MutableList<Tree> = ArrayList(),
    var mySun: Int = 0,
    var opponentSun: Int = 0,
    var myScore: Int = 0,
    var opponentScore: Int = 0,
    var opponentIsWaiting: Boolean = false
) {
    fun nextAction(): Action {
        if (day == 0) {
            return Action("WAIT")
        }

        val numberOfSeeds = myTrees.filter { it.size == 0 }.size
        val numberOf1Trees = myTrees.filter { it.size == 1 }.size
        val numberOf2Trees = myTrees.filter { it.size == 2 }.size
        val numberOf3Trees = myTrees.filter { it.size == 3 }.size

        val seed = findActionForRichestCell(possibleActions.filter { it.type == "SEED" }).takeIf { numberOfSeeds <= 2 && numberOf1Trees < 1  || day >= 22}
        val grow = findActionForRichestCell(possibleActions.filter { it.type == "GROW" })
        val complete = findActionForRichestCell(possibleActions.filter { it.type == "COMPLETE" }).takeIf { day >= 21 || numberOf3Trees > 3 }
        val wait = possibleActions.find { it.type == "WAIT" }

        return listOf(complete, grow, seed, wait).first { it != null }!!
    }

    private fun findActionForRichestCell(actions: List<Action>): Action? {
        return actions.takeIf { it.isNotEmpty() }?.sortedBy { action -> board.find { it.index == action.targetCellIdx }!!.richness }?.last()
    }
}

data class Tree(val cellIndex: Int, val size: Int, val isMine: Boolean, val isDormant: Boolean)
