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
 
 package org.openconcerto.erp.core.sales.quote.ui;

import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.invoice.ui.DateEnvoiRenderer;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.quote.component.DevisSQLComponent;
import org.openconcerto.erp.core.sales.quote.element.DevisItemSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class ListeDesDevisPanel extends JPanel {

    private JTabbedPane tabbedPane = new JTabbedPane();
    private Map<Integer, ListeAddPanel> map = new HashMap<Integer, ListeAddPanel>();
    private SQLElement eltDevis = Configuration.getInstance().getDirectory().getElement("DEVIS");
    private SQLElement eltEtatDevis = Configuration.getInstance().getDirectory().getElement("ETAT_DEVIS");
    private JButton buttonShow, buttonGen, buttonPrint, buttonFacture, buttonCmd, buttonClone;
    protected EditFrame editFrame;

    public ListeDesDevisPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;

        // Tous
        ListeAddPanel panelAll = createPanel(-1);
        this.map.put(this.tabbedPane.getTabCount(), panelAll);
        this.tabbedPane.add("Tous", panelAll);

        // Attente
        ListeAddPanel panelCours = createPanel(EtatDevisSQLElement.EN_COURS);
        this.map.put(this.tabbedPane.getTabCount(), panelCours);

        this.tabbedPane.add(eltEtatDevis.getTable().getRow(EtatDevisSQLElement.EN_COURS).getString("NOM"), panelCours);

        // Attente
        ListeAddPanel panelAttente = createPanel(EtatDevisSQLElement.EN_ATTENTE);
        this.map.put(this.tabbedPane.getTabCount(), panelAttente);
        this.tabbedPane.add(eltEtatDevis.getTable().getRow(EtatDevisSQLElement.EN_ATTENTE).getString("NOM"), panelAttente);

        // Accepte
        ListeAddPanel panelAccepte = createPanel(EtatDevisSQLElement.ACCEPTE);
        this.map.put(this.tabbedPane.getTabCount(), panelAccepte);
        this.tabbedPane.add(eltEtatDevis.getTable().getRow(EtatDevisSQLElement.ACCEPTE).getString("NOM"), panelAccepte);

        // Refuse
        ListeAddPanel panelRefuse = createPanel(EtatDevisSQLElement.REFUSE);
        this.map.put(this.tabbedPane.getTabCount(), panelRefuse);
        this.tabbedPane.add(eltEtatDevis.getTable().getRow(EtatDevisSQLElement.REFUSE).getString("NOM"), panelRefuse);

        this.tabbedPane.setSelectedIndex(1);

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.tabbedPane, c);

        // Date panel
        Map<IListe, SQLField> map = new HashMap<IListe, SQLField>();
        map.put(panelAttente.getListe(), eltDevis.getTable().getField("DATE"));
        map.put(panelAccepte.getListe(), eltDevis.getTable().getField("DATE"));
        map.put(panelRefuse.getListe(), eltDevis.getTable().getField("DATE"));
        map.put(panelCours.getListe(), eltDevis.getTable().getField("DATE"));
        map.put(panelAll.getListe(), eltDevis.getTable().getField("DATE"));

        IListFilterDatePanel datePanel = new IListFilterDatePanel(map, IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        datePanel.setFilterOnDefault();
        this.add(datePanel, c);

    }

    protected void setRenderer(SQLTableModelSourceOnline source) {

    }

    private ListeAddPanel createPanel(int idFilter) {
        // Filter
        final SQLTableModelSourceOnline lAttente = this.eltDevis.getTableSource(true);
        final SQLTableModelColumnPath dateEnvoiCol;
        if (idFilter == EtatDevisSQLElement.ACCEPTE) {
            dateEnvoiCol = new SQLTableModelColumnPath(this.eltDevis.getTable().getField("DATE_ENVOI"));
            lAttente.getColumns().add(dateEnvoiCol);
            dateEnvoiCol.setRenderer(new DateEnvoiRenderer());
            dateEnvoiCol.setEditable(true);
        } else {
            dateEnvoiCol = null;
        }
        if (idFilter > 1) {
            Where wAttente = new Where(this.eltDevis.getTable().getField("ID_ETAT_DEVIS"), "=", idFilter);
            lAttente.getReq().setWhere(wAttente);
        } else {
            lAttente.getColumns().add(new BaseSQLTableModelColumn("Etat", String.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {
                    // TODO Raccord de méthode auto-généré
                    return r.getForeign("ID_ETAT_DEVIS").getString("NOM");
                }

                @Override
                public Set<FieldPath> getPaths() {
                    // TODO Raccord de méthode auto-généré
                    Set<FieldPath> s = new HashSet<FieldPath>();
                    s.add(eltDevis.getTable().getField("ID_ETAT_DEVIS").getFieldPath());
                    return s;
                }
            });
        }

        setRenderer(lAttente);
        // one config file per idFilter since they haven't the same number of
        // columns
        final ListeAddPanel pane = new ListeAddPanel(this.eltDevis, new IListe(lAttente), "idFilter" + idFilter);

        IListTotalPanel total;
        if (this.eltDevis.getTable().contains("PREBILAN")) {
            // asList = Arrays.asList(this.eltDevis.getTable().getField("PREBILAN"),
            // this.eltDevis.getTable().getField("T_HT"));
            List<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>> fields = new ArrayList<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>>(2);
            fields.add(Tuple2.create(pane.getListe().getSource().getColumn(this.eltDevis.getTable().getField("T_HT")), IListTotalPanel.Type.SOMME));
            fields.add(Tuple2.create(pane.getListe().getSource().getColumn(this.eltDevis.getTable().getField("PREBILAN")), IListTotalPanel.Type.SOMME));
            fields.add(Tuple2.create(new BaseSQLTableModelColumn("%MB", String.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {
                    // TODO Raccord de méthode auto-généré
                    return null;
                }

                @Override
                public Set<FieldPath> getPaths() {
                    // TODO Raccord de méthode auto-généré
                    return null;
                }
            }, IListTotalPanel.Type.MOYENNE_MARGE));
            total = new IListTotalPanel(pane.getListe(), fields, null, "Total Global");
        } else if (this.eltDevis.getTable().contains("T_HA")) {

            total = new IListTotalPanel(pane.getListe(), Arrays.asList(this.eltDevis.getTable().getField("T_HA"), this.eltDevis.getTable().getField("T_HT")));
        } else {
            total = new IListTotalPanel(pane.getListe(), Arrays.asList(this.eltDevis.getTable().getField("T_HT")));
        }

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 4;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        pane.add(total, c);

        // Renderer
        JTable table = pane.getListe().getJTable();

        if (idFilter == EtatDevisSQLElement.ACCEPTE) {

            pane.getListe().setSQLEditable(true);
            // Edition des dates d'envois
            TableColumn columnDateEnvoi = pane.getListe().getJTable().getColumnModel().getColumn(table.getColumnCount() - 1);
            columnDateEnvoi.setCellEditor(new org.openconcerto.ui.table.TimestampTableCellEditor());
            final SQLTableModelSourceOnline src = (SQLTableModelSourceOnline) pane.getListe().getModel().getReq();
            for (SQLTableModelColumn column : src.getColumns()) {
                if (column != dateEnvoiCol && column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                    ((SQLTableModelColumnPath) column).setEditable(false);
                }
            }
        }

        // MouseSheetXmlListeListener mouseSheetXmlListeListener = new
        // MouseSheetXmlListeListener(DevisXmlSheet.class) {
        // @Override
        // public List<RowAction> addToMenu() {
        //
        // // int type =
        // // pane.getListe().getSelectedRow().getInt("ID_ETAT_DEVIS");
        // // factureAction.setEnabled(type ==
        // // EtatDevisSQLElement.ACCEPTE);
        // // commandeAction.setEnabled(type ==
        // // EtatDevisSQLElement.ACCEPTE);
        // // if (type == EtatDevisSQLElement.EN_ATTENTE) {
        // // list.add(accepteAction);
        // // }
        // // list.add(factureAction);
        // // list.add(commandeAction);
        // // list.add(actionTransfertCmd);
        // }
        // };
        // mouseSheetXmlListeListener.setGenerateHeader(true);
        // mouseSheetXmlListeListener.setShowHeader(true);
        // pane.getListe().addIListeActions(mouseSheetXmlListeListener.getRowActions());

        // activation des boutons
        // pane.getListe().addIListener(new IListener() {
        // public void selectionId(int id, int field) {
        // checkButton(id);
        // }
        // });
        addRowActions(pane.getListe(), idFilter);

        pane.getListe().setOpaque(false);

        pane.setOpaque(false);
        return pane;
    }

    protected void addRowActions(IListe liste, int etat) {
        // List<RowAction> list = new ArrayList<RowAction>();
        // Transfert vers facture
        RowAction factureAction = new RowAction(new AbstractAction("Transfert vers facture") {
            public void actionPerformed(ActionEvent e) {
                transfertFacture(IListe.get(e).getSelectedRow());
            }
        }, true) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };

        liste.addIListeAction(factureAction);

        // Voir le document
        RowAction actionTransfertCmd = new RowAction(new AbstractAction("Transférer en commande") {
            public void actionPerformed(ActionEvent e) {
                transfertCommande(IListe.get(e).getSelectedRow());
            }
        }, false) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };
        liste.addIListeAction(actionTransfertCmd);

        // Transfert vers commande
        RowAction commandeAction = new RowAction(new AbstractAction("Transfert vers commande client") {
            public void actionPerformed(ActionEvent e) {
                transfertCommandeClient(IListe.get(e).getSelectedRow());
            }
        }, true) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.ACCEPTE) {
                        return true;
                    }
                }
                return false;
            };
        };

        liste.addIListeAction(commandeAction);

        RowAction accepteEtCmdAction = new RowAction(new AbstractAction("Marquer comme accepté et Transfert en commande client") {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow();
                SQLRowValues rowVals = selectedRow.createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                selectedRow.getTable().fireTableModified(IListe.get(e).getSelectedId());
                transfertCommandeClient(selectedRow);
            }
        }, false) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };
        liste.addIListeAction(accepteEtCmdAction);

        // Marqué accepté
        RowAction accepteAction = new RowAction(new AbstractAction("Marquer comme accepté") {
            public void actionPerformed(ActionEvent e) {
                SQLRowValues rowVals = IListe.get(e).getSelectedRow().createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.ACCEPTE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                IListe.get(e).getSelectedRow().getTable().fireTableModified(IListe.get(e).getSelectedId());
            }
        }, false) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };

        liste.addIListeAction(accepteAction);

        // Marqué accepté
        RowAction refuseAction = new RowAction(new AbstractAction("Marquer comme refusé") {
            public void actionPerformed(ActionEvent e) {
                SQLRowValues rowVals = IListe.get(e).getSelectedRow().createEmptyUpdateRow();
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.REFUSE);
                try {
                    rowVals.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                IListe.get(e).getSelectedRow().getTable().fireTableModified(IListe.get(e).getSelectedId());
            }
        }, false) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                if (selection != null && selection.size() == 1) {
                    if (selection.get(0).getInt("ID_ETAT_DEVIS") == EtatDevisSQLElement.EN_ATTENTE) {
                        return true;
                    }
                }
                return false;
            };
        };

        liste.addIListeAction(refuseAction);

        // // Dupliquer
        RowAction cloneAction = new RowAction(new AbstractAction("Créer à partir de") {
            public void actionPerformed(ActionEvent e) {
                SQLRow selectedRow = IListe.get(e).getSelectedRow();

                if (ListeDesDevisPanel.this.editFrame == null) {
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("DEVIS");
                    ListeDesDevisPanel.this.editFrame = new EditFrame(eltFact, EditPanel.CREATION);
                }

                ((DevisSQLComponent) ListeDesDevisPanel.this.editFrame.getSQLComponent()).loadDevisExistant(selectedRow.getID());
                ListeDesDevisPanel.this.editFrame.setVisible(true);
            }
        }, true) {
            public boolean enabledFor(java.util.List<org.openconcerto.sql.model.SQLRowAccessor> selection) {
                return (selection != null && selection.size() == 1);
            };
        };

        liste.addIListeAction(cloneAction);
    }

    /**
     * Transfert en facture
     * 
     * @param row
     */
    private void transfertFacture(SQLRow row) {
        DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
        elt.transfertFacture(row.getID());
    }

    /**
     * Transfert en Commande
     * 
     * @param row
     */
    private void transfertCommandeClient(SQLRow row) {

        DevisSQLElement elt = (DevisSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS");
        elt.transfertCommandeClient(row.getID());
    }

    private void transfertCommande(SQLRow row) {
        DevisItemSQLElement elt = (DevisItemSQLElement) Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");
        SQLTable tableCmdElt = Configuration.getInstance().getDirectory().getElement("COMMANDE_ELEMENT").getTable();
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
        List<SQLRow> rows = row.getReferentRows(elt.getTable());
        CollectionMap<SQLRow, List<SQLRowValues>> map = new CollectionMap<SQLRow, List<SQLRowValues>>();
        for (SQLRow sqlRow : rows) {
            // on récupére l'article qui lui correspond
            SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
            for (SQLField field : eltArticle.getTable().getFields()) {
                if (sqlRow.getTable().getFieldsName().contains(field.getName())) {
                    rowArticle.put(field.getName(), sqlRow.getObject(field.getName()));
                }
            }
            // rowArticle.loadAllSafe(rowEltFact);
            int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowArticle, true);
            SQLRow rowArticleFind = eltArticle.getTable().getRow(idArticle);
            SQLInjector inj = SQLInjector.getInjector(rowArticle.getTable(), tableCmdElt);
            SQLRowValues rowValsElt = new SQLRowValues(inj.createRowValuesFrom(rowArticleFind));
            rowValsElt.put("ID_STYLE", sqlRow.getObject("ID_STYLE"));
            rowValsElt.put("QTE", sqlRow.getObject("QTE"));
            rowValsElt.put("T_POIDS", rowValsElt.getLong("POIDS") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_HT", rowValsElt.getLong("PA_HT") * rowValsElt.getInt("QTE"));
            rowValsElt.put("T_PA_TTC", rowValsElt.getLong("T_PA_HT") * (rowValsElt.getForeign("ID_TAXE").getFloat("TAUX") / 100.0 + 1.0));

            map.put(rowArticleFind.getForeignRow("ID_FOURNISSEUR"), rowValsElt);

        }
        MouvementStockSQLElement.createCommandeF(map, row.getForeignRow("ID_TARIF").getForeignRow("ID_DEVISE"));
    }

    public Map<Integer, ListeAddPanel> getListePanel() {
        return this.map;
    }
}