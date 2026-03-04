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

    public Phase current()                      { return current; }
    public boolean isGreen()  { return state == PhaseState.GREEN; }

    public int timeInState() { return timeInState; }

    public void requestNextPhase(Phase nextPhase) {
        if (nextPhase == null || nextPhase.id().equals(current.id())) {
            return;
        }
        if (state != PhaseState.GREEN) {
            return;
        }
        this.pendingNextPhase = nextPhase;
        if (current.timing().yellowSteps() > 0) {
            state = PhaseState.YELLOW;
        } else {
            state = PhaseState.RED;
        }
        timeInState = 0;
    }

    // Advances the state machine by one simulation step:
    // GREEN  - counts time; state only changes via requestSwitchTo
    // YELLOW - transitions to RED after yellowSteps
    // RED    - activates the new phase and returns to GREEN after redSteps
    public void manageLightState() {
        timeInState++;

        switch (state) {
            case GREEN -> {} // nothing to do; waiting for a possible switch request
                    
            case YELLOW -> { // whole one step could be just YELLOW state
                if (timeInState >= current.timing().yellowSteps()) {
                    state = PhaseState.RED;
                    timeInState = 0;
                }
            }
            // all lights red briefly so drivers can react before the next phase begins
            case RED -> { 
                if (timeInState >= current.timing().redSteps()) {
                    if (pendingNextPhase == null) return; // safety guard, should not happen normally
                    this.current = pendingNextPhase;
                    this.pendingNextPhase = null;
                    this.state = PhaseState.GREEN;
                    this.timeInState = 0;
                }
            }
        }
    }
}
