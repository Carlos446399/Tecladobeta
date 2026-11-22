# Fontes verificadas: emojis, figurinhas e GIFs

Consulta em 22 de novembro de 2026.

- O catálogo oficial Unicode atualmente publica a lista completa como Emoji 18.0 e aponta os arquivos de dados versionados em `https://www.unicode.org/Public/18.0.0/emoji/`. O gráfico principal exclui a lista de modificadores de tom de pele; por isso a integração usa o arquivo oficial `emoji-test.txt`, que inclui sequências qualificadas e combinações em grupos/categorias. Fonte: https://unicode.org/emoji/charts/full-emoji-list.html
- A documentação do Tenor informa que não aceita novos clientes de API desde janeiro de 2026. A documentação existente requer chave e atribuição para resultados; não é uma base viável para uma nova integração sem uma chave/conta previamente aprovada. Fonte: https://developers.google.com/tenor/guides/quickstart
- A documentação do GIPHY permite pesquisa de GIFs e figurinhas, mas exige uma chave criada no painel de desenvolvedor; chaves beta são limitadas (100 consultas/hora), e a exibição precisa de marca visível “Powered By GIPHY”. A documentação recomenda endpoints de Trending/Search e registra ações para analytics. Fonte: https://developers.giphy.com/docs/

A lista offline de emojis disponibiliza as sequências Unicode no seletor, mas a forma visual dos caracteres mais novos ainda depende das fontes instaladas no Android do usuário. Conteúdo GIPHY é externo e a consulta envia o termo digitado ao provedor; não deve ocorrer sem busca explícita do usuário, aviso de privacidade e chave válida. Nenhuma chave foi incluída ou criada.
