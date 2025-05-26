package org.compiere.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Callout Personalizado para ADempiere
 * Exemplo de implementação para cálculos automáticos e validações
 * 
 * @author Sistema
 * @version 1.0
 */
public class CalloutCustom extends CalloutEngine {
    
    /**
     * Callout para cálculo de total de linha em pedidos
     * Chamado quando Qty ou PriceEntered são alterados
     * 
     * @param ctx Contexto
     * @param WindowNo Número da janela
     * @param mTab Tab do grid
     * @param mField Campo do grid
     * @param value Valor atual
     * @return Mensagem de erro ou null se OK
     */
    public String calculateLineTotal(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
        
        if (isCalloutActive() || value == null)
            return "";
            
        // Obter valores dos campos
        BigDecimal qty = (BigDecimal) mTab.getValue("QtyEntered");
        BigDecimal price = (BigDecimal) mTab.getValue("PriceEntered");
        Integer taxId = (Integer) mTab.getValue("C_Tax_ID");
        
        try {
            // Validar quantidade
            if (qty != null && qty.compareTo(Env.ZERO) <= 0) {
                return "Quantidade deve ser maior que zero";
            }
            
            // Validar preço
            if (price != null && price.compareTo(Env.ZERO) < 0) {
                return "Preço não pode ser negativo";
            }
            
            // Calcular total da linha
            if (qty != null && price != null) {
                BigDecimal lineNetAmt = qty.multiply(price);
                mTab.setValue("LineNetAmt", lineNetAmt);
                
                // Calcular imposto se definido
                if (taxId != null && taxId > 0) {
                    BigDecimal taxRate = getTaxRate(taxId);
                    if (taxRate != null) {
                        BigDecimal taxAmt = lineNetAmt.multiply(taxRate).divide(Env.ONEHUNDRED, 2, BigDecimal.ROUND_HALF_UP);
                        mTab.setValue("TaxAmt", taxAmt);
                        mTab.setValue("LineTotalAmt", lineNetAmt.add(taxAmt));
                    }
                } else {
                    mTab.setValue("TaxAmt", Env.ZERO);
                    mTab.setValue("LineTotalAmt", lineNetAmt);
                }
            }
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro no callout calculateLineTotal: " + e.getMessage(), e);
            return "Erro interno: " + e.getMessage();
        }
        
        return "";
    }
    
    /**
     * Callout para validação de produto
     * Verifica se o produto está ativo e disponível
     */
    public String validateProduct(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
        
        if (isCalloutActive() || value == null)
            return "";
            
        Integer productId = (Integer) value;
        
        try {
            String sql = "SELECT IsActive, IsSold, Name FROM M_Product WHERE M_Product_ID = ?";
            PreparedStatement pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String isActive = rs.getString("IsActive");
                String isSold = rs.getString("IsSold");
                String productName = rs.getString("Name");
                
                if (!"Y".equals(isActive)) {
                    return "Produto " + productName + " não está ativo";
                }
                
                if (!"Y".equals(isSold)) {
                    return "Produto " + productName + " não está disponível para venda";
                }
                
                // Atualizar nome do produto no campo
                mTab.setValue("ProductName", productName);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro no callout validateProduct: " + e.getMessage(), e);
            return "Erro ao validar produto: " + e.getMessage();
        }
        
        return "";
    }
    
    /**
     * Callout para cálculo de desconto
     * Aplica desconto baseado na quantidade ou cliente
     */
    public String calculateDiscount(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
        
        if (isCalloutActive() || value == null)
            return "";
            
        try {
            BigDecimal qty = (BigDecimal) mTab.getValue("QtyEntered");
            Integer bPartnerId = (Integer) mTab.getValue("C_BPartner_ID");
            BigDecimal priceList = (BigDecimal) mTab.getValue("PriceList");
            
            BigDecimal discount = Env.ZERO;
            
            // Desconto por quantidade
            if (qty != null && qty.compareTo(new BigDecimal("10")) >= 0) {
                discount = new BigDecimal("5"); // 5% desconto para qty >= 10
            }
            if (qty != null && qty.compareTo(new BigDecimal("50")) >= 0) {
                discount = new BigDecimal("10"); // 10% desconto para qty >= 50
            }
            
            // Desconto adicional para clientes especiais
            if (bPartnerId != null && isSpecialCustomer(bPartnerId)) {
                discount = discount.add(new BigDecimal("5")); // +5% para clientes especiais
            }
            
            // Aplicar desconto
            if (discount.compareTo(Env.ZERO) > 0 && priceList != null) {
                BigDecimal discountAmt = priceList.multiply(discount).divide(Env.ONEHUNDRED, 2, BigDecimal.ROUND_HALF_UP);
                BigDecimal priceEntered = priceList.subtract(discountAmt);
                
                mTab.setValue("Discount", discount);
                mTab.setValue("PriceEntered", priceEntered);
            }
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro no callout calculateDiscount: " + e.getMessage(), e);
            return "Erro ao calcular desconto: " + e.getMessage();
        }
        
        return "";
    }
    
    /**
     * Método auxiliar para obter taxa de imposto
     */
    private BigDecimal getTaxRate(int taxId) {
        String sql = "SELECT Rate FROM C_Tax WHERE C_Tax_ID = ?";
        BigDecimal rate = null;
        
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, taxId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                rate = rs.getBigDecimal("Rate");
            }
            
            rs.close();
            pstmt.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro ao obter taxa de imposto: " + e.getMessage(), e);
        }
        
        return rate;
    }
    
    /**
     * Método auxiliar para verificar se é cliente especial
     */
    private boolean isSpecialCustomer(int bPartnerId) {
        String sql = "SELECT IsCustomer FROM C_BPartner WHERE C_BPartner_ID = ? AND SO_CreditLimit > 10000";
        boolean isSpecial = false;
        
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, bPartnerId);
            ResultSet rs = pstmt.executeQuery();
            
            isSpecial = rs.next();
            
            rs.close();
            pstmt.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro ao verificar cliente especial: " + e.getMessage(), e);
        }
        
        return isSpecial;
    }
}