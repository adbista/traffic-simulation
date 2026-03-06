import { cfg } from '../app/intersection-config.js';
import type { PhaseEntry } from '../types.js';

const ROAD_ARROW: Record<string, string> = {
    north: '↑', south: '↓', east: '→', west: '←',
};

export class PhaseView {
    #el: HTMLElement;

    constructor(container: HTMLElement) {
        this.#el = container;
    }

    render(): void {
        const phases: PhaseEntry[] = cfg.phaseList;
        this.#el.innerHTML = '';

        if (phases.length === 0) {
            this.#el.innerHTML = '<span class="phase-empty">No phase data available</span>';
            return;
        }

        const header = document.createElement('div');
        header.className = 'phase-summary';
        const activePh = phases.find(p => p.active);
        header.innerHTML =
            `<span class="phase-summary-count">${phases.length} phase${phases.length !== 1 ? 's' : ''}</span>` +
            (activePh
                ? `<span class="phase-summary-active">active: <strong>${activePh.id}</strong></span>`
                : '');
        this.#el.appendChild(header);

        for (const phase of phases) {
            const card = document.createElement('div');
            card.className = `phase-card phase-card--${phase.state.toLowerCase()}` +
                             (phase.active ? ' phase-card--active' : '');

            const cardHead = document.createElement('div');
            cardHead.className = 'phase-card-head';

            const idEl = document.createElement('span');
            idEl.className = 'phase-id';
            idEl.textContent = phase.id;

            const badge = document.createElement('span');
            badge.className = `phase-state-badge badge-${phase.state.toLowerCase()}`;
            badge.textContent = phase.state;

            cardHead.appendChild(idEl);
            cardHead.appendChild(badge);
            if (phase.active) {
                const activeMark = document.createElement('span');
                activeMark.className = 'phase-active-mark';
                activeMark.textContent = '● active';
                cardHead.appendChild(activeMark);
            }
            card.appendChild(cardHead);

            const lanesEl = document.createElement('div');
            lanesEl.className = 'phase-lanes';
            for (const { road, laneIndex } of phase.lanes) {
                const laneTag = document.createElement('span');
                laneTag.className = `phase-lane phase-lane--${road.toLowerCase()}`;
                const arrow = ROAD_ARROW[road.toLowerCase()] ?? '?';
                laneTag.textContent = `${arrow} ${road.toUpperCase()} L${laneIndex}`;
                lanesEl.appendChild(laneTag);
            }
            card.appendChild(lanesEl);
            this.#el.appendChild(card);
        }
    }
}
