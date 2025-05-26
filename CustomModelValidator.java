package org.adempiere.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MBPartner;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Model Validator Personalizado para ADempiere
 * Implementa validações de negócio para pedidos e linhas
 * 
 * @author Sistema
 * @version 1.0
 */
public class CustomModelValidator implements ModelValidator {
    
    /** Logger */
    private static CLogger log = CLogger.getCLogger(CustomModelValidator.class);
    
    /** Client */
    private int m_AD_Client_ID = -1;
    
    /**
     * Constructor
     */
    public CustomModelValidator() {
        super();
    }
    
    /**
     * Initialize Validation
     */
    public void initialize(ModelValidationEngine engine, MClient client) {
        // Client
        if (client != null) {
            m_AD_Client_ID = client.getAD_Client_ID();
            log.info(client.toString());
        } else {
            log.info("Initializing global validator: " + this.toString());
        }
        
        // Register for Table Events
        engine.addModelChange("C_Order", this);
        engine.addModelChange("C_OrderLine", this);
        engine.addModelChange("C_BPartner", this);
        
        // Register for Document Events  
        engine.addDocValidate("C_Order", this);
    }
    
    /**
     * Model Change
     */
    public String modelChange(PO po, int type) throws Exception {
        log.info(po.get_TableName() + " Type: " + type);
        
        // Validações para C_Order
        if (po.get_TableName().equals("C_Order")) {
            return validateOrder(po, type);
        }
        
        // Validações para C_OrderLine
        if (po.get_TableName().equals("C_OrderLine")) {
            return validateOrderLine(po, type);
        }
        
        // Validações para C_BPartner
        if (po.get_TableName().equals("C_BPartner")) {
            return validateBPartner(po, type);
        }
        
        return null;
    }
    
    /**
     * Document Validation
     */
    public String docValidate(PO po, int timing) {
        log.info(po.get_TableName() + " Timing: " + timing);
        
        if (po.get_TableName().equals("C_Order")) {
            return validateOrderDocument(po, timing);
        }
        
        return null;
    }
    
