"""Monta os 3 PDFs da Rota Segura (DDD Java, Database Design, Agile) a partir dos .md e das imagens do repositório.

Como gerar (na raiz do repositório, numa pasta temporária fora dele):

    python3 -m venv /tmp/pdfenv && /tmp/pdfenv/bin/pip install markdown==3.7
    /tmp/pdfenv/bin/python -I docs/pdf/build_pdfs.py . /tmp/rota-pdf
    CH=~/.cache/puppeteer/chrome-headless-shell/linux-*/chrome-headless-shell-linux64/chrome-headless-shell
    for n in ddd database agile; do $CH --headless --disable-gpu --allow-file-access-from-files \
        --no-pdf-header-footer --print-to-pdf=/tmp/rota-pdf/$n.pdf file:///tmp/rota-pdf/$n.html; done

O Chrome headless vem com o mermaid-cli (npx @mermaid-js/mermaid-cli@11.4.2). Depois, copie:
    ddd.pdf      -> domain-driven-design-java/docs/Rota-Segura-DDD-Java.pdf
    database.pdf -> database-design/Rota-Segura-Database-Design.pdf
    agile.pdf    -> agile-methodology/Rota-Segura-Agile.pdf

Uso: build_pdfs.py <raiz do repositório> <pasta de saída do HTML>
"""
import html
import pathlib
import re
import sys

import markdown

ROOT = pathlib.Path(sys.argv[1]).resolve()
OUT = pathlib.Path(sys.argv[2]).resolve()
OUT.mkdir(parents=True, exist_ok=True)

MEMBERS = [
    ("João Paulo Santana Basta", "565383"),
    ("Kelwin Silva Bastos", "566348"),
    ("Vitor Mantovani Camilo", "556537"),
]  # já em ordem alfabética

CSS = """
@page { size: A4; margin: 18mm 16mm 20mm 16mm;
  @bottom-center { content: counter(page); font: 10pt 'DejaVu Sans', sans-serif; color: #555; } }
@page cover { @bottom-center { content: none; } }
@page landscape { size: A4 landscape; margin: 12mm 12mm 16mm 12mm; }
.landscape { page: landscape; }
.landscape figure img { max-height: 160mm; }
body { font-family: 'DejaVu Sans', 'Liberation Sans', Arial, sans-serif; font-size: 10.5pt; line-height: 1.45; color: #1A1F2B; }
h1 { font-size: 19pt; color: #0B5CAD; border-bottom: 2px solid #0B5CAD; padding-bottom: 4px; margin-top: 0; break-before: page; }
h2 { font-size: 14pt; color: #0B5CAD; margin-top: 18px; break-after: avoid; }
h3 { font-size: 12pt; margin-top: 14px; break-after: avoid; }
h4 { font-size: 11pt; break-after: avoid; }
table { border-collapse: collapse; width: 100%; margin: 8px 0 12px; font-size: 9pt; break-inside: auto; }
th, td { border: 1px solid #C9D0D8; padding: 4px 6px; text-align: left; vertical-align: top; }
th { background: #EEF2F6; }
tr { break-inside: avoid; }
code { font-family: 'DejaVu Sans Mono', monospace; font-size: 8.6pt; background: #F1F3F5; padding: 0 2px; border-radius: 3px; }
pre { background: #F7F8FA; border: 1px solid #DDE2E8; padding: 8px; font-size: 7.6pt; line-height: 1.3; white-space: pre-wrap; word-break: break-word; }
pre code { background: none; padding: 0; font-size: inherit; }
img { max-width: 100%; }
figure { margin: 10px 0 16px; text-align: center; break-inside: avoid; }
figure img { max-width: 100%; max-height: 225mm; border: 1px solid #E3E7EC; }
figcaption { font-size: 9pt; color: #4A5263; margin-top: 4px; }
.cover { page: cover; height: 250mm; display: flex; flex-direction: column; justify-content: center; text-align: center; }
.cover .project { font-size: 34pt; font-weight: bold; color: #0B5CAD; margin: 0; }
.cover .subject { font-size: 16pt; margin: 8px 0 4px; }
.cover .event { font-size: 12pt; color: #4A5263; margin-bottom: 40px; }
.cover table { width: 70%; margin: 0 auto; font-size: 11pt; }
.cover .team { font-size: 12pt; margin-top: 30px; }
.toc { break-before: page; }
.toc h2 { color: #0B5CAD; font-size: 18pt; }
.toc ol { font-size: 12pt; line-height: 2; }
.phones { display: flex; gap: 8mm; justify-content: center; flex-wrap: wrap; break-inside: avoid; }
.phones figure { width: 52mm; }
.phones figure img { max-height: none; }
.note { border-left: 4px solid #C2410C; background: #FFF4EC; padding: 8px 12px; margin: 10px 0; }
"""


