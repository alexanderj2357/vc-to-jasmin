package VC.TreeDrawer;

import javax.swing.*;
import java.awt.*;

class DrawerPanel extends JPanel {
    private Drawer drawer;

    public DrawerPanel(Drawer drawer) {
        setPreferredSize(new Dimension(4096, 4096));
        this.drawer = drawer;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawer.paintAST(g);
    }
}
