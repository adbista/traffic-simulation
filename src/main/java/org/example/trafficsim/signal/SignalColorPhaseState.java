package org.example.trafficsim.signal;


// Cykl: GREEN -> YELLOW -> RED -> GREEN (nowa faza)
// nie mozemy robic osobnego takiego cyklu dla kazdego pasa, bo moze dojsc do kolizji
public enum SignalColorPhaseState {
    GREEN,
    YELLOW,
    RED
}