LIST_ITEM = re.compile(r"^( *)([-*]|\d+\.) ")


def fix_lists(text: str) -> str:
    """O python-markdown exige linha em branco antes de lista e 4 espaços na lista aninhada; o GitHub não."""
    out, in_code, prev, fence_indent = [], False, "", 0
    for line in text.splitlines():
        if line.lstrip().startswith("```"):
            if not in_code:
                fence_indent = len(line) - len(line.lstrip(" "))
                if out and out[-1].strip():
                    out.append("")
            in_code = not in_code
            out.append(line.lstrip(" "))
            if not in_code:
                out.append("")
            prev = ""
            continue
        if in_code and fence_indent:
            line = line[fence_indent:] if line.startswith(" " * fence_indent) else line.lstrip(" ")
        m = None if in_code else LIST_ITEM.match(line)
        if m:
            indent = len(m.group(1))
            if 0 < indent % 4:
                line = " " * (4 * ((indent + 3) // 4)) + line.lstrip(" ")
            if indent == 0 and prev.strip() and not LIST_ITEM.match(prev) and not prev.startswith(" "):
                out.append("")
        out.append(line)
        prev = line
    return "\n".join(out)


def md(text: str) -> str:
    text = re.sub(r"<(ator|ação|benefício)>", r"&lt;\1&gt;", text)
    return markdown.markdown(fix_lists(text), extensions=["tables", "fenced_code", "sane_lists"])


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def strip_h1(text: str, drop_first_paragraph: bool = False) -> str:
    """Tira o título (# ...) do arquivo e, se pedido, o primeiro parágrafo (nota interna sobre o PDF)."""
    lines = text.splitlines()
    if lines and lines[0].startswith("# "):
        lines = lines[1:]
    body = "\n".join(lines).lstrip("\n")
    if drop_first_paragraph:
        body = body.split("\n\n", 1)[1] if "\n\n" in body else ""
    return body


def img_uri(rel: str) -> str:
    return (ROOT / rel).as_uri()


def figure(rel: str, caption: str = "", style: str = "") -> str:
    cap = f"<figcaption>{html.escape(caption)}</figcaption>" if caption else ""
    st = f' style="{style}"' if style else ""
    return f'<figure><img src="{img_uri(rel)}"{st} alt="{html.escape(caption)}">{cap}</figure>'


def mermaid_to_images(text: str, images: list[str]) -> str:
    """Troca cada bloco ```mermaid pela imagem correspondente, na ordem."""
    it = iter(images)

    def repl(_m):
        return f"\n\n<!--IMG:{next(it)}-->\n\n"

    return re.sub(r"```mermaid\n.*?```", repl, text, flags=re.S)


def put_images(rendered: str) -> str:
    return re.sub(r"<!--IMG:(.*?)-->", lambda m: figure(m.group(1)), rendered)


def cover(subject: str, team_line: bool) -> str:
    rows = "".join(f"<tr><td>{n}</td><td>RM {rm}</td></tr>" for n, rm in MEMBERS)
    team = '<p class="team"><b>Equipe:</b> Rota Segura</p>' if team_line else ""
    return (f'<section class="cover"><p class="project">Rota Segura</p>'
            f'<p class="subject">{subject}</p>'
            f'<p class="event">Global Solution FIAP 2026.2 — CitizenTech</p>'
            f'<table><tr><th>Integrante</th><th>RM</th></tr>{rows}</table>{team}</section>')


def toc(titles: list[str], heading: str = "Sumário") -> str:
    items = "".join(f"<li>{html.escape(t)}</li>" for t in titles)
    return f'<section class="toc"><h2>{heading}</h2><ol>{items}</ol></section>'


def section(title: str, body_html: str) -> str:
    return f"<h1>{html.escape(title)}</h1>\n{body_html}"


def page(title: str, parts: list[str]) -> str:
    return (f'<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>{html.escape(title)}</title>'
            f"<style>{CSS}</style></head><body>{''.join(parts)}</body></html>")


def sql_block(rel: str) -> str:
    return f"<p>Arquivo <code>{rel}</code>, completo:</p><pre><code>{html.escape(read(rel))}</code></pre>"


# ---------------------------------------------------------------- DDD Java
DDD = "domain-driven-design-java/docs"
proto = [
    ("agile-methodology/img/tela-1-mapa.png", "1 · Mapa (tela inicial, RF14)"),
    ("agile-methodology/img/tela-2-alerta-sms.png", "2 · Alerta por SMS com a rota (RF05, RF07, RF12)"),
    ("agile-methodology/img/tela-3-check-in.png", "3 · Check-in \"Estou seguro\" (RF09)"),
]
phones = '<div class="phones">' + "".join(figure(p, c) for p, c in proto) + "</div>"
proto_html = (md("Protótipo no Figma: https://www.figma.com/design/EyUuqW4eIANQGnDVRNSYdR (dados fictícios). "
                 "As telas consomem os endpoints da API: o mapa usa `GET /api/areas/risk` e `GET /api/shelters`; "
                 "o painel usa `GET /api/assisted-people`.")
              + phones
              + figure("agile-methodology/img/tela-4-painel.png", "4 · Painel de prioridade de resgate do operador (RF10)"))

ddd_sections = [
    ("Especificação do projeto", md(strip_h1(read(f"{DDD}/especificacao.md"), True))),
    ("Diagrama de classes", put_images(md(mermaid_to_images(strip_h1(read(f"{DDD}/class-diagram.md")),
        [f"{DDD}/img/class-diagram-{i}.png" for i in range(1, 5)])))),
    ("Protótipos de tela", proto_html),
    ("Tabela de endpoints", md(strip_h1(read(f"{DDD}/endpoints.md")))),
    ("DER (modelo de dados)", put_images(md(mermaid_to_images(strip_h1(read(f"{DDD}/der.md"), True),
        [f"{DDD}/img/der.png"])))),
    ("Instruções de execução", md(strip_h1(read(f"{DDD}/como-executar.md"), True))),
]
(OUT / "ddd.html").write_text(page("Rota Segura — Domain Driven Design Java", [
    cover("Domain Driven Design Java", False),
    toc([t for t, _ in ddd_sections], "Índice"),
    *[section(f"{i}. {t}", b) for i, (t, b) in enumerate(ddd_sections, 1)],
]), encoding="utf-8")

# ---------------------------------------------------------------- Database
apres = read("database-design/apresentacao.md").split("\n---\n", 1)[0]
obj = apres.split("## 1. Objetivo e escopo", 1)[1].split("## 2. Funcionalidades", 1)[0]
func = apres.split("## 2. Funcionalidades", 1)[1]
modelo = (md("Modelo feito no **Oracle SQL Developer Data Modeler** a partir do `ddl.sql`. "
             "O projeto do Data Modeler está em `database-design/modelo/rota-segura.dmd`.")
          + "<h2>Modelo lógico</h2>"
          + md("Notação Barker: o pé de galinha marca o lado N do relacionamento, e a linha tracejada indica participação "
               "opcional (uma área *pode* ter sensores; todo sensor *precisa* de uma área). `#` marca o identificador e `*` "
               "os atributos obrigatórios. `T_RS_SHELTER` não se relaciona com as outras tabelas: a rota escolhe o abrigo "
               "mais próximo com vaga.")
          + figure("database-design/modelo/ModeloLogico.png", "Modelo lógico (Oracle Data Modeler)")
          + "<h2>Modelo relacional</h2>"
          + figure("database-design/modelo/ModeloRelacional.png", "Modelo relacional, com tipos, PKs, FKs e índices",
                   "max-height: 235mm"))
db_sections = [
    ("Objetivo e escopo", md(obj)),
    ("Funcionalidades", md(func)),
    ("Modelo lógico (Oracle Data Modeler)", modelo),
    ("Script DDL", sql_block("database-design/ddl.sql")),
    ("Script DML", sql_block("database-design/dml.sql")),
    ("Script DQL", md("Três consultas, cada uma com a pergunta de negócio e o resultado esperado para os dados do `dml.sql`: "
                      "INNER JOIN de duas tabelas; INNER JOIN com função de grupo e GROUP BY; LEFT OUTER JOIN com WHERE e ORDER BY.")
                   + sql_block("database-design/dql.sql")),
]
(OUT / "database.html").write_text(page("Rota Segura — Database Design", [
    cover("Database Design", True),
    toc([t for t, _ in db_sections]),
    *[section(f"{i}. {t}", b) for i, (t, b) in enumerate(db_sections, 1)],
]), encoding="utf-8")

# ---------------------------------------------------------------- Agile
AG = "agile-methodology"
ideia = strip_h1(read(f"{AG}/ideia.md"))
ideia = re.sub(r"^\*\*Equipe:\*\*.*\n\*\*Desafio:\*\*.*\n\n", "", ideia)
backlog = re.sub(r"^\*\*Equipe:\*\*.*\n\n", "", strip_h1(read(f"{AG}/backlog.md")))
prot = re.sub(r"^\*\*Equipe:\*\*.*\n\n", "", strip_h1(read(f"{AG}/prototipo.md")))
tela_imgs = {
    "## Tela 1": figure(f"{AG}/img/tela-1-mapa.png", "Tela 1 · Mapa", "width: 62mm"),
    "## Tela 2": figure(f"{AG}/img/tela-2-alerta-sms.png", "Tela 2 · Alerta por SMS com a rota", "width: 62mm"),
    "## Tela 3": figure(f"{AG}/img/tela-3-check-in.png", "Tela 3 · Check-in", "width: 62mm"),
    "## Tela 4": figure(f"{AG}/img/tela-4-painel.png", "Tela 4 · Painel de prioridade de resgate"),
}
prot_html_parts = []
for block in re.split(r"(?=^## )", prot, flags=re.M):
    prot_html_parts.append(md(block))
    for key, fig in tela_imgs.items():
        if block.startswith(key):
            prot_html_parts.append(fig)
poker = read(f"{AG}/planning-poker.md")
poker_how = poker.split("## 1. Abertura", 1)[1].split("## 3. Histórias para votar", 1)[0]
poker_sheet = poker.split("## 4. Folha de votação", 1)[1].split("## 5. Depois da sessão", 1)[0]
casos = (figure(f"{AG}/img/casos-de-uso.png", "Diagrama de casos de uso (PlantUML: agile-methodology/casos-de-uso.puml)")
         + md("Escopo total: os casos Must em cinza-claro, os Should em creme e o Won't (UC15) em cinza. "
              "Cada caso de uso numerado corresponde à história de mesmo número no backlog (UC01 → H01). "
              "\"Fazer login\" é um caso de apoio, incluído pelos casos do operador. A pessoa assistida é um morador "
              "(generalização) e herda os casos de uso dele."))
ag_sections = [
    ("Apresentação da ideia", md(ideia)),
    ("Personas", '<div class="note">' + md("**Em elaboração.** As personas serão escritas a partir das entrevistas com "
                                            "moradores e cuidadores das áreas de risco, sem dados pessoais reais.") + "</div>"),
    ("Diagrama de casos de uso", casos),
    ("Backlog do produto", md(backlog)),
    ("Planning Poker", md("Os Story Points do backlog são estimados pela equipe em Planning Poker, na sequência de Fibonacci.")
                       + "<h2>Como a sessão funciona</h2>" + md(("### Abertura" + poker_how).replace("## 2. Para cada história", "### Para cada história"))
                       + "<h2>Folha de votação</h2>" + md(poker_sheet)),
    ("Protótipo de telas", "".join(prot_html_parts)),
    ("Vídeo pitch", md("- **Link do vídeo (YouTube, não listado):** _a inserir_\n"
                       "- **Duração:** até 3 minutos, com narração dos três integrantes.\n"
                       "- **Conteúdo:** o problema, a solução e quem ela ajuda.")),
]
(OUT / "agile.html").write_text(page("Rota Segura — Agile Methodology", [
    cover("Agile Methodology", False),
    toc([t for t, _ in ag_sections]),
    *[(f'<div class="landscape">{section(f"{i}. {t}", b)}</div>' if t == "Diagrama de casos de uso"
       else section(f"{i}. {t}", b)) for i, (t, b) in enumerate(ag_sections, 1)],
]), encoding="utf-8")
print("ok", [p.name for p in OUT.glob("*.html")])
