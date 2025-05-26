-- Script SQL para criar janela personalizada no ADempiere
-- Exemplo: Janela de Controle de Estoque

-- =====================================================
-- 1. CRIAR WINDOW
-- =====================================================

INSERT INTO AD_Window (
    AD_Window_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, WindowType, IsBetaFunctionality, IsDefault, WinHeight, WinWidth,
    EntityType, Processing
) VALUES (
    nextval('ad_window_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Controle de Estoque Personalizado', 
    'Janela para controle avançado de estoque com relatórios', 
    'Esta janela permite visualizar e gerenciar o estoque de produtos com funcionalidades avançadas de relatório e análise.',
    'M', 'N', 'N', 600, 800,
    'U', 'N'
);

-- Obter ID da window criada (substitua pelo ID real após execução)
-- SELECT currval('ad_window_seq') as window_id;

-- =====================================================
-- 2. CRIAR TABS
-- =====================================================

-- Tab Principal: Produtos
INSERT INTO AD_Tab (
    AD_Tab_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Table_ID, AD_Window_ID, SeqNo, TabLevel, IsSingleRow, 
    IsInfoTab, IsTranslationTab, IsReadOnly, HasTree, WhereClause, OrderByClause, 
    CommitWarning, Processing, EntityType
) VALUES (
    nextval('ad_tab_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Produtos', 'Lista de produtos para controle de estoque', 
    'Visualize e edite informações dos produtos',
    (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_Product'),
    currval('ad_window_seq'), 10, 0, 'N', 'N', 'N', 'N', 'N', 
    'IsActive=''Y'' AND ProductType=''I''', 'Name', 
    NULL, 'N', 'U'
);

-- Tab de Estoque
INSERT INTO AD_Tab (
    AD_Tab_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Table_ID, AD_Window_ID, SeqNo, TabLevel, IsSingleRow, 
    IsInfoTab, IsTranslationTab, IsReadOnly, HasTree, WhereClause, OrderByClause, 
    CommitWarning, Processing, EntityType
) VALUES (
    nextval('ad_tab_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Estoque Atual', 'Visualização do estoque atual por localização', 
    'Mostra as quantidades disponíveis em cada localização',
    (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_Storage'),
    currval('ad_window_seq'), 20, 1, 'N', 'N', 'N', 'Y', 'N', 
    'QtyOnHand <> 0', 'QtyOnHand DESC', 
    NULL, 'N', 'U'
);

-- Tab de Movimentações
INSERT INTO AD_Tab (
    AD_Tab_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Table_ID, AD_Window_ID, SeqNo, TabLevel, IsSingleRow, 
    IsInfoTab, IsTranslationTab, IsReadOnly, HasTree, WhereClause, OrderByClause, 
    CommitWarning, Processing, EntityType
) VALUES (
    nextval('ad_tab_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Movimentações', 'Histórico de movimentações de estoque', 
    'Visualize o histórico completo de movimentações',
    (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_Transaction'),
    currval('ad_window_seq'), 30, 1, 'N', 'N', 'N', 'Y', 'N', 
    NULL, 'MovementDate DESC, Created DESC', 
    NULL, 'N', 'U'
);

-- =====================================================
-- 3. CRIAR CAMPOS AUTOMATICAMENTE
-- =====================================================

-- Campos para Tab Produtos
INSERT INTO AD_Field (
    AD_Field_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Column_ID, AD_Tab_ID, IsDisplayed, DisplayLogic, 
    DisplayLength, SeqNo, SortNo, IsSameLine, IsHeading, IsFieldOnly, IsReadOnly, 
    IsEncrypted, EntityType
)
SELECT 
    nextval('ad_field_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    c.Name, c.Description, c.Help, c.AD_Column_ID, 
    (SELECT AD_Tab_ID FROM AD_Tab WHERE Name = 'Produtos' AND AD_Window_ID = currval('ad_window_seq')),
    CASE 
        WHEN c.ColumnName IN ('M_Product_ID', 'Created', 'CreatedBy', 'Updated', 'UpdatedBy', 'AD_Client_ID', 'AD_Org_ID') THEN 'N'
        WHEN c.ColumnName IN ('Value', 'Name', 'M_Product_Category_ID', 'UOMType', 'ProductType', 'IsActive') THEN 'Y'
        WHEN c.ColumnName IN ('Description', 'Help', 'UPC', 'SKU') THEN 'Y'
        ELSE 'N' 
    END,
    NULL, c.FieldLength, 
    CASE 
        WHEN c.ColumnName = 'Value' THEN 10
        WHEN c.ColumnName = 'Name' THEN 20
        WHEN c.ColumnName = 'M_Product_Category_ID' THEN 30
        WHEN c.ColumnName = 'ProductType' THEN 40
        WHEN c.ColumnName = 'UOMType' THEN 50
        WHEN c.ColumnName = 'IsActive' THEN 60
        WHEN c.ColumnName = 'Description' THEN 70
        WHEN c.ColumnName = 'Help' THEN 80
        WHEN c.ColumnName = 'UPC' THEN 90
        WHEN c.ColumnName = 'SKU' THEN 100
        ELSE (ROW_NUMBER() OVER (ORDER BY c.ColumnName)) * 10 + 100
    END, 
    0,
    CASE 
        WHEN c.ColumnName IN ('Name', 'Description', 'Help') THEN 'N'
        ELSE 'Y' 
    END,
    'N', 'N', 
    CASE 
        WHEN c.ColumnName IN ('M_Product_ID', 'Created', 'CreatedBy', 'Updated', 'UpdatedBy') THEN 'Y'
        ELSE 'N' 
    END,
    'N', 'U'
FROM AD_Column c
WHERE c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_Product')
  AND c.IsActive = 'Y'
  AND c.ColumnName NOT IN ('Discontinued', 'DiscontinuedBy', 'ImageURL', 'DescriptionURL');

-- Campos para Tab Estoque
INSERT INTO AD_Field (
    AD_Field_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Column_ID, AD_Tab_ID, IsDisplayed, DisplayLogic, 
    DisplayLength, SeqNo, SortNo, IsSameLine, IsHeading, IsFieldOnly, IsReadOnly, 
    IsEncrypted, EntityType
)
SELECT 
    nextval('ad_field_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    c.Name, c.Description, c.Help, c.AD_Column_ID, 
    (SELECT AD_Tab_ID FROM AD_Tab WHERE Name = 'Estoque Atual' AND AD_Window_ID = currval('ad_window_seq')),
    CASE 
        WHEN c.ColumnName IN ('M_Storage_ID', 'Created', 'CreatedBy', 'Updated', 'UpdatedBy', 'AD_Client_ID', 'AD_Org_ID') THEN 'N'
        ELSE 'Y' 
    END,
    NULL, c.FieldLength, 
    CASE 
        WHEN c.ColumnName = 'M_Product_ID' THEN 10
        WHEN c.ColumnName = 'M_Locator_ID' THEN 20
        WHEN c.ColumnName = 'QtyOnHand' THEN 30
        WHEN c.ColumnName = 'QtyReserved' THEN 40
        WHEN c.ColumnName = 'QtyOrdered' THEN 50
        WHEN c.ColumnName = 'DateLastInventory' THEN 60
        ELSE (ROW_NUMBER() OVER (ORDER BY c.ColumnName)) * 10 + 60
    END, 
    0, 'Y', 'N', 'N', 'Y', 'N', 'U'
FROM AD_Column c
WHERE c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_Storage')
  AND c.IsActive = 'Y';

-- Campos para Tab Movimentações
INSERT INTO AD_Field (
    AD_Field_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Column_ID, AD_Tab_ID, IsDisplayed, DisplayLogic, 
    DisplayLength, SeqNo, SortNo, IsSameLine, IsHeading, IsFieldOnly, IsReadOnly, 
    IsEncrypted, EntityType
)
SELECT 
    nextval('ad_field_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    c.Name, c.Description, c.Help, c.AD_Column_ID, 
    (SELECT AD_Tab_ID FROM AD_Tab WHERE Name = 'Movimentações' AND AD_Window_ID = currval('ad_window_seq')),
    CASE 
        WHEN c.ColumnName IN ('M_Transaction_ID', 'AD_Client_ID', 'AD_Org_ID') THEN 'N'
        ELSE 'Y' 
    END,
    NULL, c.FieldLength, 
    CASE 
        WHEN c.ColumnName = 'MovementDate' THEN 10
        WHEN c.ColumnName = 'M_Product_ID' THEN 20
        WHEN c.ColumnName = 'M_Locator_ID' THEN 30
        WHEN c.ColumnName = 'MovementType' THEN 40
        WHEN c.ColumnName = 'MovementQty' THEN 50
        WHEN c.ColumnName = 'M_InOutLine_ID' THEN 60
        WHEN c.ColumnName = 'M_InventoryLine_ID' THEN 70
        WHEN c.ColumnName = 'M_MovementLine_ID' THEN 80
        WHEN c.ColumnName = 'Created' THEN 90
        WHEN c.ColumnName = 'CreatedBy' THEN 100
        ELSE (ROW_NUMBER() OVER (ORDER BY c.ColumnName)) * 10 + 100
    END, 
    0, 'Y', 'N', 'N', 'Y', 'N', 'U'
FROM AD_Column c
WHERE c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'M_Transaction')
  AND c.IsActive = 'Y';

-- =====================================================
-- 4. CRIAR MENU ITEM
-- =====================================================

INSERT INTO AD_Menu (
    AD_Menu_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, IsSummary, IsSOTrx, IsReadOnly, Action, AD_Window_ID, EntityType
) VALUES (
    nextval('ad_menu_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Controle de Estoque Personalizado', 
    'Janela para controle avançado de estoque com relatórios', 
    'N', 'Y', 'N', 'W', currval('ad_window_seq'), 'U'
);

-- =====================================================
-- 5. CRIAR PROCESSO PERSONALIZADO (OPCIONAL)
-- =====================================================

INSERT INTO AD_Process (
    AD_Process_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Value, Name, Description, Help, AccessLevel, EntityType, ProcedureName, IsReport,
    IsDirectPrint, Classname, Statistic_Count, Statistic_Seconds, WorkflowValue
) VALUES (
    nextval('ad_process_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'CustomStockReport', 'Relatório de Estoque Personalizado',
    'Gera relatório detalhado de estoque com análises',
    'Este processo gera um relatório completo do estoque atual com análises de giro e sugestões de reposição',
    '3', 'U', NULL, 'N', 'N', 'org.compiere.process.CustomProcess', 0, 0, NULL
);

-- Parâmetros do processo
INSERT INTO AD_Process_Para (
    AD_Process_Para_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, Description, Help, AD_Process_ID, SeqNo, AD_Reference_ID, AD_Reference_Value_ID,
    AD_Val_Rule_ID, ColumnName, IsCentrallyMaintained, FieldLength, IsMandatory, IsRange,
    DefaultValue, DefaultValue2, VFormat, ValueMin, ValueMax, EntityType
) VALUES 
(nextval('ad_process_para_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
 'Categoria do Produto', 'Filtrar por categoria de produto', NULL,
 currval('ad_process_seq'), 10, 19, 
 (SELECT AD_Reference_ID FROM AD_Reference WHERE Name = 'M_Product_Category'),
 NULL, 'M_Product_Category_ID', 'Y', 10, 'N', 'N', NULL, NULL, NULL, NULL, NULL, 'U'),

(nextval('ad_process_para_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
 'Data de Referência', 'Data para análise do estoque', NULL,
 currval('ad_process_seq'), 20, 15, NULL, NULL, 'DateRef', 'Y', 7, 'Y', 'N', 
 '@#Date@', NULL, NULL, NULL, NULL, 'U'),

(nextval('ad_process_para_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
 'Incluir Produtos Inativos', 'Incluir produtos inativos no relatório', NULL,
 currval('ad_process_seq'), 30, 20, NULL, NULL, 'IncludeInactive', 'Y', 1, 'N', 'N', 
 'N', NULL, NULL, NULL, NULL, 'U');

-- =====================================================
-- 6. ADICIONAR BOTÃO DO PROCESSO NA JANELA
-- =====================================================

INSERT INTO AD_ToolBarButton (
    AD_ToolBarButton_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    Name, AD_Tab_ID, AD_Process_ID, SeqNo, EntityType
) VALUES (
    nextval('ad_toolbarbutton_seq'), 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'Relatório de Estoque',
    (SELECT AD_Tab_ID FROM AD_Tab WHERE Name = 'Produtos' AND AD_Window_ID = currval('ad_window_seq')),
    currval('ad_process_seq'), 10, 'U'
);

-- =====================================================
-- 7. CONFIGURAR PERMISSÕES (ROLE SYSTEM ADMINISTRATOR)
-- =====================================================

-- Permissão para Window
INSERT INTO AD_Window_Access (
    AD_Window_ID, AD_Role_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    IsReadWrite
) VALUES (
    currval('ad_window_seq'), 
    (SELECT AD_Role_ID FROM AD_Role WHERE Name = 'System Administrator'),
    0, 0, 'Y', NOW(), 100, NOW(), 100, 'Y'
);

-- Permissão para Process
INSERT INTO AD_Process_Access (
    AD_Process_ID, AD_Role_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
    IsReadWrite
) VALUES (
    currval('ad_process_seq'),
    (SELECT AD_Role_ID FROM AD_Role WHERE Name = 'System Administrator'),
    0, 0, 'Y', NOW(), 100, NOW(), 100, 'Y'
);

-- =====================================================
-- 8. FINALIZAR
-- =====================================================

COMMIT;

-- Mensagens de confirmação
SELECT 'Janela criada com ID: ' || currval('ad_window_seq') as resultado;
SELECT 'Execute "Synchronize Terminology" no Application Dictionary para ativar as mudanças' as instrucao;