    /**
     * User Login
     */
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
        log.info("AD_User_ID=" + AD_User_ID);
        return null;
    }
    
    /**
     * Get Client
     */
    public int getAD_Client_ID() {
        return m_AD_Client_ID;
    }
    
    /**
     * Validação de pedidos
     */
    private String validateOrder(PO po, int type) {
        MOrder order = (MOrder) po;
        
        // Validação BEFORE_NEW
        if (type == TYPE_BEFORE_NEW) {
            // Verificar se o parceiro de negócio está ativo
            if (order.getC_BPartner_ID() > 0) {
                MBPartner bp = new MBPartner(po.getCtx(), order.getC_BPartner_ID(), po.get_TrxName());
                if (!bp.isActive()) {
                    return "Parceiro de negócio não está ativo";
                }
                if (!bp.isCustomer()) {
                    return "Parceiro de negócio não é um cliente";
                }
            }
        }
        
        // Validação BEFORE_CHANGE
        if (type == TYPE_BEFORE_CHANGE) {
            // Verificar se o total não excede o limite de crédito
            if (order.is_ValueChanged("GrandTotal")) {
                BigDecimal grandTotal = order.getGrandTotal();
                if (grandTotal != null && grandTotal.compareTo(new BigDecimal("100000")) > 0) {
                    return "Valor do pedido excede o limite máximo permitido (R$ 100.000,00)";
                }
            }
            
            // Verificar alteração de data
            if (order.is_ValueChanged("DateOrdered")) {
                Timestamp dateOrdered = order.getDateOrdered();
                Timestamp today = new Timestamp(System.currentTimeMillis());
                if (dateOrdered.before(today)) {
                    return "Data do pedido não pode ser anterior à data atual";
                }
            }
        }
        
        // Validação BEFORE_DELETE
        if (type == TYPE_BEFORE_DELETE) {
            // Não permitir deletar pedidos processados
            if (order.isProcessed()) {
                return "Não é possível deletar pedidos já processados";
            }
        }
        
        return null;
    }
    
    /**
     * Validação de linhas de pedido
     */
    private String validateOrderLine(PO po, int type) {
        MOrderLine orderLine = (MOrderLine) po;
        
        // Validação BEFORE_NEW e BEFORE_CHANGE
        if (type == TYPE_BEFORE_NEW || type == TYPE_BEFORE_CHANGE) {
            // Verificar quantidade mínima
            BigDecimal qty = orderLine.getQtyEntered();
            if (qty != null && qty.compareTo(Env.ZERO) <= 0) {
                return "Quantidade deve ser maior que zero";
            }
            
            // Verificar preço
            BigDecimal price = orderLine.getPriceEntered();
            if (price != null && price.compareTo(Env.ZERO) < 0) {
                return "Preço não pode ser negativo";
            }
            
            // Verificar desconto máximo
            BigDecimal discount = orderLine.getDiscount();
            if (discount != null && discount.compareTo(new BigDecimal("50")) > 0) {
                return "Desconto não pode ser superior a 50%";
            }
        }
        
        return null;
    }
    
    /**
     * Validação de parceiros de negócio
     */
    private String validateBPartner(PO po, int type) {
        MBPartner bp = (MBPartner) po;
        
        // Validação BEFORE_NEW e BEFORE_CHANGE
        if (type == TYPE_BEFORE_NEW || type == TYPE_BEFORE_CHANGE) {
            // Verificar se o nome não está vazio
            String name = bp.getName();
            if (name == null || name.trim().length() == 0) {
                return "Nome do parceiro de negócio é obrigatório";
            }
            
            // Verificar duplicação de nome
            if (type == TYPE_BEFORE_NEW || bp.is_ValueChanged("Name")) {
                String sql = "SELECT COUNT(*) FROM C_BPartner WHERE Name = ? AND C_BPartner_ID != ?";
                int count = DB.getSQLValue(po.get_TrxName(), sql, name, bp.getC_BPartner_ID());
                if (count > 0) {
                    return "Já existe um parceiro de negócio com este nome";
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validação de documento de pedido
     */
    private String validateOrderDocument(PO po, int timing) {
        MOrder order = (MOrder) po;
        
        // Validação antes de completar
        if (timing == TIMING_BEFORE_COMPLETE) {
            // Verificar se tem linhas
            MOrderLine[] lines = order.getLines();
            if (lines == null || lines.length == 0) {
                return "Pedido deve ter pelo menos uma linha";
            }
            
            // Verificar limite de crédito do cliente
            MBPartner bp = new MBPartner(po.getCtx(), order.getC_BPartner_ID(), po.get_TrxName());
            BigDecimal creditLimit = bp.getSO_CreditLimit();
            BigDecimal creditUsed = bp.getSO_CreditUsed();
            BigDecimal grandTotal = order.getGrandTotal();
            
            if (creditLimit != null && creditLimit.compareTo(Env.ZERO) > 0) {
                BigDecimal newCreditUsed = creditUsed.add(grandTotal);
                if (newCreditUsed.compareTo(creditLimit) > 0) {
                    return "Pedido excede o limite de crédito do cliente. " +
                           "Limite: " + creditLimit + ", Usado: " + creditUsed + 
                           ", Novo total: " + newCreditUsed;
                }
            }
            
            // Verificar estoque para produtos
            for (MOrderLine line : lines) {
                if (line.getM_Product_ID() > 0) {
                    String stockValidation = validateStock(line);
                    if (stockValidation != null) {
                        return stockValidation;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validação de estoque
     */
    private String validateStock(MOrderLine line) {
        try {
            String sql = "SELECT QtyOnHand FROM M_Storage " +
                        "WHERE M_Product_ID = ? AND M_Locator_ID IN " +
                        "(SELECT M_Locator_ID FROM M_Locator WHERE M_Warehouse_ID = ?)";
            
            BigDecimal qtyOnHand = DB.getSQLValueBD(line.get_TrxName(), sql, 
                                                   line.getM_Product_ID(), 
                                                   line.getM_Warehouse_ID());
            
            if (qtyOnHand == null) {
                qtyOnHand = Env.ZERO;
            }
            
            if (line.getQtyEntered().compareTo(qtyOnHand) > 0) {
                return "Quantidade solicitada (" + line.getQtyEntered() + 
                       ") excede o estoque disponível (" + qtyOnHand + ") " +
                       "para o produto: " + line.getProduct().getName();
            }
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro ao validar estoque: " + e.getMessage(), e);
            return "Erro ao validar estoque: " + e.getMessage();
        }
        
        return null;
    }
}