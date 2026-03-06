import type { StepEntry } from '../types.js';

export class StepHistoryView {
    #container: HTMLElement;

    constructor(container: HTMLElement) {
        this.#container = container;
    }

    addEntry({ step, leftVehicles }: Pick<StepEntry, 'step' | 'leftVehicles'>): void {
        const row = document.createElement('div');
        row.className = 'history-row';

        const badge = document.createElement('span');
        badge.className = 'history-step';
        badge.textContent = `Step ${step}`;

        const vehicles = document.createElement('span');
        vehicles.className = leftVehicles.length > 0 ? 'history-vehicles' : 'history-empty';
        vehicles.textContent = leftVehicles.length > 0
            ? leftVehicles.join(', ')
            : 'no vehicles left';

        row.appendChild(badge);
        row.appendChild(vehicles);
        this.#container.prepend(row);
    }

    clear(): void {
        this.#container.innerHTML = '';
    }
}
