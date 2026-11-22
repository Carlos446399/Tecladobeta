# Configuração opcional de GIFs e figurinhas GIPHY

O seletor de mídia continua offline até a pessoa tocar em **Buscar** ou **Em alta**. A consulta envia somente o termo digitado para a GIPHY. O app desativa essas consultas em campos seguros e no modo anônimo, mostra a atribuição exigida (**Powered by GIPHY**) e limita os resultados ao conteúdo com classificação `G`.

Para ativar resultados ao compilar o APK:

1. Crie uma chave Android no painel de desenvolvedor da GIPHY e confirme que a conta/chave pode ser usada para este aplicativo. A documentação consultada informa que as chaves beta têm limite de 100 consultas por hora; publicação comercial pode exigir revisão ou outra autorização.
2. No `local.properties` ignorado pelo Git, acrescente `GIPHY_API_KEY=sua_chave`. Não inclua o valor em commits, mensagens, capturas de tela ou APKs públicos.
3. Gere um novo APK. A chave do provedor será compilada dentro do aplicativo, como ocorre com qualquer chave de cliente móvel; portanto, ela não é um segredo forte. Acompanhe o limite de uso e substitua/restrinja a chave no painel da GIPHY se necessário.

Sem a chave, a aba mantém o catálogo local de figurinhas e mostra instruções, sem fazer solicitações externas. O catálogo de emojis Unicode continua disponível offline.

## Fontes

- GIPHY API: https://developers.giphy.com/docs/
- Documentação de Tenor: https://developers.google.com/tenor/guides/quickstart (a página informa que não aceita novos clientes de API desde janeiro de 2026).
