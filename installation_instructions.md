# Instruções de Instalação - Componentes ADempiere

## 📋 Pré-requisitos

- ADempiere versão 3.9.0 ou superior
- Acesso ao banco de dados PostgreSQL/Oracle
- Permissões de System Administrator
- Ambiente de desenvolvimento Java configurado

## 🔧 Instalação dos Componentes

### 1. Callout (CalloutCustom.java)

**Compilação:**
```bash
# Compilar a classe
javac -cp $ADEMPIERE_HOME/lib/*:$ADEMPIERE_HOME/packages/* CalloutCustom.java

# Copiar para o diretório de classes
cp CalloutCustom.class $ADEMPIERE_HOME/packages/org/compiere/model/