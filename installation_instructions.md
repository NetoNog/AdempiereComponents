# Instruções de Instalação - Componentes ADempiere

## 📋 Pré-requisitos

- ADempiere versão 3.9.0 ou superior
- Acesso ao banco de dados PostgreSQL/Oracle
- Permissões de System Administrator
- Ambiente de desenvolvimento Java configurado

## 🔧 Instalação dos Componentes

### 1. Callout (CalloutCustom.java)

**Compilação:**
''bash
# Compilar a classe
javac -cp $ADEMPIERE_HOME/lib/*:$ADEMPIERE_HOME/packages/* CalloutCustom.java

# Copiar para o diretório de classes
cp CalloutCustom.class $ADEMPIERE_HOME/packages/org/compiere/model/


### 🎯 **Como Funciona na Prática**

### Fluxo de um Pedido:

1. **Usuário abre pedido** → Callout valida cliente
2. **Adiciona produto** → Callout verifica se ativo
3. **Digita quantidade** → Callout calcula total e desconto
4. **Salva linha** → Validator verifica regras de negócio
5. **Finaliza pedido** → Validator verifica crédito e estoque
6. **Processo noturno** → Atualiza preços automaticamente

Exemplo Real:
Usuário: Seleciona produto "Notebook Dell"
Callout: ✓ Produto ativo, preenche nome automaticamente

Usuário: Digita quantidade "15"  
Callout: ✓ Aplica 5% desconto (qty ≥ 10)
Callout: ✓ Calcula total: 15 × R$ 2.000 = R$ 30.000

Usuário: Salva pedido
Validator: ✓ Verifica limite crédito cliente
Validator: ✓ Verifica estoque disponível
Validator: ✓ Pedido salvo com sucesso

### Vantagens:

- **Automação**: Cálculos automáticos, menos erros
- **Validação**: Impede dados incorretos
- **Eficiência**: Processos em lote
- **Controle**: Regras de negócio centralizadas


Cada componente trabalha em conjunto para criar um sistema robusto e automatizado! 🚀
