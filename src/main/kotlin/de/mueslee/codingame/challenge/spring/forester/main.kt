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
            val tree = Tree(game.board.find { it.index == cellIndex }!!, size, isMine, isDormant)
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

        val desiredOuterFarmingTreeCount = 4
        val lastDayBeforeCompletions = 20

        val mySeeds = myTrees.filter { it.size == 0 }
        val my1Trees = myTrees.filter { it.size == 1 }
        val my2Trees = myTrees.filter { it.size == 2 }
        val my3Trees = myTrees.filter { it.size == 3 }

        val myOuterFarmingTrees = myTrees.filter { isInOuterRing(it.cell.index) }

        val coreStrategyActions: MutableList<Action?> = ArrayList()

        if (day > lastDayBeforeCompletions) {
            coreStrategyActions.add(findActionForRichestCell(possibleActions.filter { it.type == "COMPLETE" }))
        }

        if (myOuterFarmingTrees.filter { it.size < 3 }.count() < desiredOuterFarmingTreeCount) {
            coreStrategyActions.add(possibleActions.find { it.type == "GROW" && isInOuterRing(it.targetCellIdx!!) })
        }

        if (myTrees.filter { isInOuterRing(it.cell.index) }.count() < desiredOuterFarmingTreeCount && day < lastDayBeforeCompletions) {
            val seedForOuterFarmingTree =
                possibleActions.find { it.type == "SEED" && isInOuterRing(it.targetCellIdx!!) && !isBadNeighbourhood(it.targetCellIdx!!) }
            coreStrategyActions.add(seedForOuterFarmingTree)
        }

        val completeValueTreeAction =
            possibleActions.filter { it.type == "COMPLETE" && !isInOuterRing(it.targetCellIdx!!) }
                .takeIf { it.size > 1 }?.firstOrNull()
        coreStrategyActions.add(completeValueTreeAction)

        val wellPlacedRichSeed =
            findActionForRichestCell(possibleActions.filter { it.type == "SEED" && !isBadNeighbourhood(it.targetCellIdx!!) }).takeIf { mySeeds.size < 3 || day < lastDayBeforeCompletions }

        val badPlacedRichSeed =
            findActionForRichestCell(possibleActions.filter { it.type == "SEED" }).takeIf { mySeeds.isEmpty() && myOuterFarmingTrees.size >= desiredOuterFarmingTreeCount && day < lastDayBeforeCompletions }

        val grow = findActionForRichestCell(possibleActions.filter { it.type == "GROW" })
        val growBiggestTree = possibleActions.asSequence()
            .filter { it.type == "GROW" }
            .map { action -> myTrees.find { it.cell.index == action.targetCellIdx } }
            .filterNotNull()
            .sortedBy { it.size }.map { Action("GROW", targetCellIdx = it.cell.index) }
            .lastOrNull()

        coreStrategyActions.addAll(listOf(growBiggestTree, grow, wellPlacedRichSeed, badPlacedRichSeed))

        if (day == 23) {
            return findActionForRichestCell(possibleActions.filter { it.type == "COMPLETE" }) ?: Action("WAIT")
        }

        return coreStrategyActions.filterNotNull().firstOrNull() ?: Action("WAIT")
    }

    private fun isBadNeighbourhood(targetCellIdx: Int): Boolean {

        val isNeighbourOfTree = trees.flatMap { it.cell.neighbours.asIterable() }.contains(targetCellIdx)
        val isNeighbourOfMountain = board.first { it.index == targetCellIdx }.richness == 0
        return isNeighbourOfMountain || isNeighbourOfTree
    }

    private fun findActionForRichestCell(actions: List<Action>): Action? {
        return actions.takeIf { it.isNotEmpty() }
            ?.sortedBy { action -> board.find { it.index == action.targetCellIdx }!!.richness }?.last()
    }

    private fun isInOuterRing(cellId: Int): Boolean {
        return cellId in 19..36
    }

    private fun isInMiddleRing(cellId: Int): Boolean {
        return cellId in 7..18
    }
}

data class Tree(val cell: Cell, val size: Int, val isMine: Boolean, val isDormant: Boolean)
