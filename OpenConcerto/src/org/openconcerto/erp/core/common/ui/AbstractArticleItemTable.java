/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableControlPanel;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTableRenderer;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.XTableColumnModel;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public abstract class AbstractArticleItemTable extends JPanel {
    protected RowValuesTable table;
    protected SQLTableElement totalHT, totalHA;
    protected SQLTableElement tableElementTVA;
    protected SQLTableElement tableElementTotalTTC;
    protected SQLTableElement tableElementTotalDevise;
    protected SQLTableElement service, qte, ha;
    protected SQLTableElement tableElementPoidsTotal;
    protected SQLTableElement prebilan;
    protected RowValuesTableModel model;
    protected SQLRowValues defaultRowVals;
    private List<JButton> buttons = null;
    protected RowValuesTableControlPanel control = null;
    private SQLRowAccessor tarif = null;

    public AbstractArticleItemTable() {
        init();
        uiInit();
    }

    public AbstractArticleItemTable(List<JButton> buttons) {
        this.buttons = buttons;
        init();
        uiInit();
    }

    /**
     * 
     */
    abstract protected void init();

    protected File getConfigurationFile() {
        return new File(Configuration.getInstance().getConfDir(), "Table/" + getConfigurationFileName());
    }

    /**
     * 
     */
    protected void uiInit() {
        // Ui init
        setLayout(new GridBagLayout());
        this.setOpaque(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;

        control = new RowValuesTableControlPanel(this.table, this.buttons);
        control.setOpaque(false);
        this.add(control, c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        final JScrollPane comp = new JScrollPane(this.table);
        comp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.add(comp, c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
    }

    /**
     * @return the coniguration file to store pref
     */
    protected abstract String getConfigurationFileName();

    public abstract SQLElement getSQLElement();

    public void updateField(final String field, final int id) {
        this.table.updateField(field, id);
    }

    public RowValuesTable getRowValuesTable() {
        return this.table;
    }

    public void insertFrom(final String field, final int id) {
        this.table.insertFrom(field, id);

    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public SQLTableElement getPrebilanElement() {
        return this.prebilan;
    }

    public SQLTableElement getPrixTotalHTElement() {
        return this.totalHT;
    }

    public SQLTableElement getPoidsTotalElement() {
        return this.tableElementPoidsTotal;
    }

    public SQLTableElement getPrixTotalTTCElement() {
        return this.tableElementTotalTTC;
    }

    public SQLTableElement getPrixServiceElement() {
        return this.service;
    }

    public SQLTableElement getQteElement() {
        return this.qte;
    }

    public SQLTableElement getHaElement() {
        return this.ha;
    }

    public SQLTableElement getTotalHaElement() {
        return this.totalHA;
    }

    public SQLTableElement getTVAElement() {
        return this.tableElementTVA;
    }

    public SQLTableElement getTableElementTotalDevise() {
        return this.tableElementTotalDevise;
    }

    public void deplacerDe(final int inc) {
        final int rowIndex = this.table.getSelectedRow();

        final int dest = this.model.moveBy(rowIndex, inc);
        this.table.getSelectionModel().setSelectionInterval(dest, dest);
    }

    /**
     * @return le poids total de tous les éléments (niveau 1) du tableau
     */
    public float getPoidsTotal() {

        float poids = 0.0F;
        final int poidsTColIndex = this.model.getColumnIndexForElement(this.tableElementPoidsTotal);
        if (poidsTColIndex >= 0) {
            for (int i = 0; i < this.table.getRowCount(); i++) {
                final Number tmp = (Number) this.model.getValueAt(i, poidsTColIndex);
                int level = 1;
                if (this.model.getRowValuesAt(i).getObject("NIVEAU") != null) {
                    level = this.model.getRowValuesAt(i).getInt("NIVEAU");
                }
                if (tmp != null && level == 1) {
                    poids += tmp.floatValue();
                }
            }
        }
        return poids;
    }

    public void refreshTable() {
        this.table.repaint();
    }

    public void createArticle(final int id, final SQLElement eltSource) {

        final SQLElement eltArticleTable = getSQLElement();

        final SQLTable tableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");

        final boolean modeAvance = DefaultNXProps.getInstance().getBooleanValue("ArticleModeVenteAvance", false);
        SQLPreferences prefs = SQLPreferences.getMemCached(tableArticle.getDBRoot());
        final boolean createArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);

        // On récupére les articles qui composent la table
        final List<SQLRow> listElts = eltSource.getTable().getRow(id).getReferentRows(eltArticleTable.getTable());
        final SQLRowValues rowArticle = new SQLRowValues(tableArticle);
        final Set<SQLField> fields = tableArticle.getFields();

        for (final SQLRow rowElt : listElts) {
            // final SQLRow foreignRow = rowElt.getForeignRow("ID_ARTICLE");
            // if (foreignRow == null || foreignRow.isUndefined()) {
            final Set<String> fieldsName = rowElt.getTable().getFieldsName();
            // on récupére l'article qui lui correspond

            for (final SQLField field : fields) {

                final String name = field.getName();
                if (fieldsName.contains(name) && !field.isPrimaryKey()) {
                    rowArticle.put(name, rowElt.getObject(name));
                }
            }
            // crée les articles si il n'existe pas

            int idArt = -1;
            if (modeAvance)
                idArt = ReferenceArticleSQLElement.getIdForCNM(rowArticle, createArticle);
            else {
                idArt = ReferenceArticleSQLElement.getIdForCN(rowArticle, createArticle);
            }
            if (createArticle && idArt > 1 && rowElt.isForeignEmpty("ID_ARTICLE")) {
                try {
                    rowElt.createEmptyUpdateRow().put("ID_ARTICLE", idArt).update();
                } catch (SQLException e) {
                    ExceptionHandler.handle("Erreur lors de l'affectation de l'article crée!", e);
                }
            }
            // ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
        }
        // }
    }


    public SQLRowValues getDefaultRowValues() {
        return this.defaultRowVals;
    }

    public SQLRowAccessor getTarif() {
        return tarif;
    }

    public void setTarif(SQLRowAccessor idTarif, boolean ask) {
        this.tarif = idTarif;
    }

    protected void setColumnVisible(int col, boolean visible) {
        if (col >= 0) {
            XTableColumnModel columnModel = this.table.getColumnModel();
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(col), visible);
        }
    }

    protected void calculTarifNomenclature(int index) {

        int rowCount = this.model.getRowCount();
        SQLRowValues rowValsSource = this.getRowValuesTable().getRowValuesTableModel().getRowValuesAt(index);
        int niveauSource = 1;
        if (rowValsSource.getObject("NIVEAU") != null) {
            niveauSource = rowValsSource.getInt("NIVEAU");
        }
        if (niveauSource > 1) {
            int startFrom = index;
            for (int i = index + 1; i < rowCount; i++) {
                SQLRowValues rowVals = this.getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
                if (rowVals.getInt("NIVEAU") == niveauSource) {
                    startFrom = i;
                } else {
                    break;
                }
            }

            if (startFrom == index) {

                // index à mettre à jour (de niveau n-1)
                int indexToUpdate = index;
                BigDecimal prixUnitHT = BigDecimal.ZERO;
                BigDecimal prixUnitHA = BigDecimal.ZERO;

                // Test pour éviter aucun niveau n-1 dans le tableau (ex : que des niveaux 2)
                boolean update = false;

                // Calcul du sous total
                for (int i = index; i >= 0; i--) {
                    indexToUpdate = i;
                    SQLRowValues rowVals = this.getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
                    int niveauCourant = 1;
                    if (rowVals.getObject("NIVEAU") != null) {
                        niveauCourant = rowVals.getInt("NIVEAU");
                    }
                    if (niveauCourant < niveauSource) {
                        update = true;
                        break;
                    } else if (niveauCourant == niveauSource) {
                        // Cumul des valeurs
                        prixUnitHT = prixUnitHT.add(rowVals.getBigDecimal("PV_HT").multiply(new BigDecimal(rowVals.getInt("QTE"))).multiply(rowVals.getBigDecimal("QTE_UNITAIRE")));
                        prixUnitHA = prixUnitHA.add(rowVals.getBigDecimal("PA_HT").multiply(new BigDecimal(rowVals.getInt("QTE"))).multiply(rowVals.getBigDecimal("QTE_UNITAIRE")));
                    }
                }
                if (update) {
                    this.model.putValue(prixUnitHA, indexToUpdate, "PRIX_METRIQUE_HA_1");
                    // this.model.putValue(prixUnitHA, indexToUpdate, "PA_HT");
                    this.model.putValue(ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getForeignID("ID_DEVISE"), indexToUpdate, "ID_DEVISE");
                    this.model.putValue(prixUnitHT, indexToUpdate, "PV_U_DEVISE");
                    this.model.putValue(prixUnitHT, indexToUpdate, "PRIX_METRIQUE_VT_1");
                    // this.model.putValue(prixUnitHT, indexToUpdate, "PV_HT");
                    // if (indexToUpdate < rowCount) {
                    // calculTarifNomenclature(indexToUpdate);
                    // }
                }
            } else {
                calculTarifNomenclature(startFrom);
            }
        }
    }

    public List<SQLRowValues> getRowValuesAtLevel(int level) {
        final int rowCount = this.model.getRowCount();
        final List<SQLRowValues> result = new ArrayList<SQLRowValues>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            final SQLRowValues row = this.model.getRowValuesAt(i);
            if (row.getObject("NIVEAU") == null || row.getInt("NIVEAU") == level) {
                result.add(row);
            }
        }
        return result;
    }
}
