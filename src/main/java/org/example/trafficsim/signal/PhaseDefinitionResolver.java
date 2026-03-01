package org.example.trafficsim.signal;


@FunctionalInterface
public interface PhaseDefinitionResolver {
    Phase resolve(String id);
}
