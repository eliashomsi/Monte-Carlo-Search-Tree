package student_player;

import java.util.*;

import boardgame.Board;
import pentago_swap.PentagoBoardState;
import pentago_swap.PentagoMove;

public class MonteCarloTreeSearch {
    static final int WIN_SCORE = 70;
    static final int DRAW_SCORE = 20;
    static final int SIMULATION_FACTOR = 100;
    int opponent;
    int me;
    Tree tree;

    public PentagoMove findNextMove(PentagoBoardState pbs) {
        opponent = pbs.getOpponent();
        me = pbs.getTurnPlayer();
        if (opponent == me) {
            throw new RuntimeException("DAFAQ?");
        }

        tree = new Tree(new Node(new State(pbs, 0, 0.0d, null), null));
        // figure out where are we

        Node promisingNode = selectPromisingNode(tree.getRoot());
        if (!promisingNode.getState().getPbs().gameOver())
            expandNode(promisingNode);

        Node nodeToExplore = promisingNode;
        if (promisingNode.getChildArray().size() > 0)
            nodeToExplore = promisingNode.getRandomChildNode();

        int playoutResult = simulateRandomPlayout(nodeToExplore);
        backPropogation(nodeToExplore, playoutResult);

        Node winnerNode = tree.getRoot().getChildWithMaxScore();
        return winnerNode.getState().getMove();
    }

    private Node selectPromisingNode(Node rootNode) {
        // expand and simulate first level (explore)
        expandAndSimulate(rootNode);

        // now go greedy
        Node node = rootNode;
        while (node.getChildArray().size() != 0)
            node = UCT.findBestNodeWithUCT(node);
        return node;
    }

    private void expandNode(Node node) {
        List<State> possibleStates = node.getState().getAllPossibleStates();
        possibleStates.forEach(state -> {
            Node newNode = new Node(state, node);
            newNode.setParent(node);
            node.getChildArray().add(newNode);
        });
    }

    private void expandAndSimulate(Node node) {
        List<State> possibleStates = node.getState().getAllPossibleStates();
        possibleStates.forEach(state -> {
            Node newNode = new Node(state, node);
            newNode.setParent(node);
            node.getChildArray().add(newNode);

            // simulate for each one a random number of times
            for (int index = 0; index < new Random().nextInt(SIMULATION_FACTOR); index++) {
                int playoutResult = simulateRandomPlayout(newNode);
                backPropogation(newNode, playoutResult);
                if (new Random().nextBoolean()) {
                    expandAndSimulate(newNode);
                }
            }
        });
    }

    private void backPropogation(Node nodeToExplore, int playerNo) {
        Node tempNode = nodeToExplore;
        while (tempNode != null) {
            tempNode.getState().incrementVisit();
            if (playerNo == me) {
                tempNode.getState().addScore(WIN_SCORE);
            } else if (playerNo == Board.DRAW) {
                tempNode.getState().addScore(DRAW_SCORE);
            }
            tempNode = tempNode.getParent();
        }
    }

    private int simulateRandomPlayout(Node node) {
        Node tempNode = new Node(node);
        State tempState = tempNode.getState();
        PentagoBoardState pbs = tempNode.getState().getPbs();

        while (!tempState.terminal()) {
            tempState.randomPlay();
        }

        if (pbs.gameOver() && pbs.getWinner() == opponent) {
            tempNode.getParent().getState().setWinScore(Integer.MIN_VALUE);
        }

        return pbs.getWinner();
    }
}
