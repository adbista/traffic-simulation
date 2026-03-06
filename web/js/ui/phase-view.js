import { cfg } from '../app/intersection-config.js';

const ROAD_ARROW = { north: '↑', south: '↓', east: '→', west: '←' };

export class PhaseView {
    /** @param {HTMLElement} container */
    constructor(container) {
        this._el = container;
    }

    render() {
        const phases = cfg.phaseList;   // [{id, lanes, active, state}]
        this._el.innerHTML = '';

        if (phases.length === 0) {
            this._el.innerHTML = '<span class="phase-empty">No phase data available</span>';
            return;
        }

        // Header summary
        const header = document.createElement('div');
        header.className = 'phase-summary';
        const activePh = phases.find(p => p.active);
        header.innerHTML =
            `<span class="phase-summary-count">${phases.length} phase${phases.length !== 1 ? 's' : ''}</span>` +
            (activePh
                ? `<span class="phase-summary-active">active: <strong>${activePh.id}</strong></span>`
                : ``);
        this._el.appendChild(header);

        // Phase cards
        for (const phase of phases) {
            const card = document.createElement('div');
            card.className = `phase-card phase-card--${phase.state.toLowerCase()}` +
                             (phase.active ? ' phase-card--active' : '');

            // Top row: id + state badge
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

            // Lanes list
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

            this._el.appendChild(card);
        }
    }
}
