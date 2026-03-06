export class LogView {
    #logElement: HTMLElement;

    constructor(logElement: HTMLElement) {
        this.#logElement = logElement;
    }

    info(message: string):    void { this.#append('info',    message); }
    success(message: string): void { this.#append('success', message); }
    error(message: string):   void { this.#append('error',   message); }

    #append(level: string, message: string): void {
        const line = document.createElement('div');
        line.className = `log-line ${level}`;
        const time = new Date().toLocaleTimeString('en-GB', { hour12: false });
        line.textContent = `[${time}] ${message}`;
        this.#logElement.prepend(line);
    }
}
