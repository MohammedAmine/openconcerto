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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.core.sales.invoice.report.ListeVenteXmlSheet;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class GenListeVentePanel extends JPanel implements ActionListener {

    private final JButton buttonGen = new JButton("Créer");
    private final JDate du;
    private final JDate au;
    JProgressBar bar = new JProgressBar();

    public GenListeVentePanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        this.add(new JLabelBold("Journal des Ventes"), c);

        c.gridwidth = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("Du"), c);

        c.gridx++;
        c.weightx = 1;
        this.du = new JDate(true);
        this.add(this.du, c);

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("au"), c);

        c.gridx++;
        c.weightx = 1;
        this.au = new JDate(true);
        this.add(this.au, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(this.bar, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        JPanel panelButton = new JPanel();
        panelButton.add(this.buttonGen);
        final JButton buttonClose = new JButton("Fermer");
        panelButton.add(buttonClose);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.weightx = 0;
        c.weighty = 1;
        this.add(panelButton, c);
        this.buttonGen.addActionListener(this);
        buttonClose.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.buttonGen) {
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        SQLTable tableFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE").getTable();
                        SQLTable tableAvoir = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT").getTable();
                        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
                        final SQLDataSource dataSource = Configuration.getInstance().getBase().getDataSource();
                        sel.addSelectStar(tableFact);
                        sel.setDistinct(true);
                        sel.setWhere(new Where(tableFact.getField("DATE"), GenListeVentePanel.this.du.getDate(), GenListeVentePanel.this.au.getDate()));
                        List<SQLRow> l = (List<SQLRow>) dataSource.execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, tableFact));
                        SQLSelect sel2 = new SQLSelect(Configuration.getInstance().getBase());
                        sel2.addSelectStar(tableAvoir);
                        sel2.setWhere(new Where(tableAvoir.getField("DATE"), GenListeVentePanel.this.du.getDate(), GenListeVentePanel.this.au.getDate()));
                        sel2.setDistinct(true);
                        l.addAll((List<SQLRow>) dataSource.execute(sel2.asString(), SQLRowListRSH.createFromSelect(sel2, tableAvoir)));
                        ListeVenteXmlSheet sheet = new ListeVenteXmlSheet(l, GenListeVentePanel.this.du.getDate(), GenListeVentePanel.this.au.getDate(), GenListeVentePanel.this.bar);

                        sheet.createDocumentAsynchronous().get();
                        sheet.showPrintAndExport(true, false, false);
                    } catch (Exception e) {
                        ExceptionHandler.handle("Erreur de traitement", e);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ((JFrame) SwingUtilities.getRoot(GenListeVentePanel.this)).dispose();
                        }
                    });
                }
            });
            thread.start();
        } else {
            ((JFrame) SwingUtilities.getRoot(this)).dispose();
        }
    }
}
