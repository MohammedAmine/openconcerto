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
 
 package org.openconcerto.erp.core.sales.invoice.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.core.sales.invoice.ui.DateEnvoiRenderer;
import org.openconcerto.erp.core.sales.invoice.ui.DateReglementRenderer;
import org.openconcerto.erp.core.sales.invoice.ui.ListeFactureRenderer;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;

public class ListeSaisieVenteFactureAction extends CreateFrameAbstractAction {

    private IListFrame frame;
    // private EditFrame editFrame;
    private ListeGestCommEltPanel listeAddPanel;
    private boolean filterOnCurrentYear = false;
    private boolean reglementEditable = true;

    public ListeSaisieVenteFactureAction() {
        this(false, true);
    }

    public ListeSaisieVenteFactureAction(boolean filterOnCurrentYear, boolean reglementEditable) {
        super();
        this.putValue(Action.NAME, "Liste des factures");
        this.filterOnCurrentYear = filterOnCurrentYear;
        this.reglementEditable = reglementEditable;
    }

    public JFrame createFrame() {
        SQLElement eltFacture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");

        final SQLTableModelSourceOnline src = eltFacture.getTableSource(true);

        for (SQLTableModelColumn column : src.getColumns()) {
            // if (column.getValueClass() == Long.class ||
            // column.getValueClass() ==
            // BigInteger.class || column.getValueClass() == BigDecimal.class)
            column.setRenderer(ListeFactureRenderer.UTILS.getRenderer(column.getRenderer()));
            if (column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                ((SQLTableModelColumnPath) column).setEditable(false);
            }
        }
            final SQLTableModelColumn dateEnvoiCol = src.getColumn(eltFacture.getTable().getField("DATE_ENVOI"));

            if (dateEnvoiCol != null) {
                ((SQLTableModelColumnPath) dateEnvoiCol).setEditable(reglementEditable);

                dateEnvoiCol.setColumnInstaller(new IClosure<TableColumn>() {
                    @Override
                    public void executeChecked(TableColumn columnDateEnvoi) {
                        columnDateEnvoi.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
                        columnDateEnvoi.setCellRenderer(new DateEnvoiRenderer());
                    }
                });
            }
            final SQLTableModelColumn dateReglCol = src.getColumn(eltFacture.getTable().getField("DATE_REGLEMENT"));
            ((SQLTableModelColumnPath) dateReglCol).setEditable(reglementEditable);

            // Edition des dates de reglement
            dateReglCol.setColumnInstaller(new IClosure<TableColumn>() {
                @Override
                public void executeChecked(TableColumn columnDateReglement) {
                    columnDateReglement.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
                    columnDateReglement.setCellRenderer(new DateReglementRenderer());
                }
            });
        this.listeAddPanel = new ListeGestCommEltPanel(eltFacture, new IListe(src)) {

            @Override
            protected GridBagConstraints createConstraints() {
                GridBagConstraints c = super.createConstraints();
                c.gridy++;
                return c;
            }
        };
        this.listeAddPanel.setAddVisible(true);
        this.listeAddPanel.getListe().getModel().setEditable(true);
        GridBagConstraints c = new DefaultGridBagConstraints();
        // Total panel
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;
        JPanel panelTotal = new JPanel(new FlowLayout());

        String sfe = DefaultNXProps.getInstance().getStringProperty("ArticleSFE");
        Boolean bSfe = Boolean.valueOf(sfe);
        boolean isSFE = bSfe != null && bSfe.booleanValue();

        List<SQLField> fields = new ArrayList<SQLField>(2);
        if (isSFE) {
            fields.add(eltFacture.getTable().getField("T_HA"));
        }

        fields.add(eltFacture.getTable().getField("T_HT"));
        fields.add(eltFacture.getTable().getField("T_TTC"));
        IListTotalPanel totalPanel = new IListTotalPanel(this.listeAddPanel.getListe(), IListTotalPanel.initListe(this.listeAddPanel.getListe(), fields), null, "Total Global");

        panelTotal.add(totalPanel);

        this.listeAddPanel.add(panelTotal, c);

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(this.listeAddPanel.getListe(), eltFacture.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        this.listeAddPanel.add(datePanel, c);
        if (this.filterOnCurrentYear) {
            datePanel.setFilterOnDefault();
        }

        this.frame = new IListFrame(this.listeAddPanel);

        return this.frame;
    }

    public List<Integer> getListId() {
        IListe liste = this.listeAddPanel.getListe();

        if (liste != null) {
            List<Integer> listeIds = new ArrayList<Integer>(liste.getRowCount());
            for (int i = 0; i < liste.getRowCount(); i++) {
                listeIds.add(liste.idFromIndex(i));
            }
            return listeIds;
        }
        return null;
    }

    private JPanel getPanelLegende() {
        JPanel panelLegende = new JPanel();

        JLabel labelLeg = new JLabel("Légende : ");
        labelLeg.setOpaque(true);
        panelLegende.add(labelLeg);

        // Acompte
        JLabel labelAcompte = new JLabel("  Acompte ");
        labelAcompte.setOpaque(true);
        labelAcompte.setBackground(ListeFactureRenderer.acompte);
        final Border lineBorder = BorderFactory.createEtchedBorder();
        labelAcompte.setBorder(lineBorder);
        panelLegende.add(labelAcompte);

        // Complement
        JLabel labelCompl = new JLabel("  Complément ");
        labelCompl.setOpaque(true);
        labelCompl.setBackground(ListeFactureRenderer.complement);
        labelCompl.setBorder(lineBorder);
        panelLegende.add(labelCompl);

        // Previsionnelle
        JLabel labelPrev = new JLabel("  Prévisionnelle ");
        labelPrev.setOpaque(true);
        labelPrev.setBackground(ListeFactureRenderer.prev);
        labelPrev.setBorder(lineBorder);

        panelLegende.add(labelPrev);

        return panelLegende;
    }
}
