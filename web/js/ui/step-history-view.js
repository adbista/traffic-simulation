/**
 * Single Responsibility: displays the server-emitted StepStatus history.
 * Each received emit from the server is appended as a row in the panel.
 */
export class StepHistoryView {
    /** @param {HTMLElement} container */
    constructor(container) {
        this._container = container;
        this._stepCount = 0;
    }

    /**
     * @param {{ step: number, leftVehicles: string[] }} entry
     */
    addEntry({ step, leftVehicles }) {
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
        this._container.prepend(row); // newest on top
    }

    clear() {
        this._container.innerHTML = '';
        this._stepCount = 0;
    }
}
