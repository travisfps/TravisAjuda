# 🆘 AjudaPlugin - Sistema de Suporte In-Game

Plugin leve e funcional para servidores Minecraft, permitindo que jogadores enviem dúvidas pelo comando `/ajuda` e que a staff responda por um painel interativo.

---

## ✅ Recursos

- GUI interativa para dúvidas dos jogadores
- Painel exclusivo para staff visualizar e responder
- Cabeças personalizadas ou materiais customizáveis
- Sons e mensagens personalizáveis
- Anti-conflito: só um staff responde por vez
- Compatível com versões **1.8.x**

---

## 💻 Comandos

| Comando         | Permissão     | Função                           |
|-----------------|---------------|----------------------------------|
| `/ajuda`        | -             | Jogador envia uma dúvida         |
| `/ajuda painel` | ajuda.staff   | Staff acessa o painel de dúvidas |

---

## ⚙️ Exemplo de `config.yml`

```yaml
menu:
  titulo: "&8Menu de Ajuda"
  tamanho: 9
  itens:
    ajuda:
      slot: 13
      CustomSkull: true
      URL: "d24892a3142d2e130e5feb88b805b83de905489d2ccd1d031b9d7a2922b96500"
      nome: "&aPedir Ajuda"
      lore:
        - "&7Clique para enviar sua dúvida"
        - "&7Você: &f%jogador%"
      material: PAPER

painel:
  titulo: "&cPainel de Dúvidas"
  tamanho: 27
  item_duvida:
    nome: "&c%duvida%"
    lore:
      - "&7Clique para responder essa dúvida"
```

---

## 🧪 Compatibilidade

Testado e funcionando em:

- ✅ 1.8.x 
