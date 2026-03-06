export class LogView {
    constructor(logElement) {
        this.logElement = logElement;
    }

    info(message) {
        this.append("info", message);
    }

    success(message) {
        this.append("success", message);
    }

    error(message) {
        this.append("error", message);
    }

    append(level, message) {
        const line = document.createElement("div");
        line.className = `log-line ${level}`;
        const time = new Date().toLocaleTimeString('en-GB', { hour12: false });
        line.textContent = `[${time}] ${message}`;
        this.logElement.prepend(line);
    }
}
