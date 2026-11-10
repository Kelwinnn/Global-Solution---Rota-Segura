"""Registra cada prompt enviado ao Claude Code em prompts.md (exigencia do briefing da GS).

Roda no evento UserPromptSubmit. Le o JSON do evento pela entrada padrao,
acrescenta o prompt ao final de prompts.md e nao imprime nada (o que for
impresso nesse evento vira contexto da conversa).
"""
import json
import os
import sys
from datetime import datetime


def main() -> None:
    try:
        event = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return

    prompt = (event.get("prompt") or "").strip()
    if not prompt:
        return

    project_dir = os.environ.get("CLAUDE_PROJECT_DIR") or event.get("cwd") or os.getcwd()
    path = os.path.join(project_dir, "prompts.md")

    new_file = not os.path.exists(path)
    timestamp = datetime.now().strftime("%d/%m/%Y %H:%M")

    with open(path, "a", encoding="utf-8") as f:
        if new_file:
            f.write("# Prompts de IA usados no projeto\n\n")
            f.write("Registro automatico (hook do Claude Code), exigido pelo briefing da Global Solution.\n\n")
        f.write(f"## {timestamp} - Claude Code\n\n")
        f.write(prompt + "\n\n")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Nunca bloquear o prompt por causa do registro.
        pass
    sys.exit(0)
