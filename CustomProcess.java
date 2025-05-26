package org.compiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Processo Personalizado para ADempiere
 * Exemplo: Processo de atualização em lote de preços de produtos
 * 
 * @author Sistema
 * @version 1.0
 */
public class CustomProcess extends SvrProcess {
    
    /** Parâmetros do processo */
    private int p_M_Product_Category_ID = 0;
    private BigDecimal p_PriceAdjustment = Env.ZERO;
    private String p_AdjustmentType = "P"; // P=Percentage, A=Amount
    private Timestamp p_DateFrom = null;
    private Timestamp p_DateTo = null;
    private boolean p_IsActive = true;
    
    /**
     * Prepare - Obter parâmetros
     */
    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            
            if (name.equals("M_Product_Category_ID")) {
                p_M_Product_Category_ID = para[i].getParameterAsInt();
            }
            else if (name.equals("PriceAdjustment")) {
                p_PriceAdjustment = (BigDecimal) para[i].getParameter();
            }
            else if (name.equals("AdjustmentType")) {
                p_AdjustmentType = (String) para[i].getParameter();
            }
            else if (name.equals("DateFrom")) {
                p_DateFrom = (Timestamp) para[i].getParameter();
            }
            else if (name.equals("DateTo")) {
                p_DateTo = (Timestamp) para[i].getParameter_To();
            }
            else if (name.equals("IsActive")) {
                p_IsActive = "Y".equals(para[i].getParameter());
            }
            else {
                log.log(Level.SEVERE, "Unknown Parameter: " + name);
            }
        }
    }
    
    /**
     * Process - Executar o processo
     */
    protected String doIt() throws Exception {
        log.info("Iniciando processo de atualização de preços...");
        
        // Validar parâmetros obrigatórios
        if (p_PriceAdjustment == null || p_PriceAdjustment.compareTo(Env.ZERO) == 0) {
            return "Ajuste de preço é obrigatório e deve ser diferente de zero";
        }
        
        if (p_AdjustmentType == null || (!p_AdjustmentType.equals("P") && !p_AdjustmentType.equals("A"))) {
            return "Tipo de ajuste deve ser 'P' (Percentual) ou 'A' (Valor)";
        }
        
        try {
            int updatedProducts = 0;
            int updatedPrices = 0;
            
            // Construir query para buscar produtos
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.M_Product_ID, p.Name, pl.M_PriceList_Version_ID, ")
               .append("pl.PriceList, pl.PriceStd, pl.PriceLimit ")
               .append("FROM M_Product p ")
               .append("INNER JOIN M_ProductPrice pl ON p.M_Product_ID = pl.M_Product_ID ")
               .append("INNER JOIN M_PriceList_Version plv ON pl.M_PriceList_Version_ID = plv.M_PriceList_Version_ID ")
               .append("WHERE p.IsActive = ? ");
            
            // Adicionar filtros opcionais
            if (p_M_Product_Category_ID > 0) {
                sql.append("AND p.M_Product_Category_ID = ? ");
            }
            
            if (p_DateFrom != null) {
                sql.append("AND plv.ValidFrom >= ? ");
            }
            
            if (p_DateTo != null) {
                sql.append("AND plv.ValidFrom <= ? ");
            }
            
            sql.append("ORDER BY p.Name");
            
            // Executar query
            PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
            int paramIndex = 1;
            
            pstmt.setString(paramIndex++, p_IsActive ? "Y" : "N");
            
            if (p_M_Product_Category_ID > 0) {
                pstmt.setInt(paramIndex++, p_M_Product_Category_ID);
            }
            
            if (p_DateFrom != null) {
                pstmt.setTimestamp(paramIndex++, p_DateFrom);
            }
            
            if (p_DateTo != null) {
                pstmt.setTimestamp(paramIndex++, p_DateTo);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int productId = rs.getInt("M_Product_ID");
                String productName = rs.getString("Name");
                int priceListVersionId = rs.getInt("M_PriceList_Version_ID");
                BigDecimal priceList = rs.getBigDecimal("PriceList");
                BigDecimal priceStd = rs.getBigDecimal("PriceStd");
                BigDecimal priceLimit = rs.getBigDecimal("PriceLimit");
                
                // Calcular novos preços
                BigDecimal newPriceList = calculateNewPrice(priceList, p_PriceAdjustment, p_AdjustmentType);
                BigDecimal newPriceStd = calculateNewPrice(priceStd, p_PriceAdjustment, p_AdjustmentType);
                BigDecimal newPriceLimit = calculateNewPrice(priceLimit, p_PriceAdjustment, p_AdjustmentType);
                
                // Atualizar preços
                String updateSQL = "UPDATE M_ProductPrice SET " +
                                  "PriceList = ?, PriceStd = ?, PriceLimit = ?, " +
                                  "Updated = NOW(), UpdatedBy = ? " +
                                  "WHERE M_Product_ID = ? AND M_PriceList_Version_ID = ?";
                
                PreparedStatement updateStmt = DB.prepareStatement(updateSQL, get_TrxName());
                updateStmt.setBigDecimal(1, newPriceList);
                updateStmt.setBigDecimal(2, newPriceStd);
                updateStmt.setBigDecimal(3, newPriceLimit);
                updateStmt.setInt(4, getAD_User_ID());
                updateStmt.setInt(5, productId);
                updateStmt.setInt(6, priceListVersionId);
                
                int updated = updateStmt.executeUpdate();
                updateStmt.close();
                
                if (updated > 0) {
                    updatedPrices++;
                    addLog("Produto atualizado: " + productName + 
                          " - Preço Lista: " + priceList + " -> " + newPriceList);
                }
                
                updatedProducts++;
            }
            
            rs.close();
            pstmt.close();
            
            // Log final
            addLog("Processo concluído:");
            addLog("- Produtos processados: " + updatedProducts);
            addLog("- Preços atualizados: " + updatedPrices);
            addLog("- Tipo de ajuste: " + (p_AdjustmentType.equals("P") ? "Percentual" : "Valor"));
            addLog("- Ajuste aplicado: " + p_PriceAdjustment);
            
            return "Processo de atualização de preços concluído com sucesso. " +
                   "Produtos processados: " + updatedProducts + 
                   ", Preços atualizados: " + updatedPrices;
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro no processo de atualização de preços: " + e.getMessage(), e);
            throw new Exception("Erro no processo: " + e.getMessage());
        }
    }
    
    /**
     * Calcular novo preço baseado no tipo de ajuste
     */
    private BigDecimal calculateNewPrice(BigDecimal currentPrice, BigDecimal adjustment, String adjustmentType) {
        if (currentPrice == null || adjustment == null) {
            return currentPrice;
        }
        
        BigDecimal newPrice;
        
        if ("P".equals(adjustmentType)) {
            // Ajuste percentual
            BigDecimal factor = Env.ONE.add(adjustment.divide(Env.ONEHUNDRED, 4, BigDecimal.ROUND_HALF_UP));
            newPrice = currentPrice.multiply(factor);
        } else {
            // Ajuste por valor
            newPrice = currentPrice.add(adjustment);
        }
        
        // Arredondar para 2 casas decimais
        return newPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Método adicional: Processo de relatório de vendas
     */
    public String generateSalesReport() throws Exception {
        log.info("Gerando relatório de vendas...");
        
        try {
            String sql = "SELECT bp.Name as Cliente, " +
                        "SUM(o.GrandTotal) as TotalVendas, " +
                        "COUNT(o.C_Order_ID) as QtdPedidos, " +
                        "AVG(o.GrandTotal) as TicketMedio " +
                        "FROM C_Order o " +
                        "INNER JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID " +
                        "WHERE o.IsSOTrx = 'Y' AND o.DocStatus IN ('CO', 'CL') ";
            
            if (p_DateFrom != null) {
                sql += "AND o.DateOrdered >= ? ";
            }
            
            if (p_DateTo != null) {
                sql += "AND o.DateOrdered <= ? ";
            }
            
            sql += "GROUP BY bp.C_BPartner_ID, bp.Name " +
                   "ORDER BY TotalVendas DESC";
            
            PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName());
            int paramIndex = 1;
            
            if (p_DateFrom != null) {
                pstmt.setTimestamp(paramIndex++, p_DateFrom);
            }
            
            if (p_DateTo != null) {
                pstmt.setTimestamp(paramIndex++, p_DateTo);
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            addLog("=== RELATÓRIO DE VENDAS ===");
            addLog("Cliente | Total Vendas | Qtd Pedidos | Ticket Médio");
            addLog("------------------------------------------------");
            
            BigDecimal totalGeral = Env.ZERO;
            int totalPedidos = 0;
            
            while (rs.next()) {
                String cliente = rs.getString("Cliente");
                BigDecimal totalVendas = rs.getBigDecimal("TotalVendas");
                int qtdPedidos = rs.getInt("QtdPedidos");
                BigDecimal ticketMedio = rs.getBigDecimal("TicketMedio");
                
                addLog(cliente + " | " + totalVendas + " | " + qtdPedidos + " | " + ticketMedio);
                
                totalGeral = totalGeral.add(totalVendas);
                totalPedidos += qtdPedidos;
            }
            
            addLog("------------------------------------------------");
            addLog("TOTAL GERAL: " + totalGeral + " | " + totalPedidos + " pedidos");
            
            rs.close();
            pstmt.close();
            
            return "Relatório de vendas gerado com sucesso";
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "Erro ao gerar relatório: " + e.getMessage(), e);
            throw new Exception("Erro ao gerar relatório: " + e.getMessage());
        }
    }
}