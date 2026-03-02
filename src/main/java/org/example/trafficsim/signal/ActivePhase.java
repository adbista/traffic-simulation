package org.example.trafficsim.signal;



public class ActivePhase {
    private Phase current;
    private SignalColorPhaseState state;
    private int timeInState; // steps spent in the current signal colour state
    private String pendingNextPhaseId; // set when a switch request has been made

    public ActivePhase(Phase initial) {
        this.current = initial;
        this.state = SignalColorPhaseState.GREEN;
        this.timeInState = 0;
    }

    public Phase current()                      { return current; }
    public SignalColorPhaseState signalState()  { return state; }

    public boolean isGreen()  { return state == SignalColorPhaseState.GREEN; }
    public boolean isYellow() { return state == SignalColorPhaseState.YELLOW; }

    public int timeInState()           { return timeInState; }
    public String pendingNextPhaseId(){ return pendingNextPhaseId; }


    public void requestSwitchTo(String nextPhaseId) {
        if (nextPhaseId == null || nextPhaseId.equals(current.id())) {
            return;
        }
        if (state != SignalColorPhaseState.GREEN) {
            return;
        }
        this.pendingNextPhaseId = nextPhaseId;
        if (current.timing().yellowSteps() > 0) {
            state = SignalColorPhaseState.YELLOW;
        } else {
            state = SignalColorPhaseState.RED;
        }
        timeInState = 0;
    }

    // Advances the state machine by one simulation step:
    // GREEN  - counts time; state only changes via requestSwitchTo
    // YELLOW - transitions to RED after yellowSteps
    // RED    - activates the new phase and returns to GREEN after redSteps
    public void tick(PhaseDefinitionResolver resolver) {
        timeInState++;

        switch (state) {
            case GREEN -> {} // nothing to do; waiting for a possible switch request
                    
            case YELLOW -> {
                if (timeInState >= current.timing().yellowSteps()) {
                    state = SignalColorPhaseState.RED;
                    timeInState = 0;
                }
            }
            // all lights red briefly so drivers can react before the next phase begins
            case RED -> { 
                if (timeInState >= current.timing().redSteps()) {
                    if (pendingNextPhaseId == null) return; // safety guard; should not happen normally 
                    this.current = resolver.resolve(pendingNextPhaseId);
                    this.pendingNextPhaseId = null;
                    this.state = SignalColorPhaseState.GREEN;
                    this.timeInState = 0;
                }
            }
        }
    }
}
