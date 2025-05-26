**Configuração no Application Dictionary:**

1. Acesse **Application Dictionary > Table and Column**
2. Encontre a tabela desejada (ex: C_OrderLine)
3. Selecione a coluna que deve disparar o callout (ex: QtyEntered)
4. No campo **Callout**, insira: `org.compiere.model.CalloutCustom.calculateLineTotal`
5. Salve e sincronize o Application Dictionary


**Exemplos de configuração:**

- Para cálculo de total: `CalloutCustom.calculateLineTotal`
- Para validação de produto: `CalloutCustom.validateProduct`
- Para cálculo de desconto: `CalloutCustom.calculateDiscount`


### 2. Model Validator (CustomModelValidator.java)

**Compilação:**

# Compilar a classe
javac -cp $ADEMPIERE_HOME/lib/*:$ADEMPIERE_HOME/packages/* CustomModelValidator.java

# Copiar para o diretório de classes
cp CustomModelValidator.class $ADEMPIERE_HOME/packages/org/adempiere/model/

**Configuração no Application Dictionary:**

1. Acesse **Application Dictionary > Model Validator**
2. Crie um novo registro:

1. **Name**: Custom Model Validator
2. **Model Validation Class**: `org.adempiere.model.CustomModelValidator`
3. **Entity Type**: U (User)
4. **Sequence**: 100



3. Marque como **Active**
4. Reinicie o servidor de aplicação


### 3. Process (CustomProcess.java)

**Compilação:**
# Compilar a classe
javac -cp $ADEMPIERE_HOME/lib/*:$ADEMPIERE_HOME/packages/* CustomProcess.java

# Copiar para o diretório de classes
cp CustomProcess.class $ADEMPIERE_HOME/packages/org/compiere/process/

**Configuração no Application Dictionary:**

1. Acesse **Application Dictionary > Process**
2. Crie um novo processo:

1. **Value**: CustomPriceUpdate
2. **Name**: Atualização de Preços Personalizada
3. **Classname**: `org.compiere.process.CustomProcess`
4. **Entity Type**: U



3. Configure os parâmetros na aba **Parameter**:

1. M_Product_Category_ID (Table Direct)
2. PriceAdjustment (Number)
3. AdjustmentType (List: P=Percentage, A=Amount)
4. DateFrom/DateTo (Date)
5. IsActive (Yes/No)



4. Crie um menu item para acessar o processo


### 4. Window (create_custom_window.sql)

**Execução do Script:**
-- Conecte-se ao banco como usuário adempiere
psql -U adempiere -d adempiere -f create_custom_window.sql

-- Ou execute via pgAdmin/SQL Developer

**Pós-instalação:**

1. Acesse o Application Dictionary como System Administrator
2. Execute **Synchronize Terminology**
3. Verifique se a janela foi criada corretamente
4. Configure permissões nos roles apropriados
5. Teste a janela no cliente ZK ou Swing


## 🔍 Verificação da Instalação

### Verificar Callouts
SELECT t.TableName, c.ColumnName, c.Callout 
FROM AD_Column c 
JOIN AD_Table t ON c.AD_Table_ID = t.AD_Table_ID 
WHERE c.Callout LIKE '%CalloutCustom%';

### Verificar Model Validators
SELECT Name, ModelValidationClass, IsActive 
FROM AD_ModelValidator 
WHERE ModelValidationClass LIKE '%CustomModelValidator%';

### Verificar Processos
SELECT Value, Name, Classname, IsActive 
FROM AD_Process 
WHERE Classname LIKE '%CustomProcess%';

### Verificar Janelas
SELECT Name, Description, WindowType 
FROM AD_Window 
WHERE Name LIKE '%Controle de Estoque%';

## 🚨 Troubleshooting

### Problemas Comuns

**1. ClassNotFoundException**

- Verifique se as classes foram compiladas corretamente
- Confirme se estão no classpath correto
- Reinicie o servidor de aplicação


**2. Callout não executa**

- Verifique a sintaxe no campo Callout
- Confirme se a classe e método existem
- Verifique logs do servidor para erros


**3. Validator não funciona**

- Confirme se o validator está ativo
- Verifique se foi registrado corretamente
- Reinicie o servidor após instalação


**4. Processo não aparece**

- Verifique permissões do role
- Confirme se o menu foi criado
- Execute Synchronize Terminology


### Logs para Debug

**Localização dos logs:**
# Logs do servidor
tail -f $ADEMPIERE_HOME/log/adempiere_server.log

# Logs específicos
grep -i "CalloutCustom\|CustomModelValidator\|CustomProcess" $ADEMPIERE_HOME/log/*.log

## 📝 Customização

### Modificar Callouts

1. Edite o arquivo `CalloutCustom.java`
2. Recompile e substitua a classe
3. Reinicie o servidor (se necessário)


### Adicionar Validações

1. Edite `CustomModelValidator.java`
2. Adicione novos métodos de validação
3. Registre novas tabelas no método `initialize()`
4. Recompile e reinicie o servidor


### Estender Processos

1. Adicione novos métodos em `CustomProcess.java`
2. Crie novos processos no Application Dictionary
3. Configure parâmetros conforme necessário


## 🔒 Segurança

### Permissões Recomendadas

- Conceda acesso apenas aos roles necessários
- Teste em ambiente de desenvolvimento primeiro
- Mantenha backup antes de aplicar em produção


### Auditoria

- Monitore logs de execução
- Verifique performance dos callouts
- Acompanhe validações rejeitadas


## 📚 Documentação Adicional

- [ADempiere Wiki](http://wiki.adempiere.net/)
- [Javadoc ADempiere](http://javadoc.adempiere.net/)
- [Forum ADempiere](http://www.adempiere.com/forum/)


## 🆘 Suporte

Para suporte adicional:

1. Consulte a documentação oficial do ADempiere
2. Acesse o fórum da comunidade
3. Verifique os logs detalhados
4. Teste em ambiente isolado antes da produção
