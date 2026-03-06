package org.example.trafficsim.signal;

import org.example.trafficsim.model.PhaseState;

public class ActivePhase {
    private Phase current;
    private PhaseState state;
    private int timeInState; // steps spent in the current signal light state
    private Phase pendingNextPhase; // set when a switch request has been made

    public ActivePhase(Phase initial) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial phase must not be null");
        }
        this.current = initial;
        this.state = PhaseState.GREEN;
        this.timeInState = 0;

    }

    public Phase current()           { return current; }
    public boolean isGreen()          { return state == PhaseState.GREEN; }
    public int timeInState()          { return timeInState; }
    public PhaseState currentState()  { return state; }

    public void requestNextPhase(Phase nextPhase) {
        if (nextPhase == null) {
            return;
        }
        if (nextPhase.id().equals(current.id())) {
            return;
        }
        if (state != PhaseState.GREEN) {
            return;
        }

        this.pendingNextPhase = nextPhase;

        PhaseState prev = this.state;
        this.state = (current.timing().yellowSteps() > 0) ? PhaseState.YELLOW : PhaseState.RED;
        this.timeInState = 0;
    }

    // Advances the state machine by one simulation step:
    // GREEN  - counts time; state only changes via requestNextPhase
    // YELLOW - transitions to RED after yellowSteps
    // RED    - activates the new phase and returns to GREEN after redSteps
    public void manageLightState() {
        timeInState++;

        switch (state) {
            case GREEN -> {
                // no-op
             }

            case YELLOW -> {
                int yellow = current.timing().yellowSteps();
                if (timeInState >= yellow) {
                    state = PhaseState.RED;
                    timeInState = 0;
                }
            }

            case RED -> {
                int red = current.timing().redSteps();
                if (timeInState >= red) {
                    if (pendingNextPhase == null) {
                        throw new IllegalStateException("RED with null pendingNextPhase! current=" + current.id());
                    }
                    current = pendingNextPhase;
                    pendingNextPhase = null;
                    state = PhaseState.GREEN;
                    timeInState = 0;

                }
            }
        }
    }

}