"use strict";

document.documentElement.classList.add("js-ready");

const escapeHtml = (value) => value.replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
})[character]);

function tokenClass(token, language) {
    if (token.startsWith("//") || (language === "shell" && token.startsWith("#"))) return "tok-comment";
    if (token.startsWith('"') || token.startsWith("'")) return "tok-string";
    if (/^\d+$/.test(token)) return "tok-number";
    return "tok-keyword";
}

function highlight(code) {
    const language = code.dataset.language;
    const source = code.textContent;
    let pattern;
    if (language === "kotlin") {
        pattern = /(\/\/[^\n]*|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])'|\b(?:abstract|as|class|companion|data|else|extension|false|fun|if|import|interface|internal|is|null|object|override|package|private|return|true|val|var)\b|\b\d+\b)/g;
    } else if (language === "shell") {
        pattern = /(#[^\n]*|"(?:\\.|[^"\\])*"|'[^']*'|\b(?:git|cd|node|kotlinr)\b|\.\/kotlin\b)/g;
    } else if (language === "markdown") {
        pattern = /(^#{1,6}\s.*$|\*\*[^*]+\*\*|`[^`]+`)/gm;
    } else {
        return;
    }
    code.innerHTML = source.replace(pattern, (token) => {
        const type = language === "markdown" ? "tok-keyword" : tokenClass(token, language);
        return `<span class="${type}">${escapeHtml(token)}</span>`;
    });
}

document.querySelectorAll("pre code[data-language]").forEach(highlight);

if (navigator.clipboard && navigator.clipboard.writeText) {
    document.querySelectorAll("pre code").forEach((code) => {
        const button = document.createElement("button");
        button.className = "copy-button";
        button.type = "button";
        button.textContent = "Copy";
        button.setAttribute("aria-label", "Copy code to clipboard");
        code.closest("pre").parentElement.append(button);
        button.addEventListener("click", async () => {
            try {
                await navigator.clipboard.writeText(code.textContent);
                button.textContent = "Copied";
                window.setTimeout(() => { button.textContent = "Copy"; }, 1400);
            } catch {
                button.textContent = "Unavailable";
                window.setTimeout(() => { button.textContent = "Copy"; }, 1400);
            }
        });
    });
}

const previewButtons = [...document.querySelectorAll("[data-preview-button]")];
for (const button of previewButtons) {
    button.addEventListener("click", () => {
        for (const candidate of previewButtons) {
            const selected = candidate === button;
            candidate.setAttribute("aria-pressed", String(selected));
            document.querySelector(`[data-preview-panel="${candidate.dataset.previewButton}"]`).hidden = !selected;
        }
    });
}

const navTargets = ["spec", "extensions", "reference"].map((id) => document.getElementById(id));
if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver((entries) => {
        const visible = entries.filter((entry) => entry.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (!visible) return;
        document.querySelectorAll(".primary-nav a[href^='#']").forEach((link) => {
            if (link.hash === `#${visible.target.id}`) link.setAttribute("aria-current", "location");
            else link.removeAttribute("aria-current");
        });
    }, { rootMargin: "-20% 0px -65% 0px", threshold: [0, 0.25, 0.5, 1] });
    navTargets.filter(Boolean).forEach((section) => observer.observe(section));
}