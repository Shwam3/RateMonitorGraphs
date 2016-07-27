package ratemonitorgrapher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;

public class RateMonitorGrapher
{
    public static File csvFile = null;
    public static JFrame frame;
    public static JGraphPanel graph;

    public static void main(String[] args)
    {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { e.printStackTrace(); }

        if (args.length >= 1)
            csvFile = new File(args[0]);

        final JFileChooser fc = new JFileChooser(System.getProperty("user.home", "C:") + File.separator + ".easigmap" + File.separator + "Logs" + File.separator + "RateMonitor");
        fc.getActionMap().get("viewTypeDetails").actionPerformed(null);
        JTable tabl = getDescendantsOfType(fc).get(0);
        tabl.getRowSorter().toggleSortOrder(3);
        tabl.getRowSorter().toggleSortOrder(3);
        
        if (csvFile == null || !csvFile.exists() || !csvFile.isFile())
        {
            int ret = fc.showOpenDialog(null);

            if (ret == JFileChooser.APPROVE_OPTION)
                csvFile = fc.getSelectedFile();
            else
                System.exit(-1);
        }

        try
        {
            EventQueue.invokeAndWait(() ->
            {
                frame = new JFrame("Rate Monitor Grapher");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(600, 400));
                frame.setPreferredSize(new Dimension(800, 600));
                frame.setLayout(new BorderLayout());

                JPanel backgroundPnl = new JPanel(new BorderLayout());
                graph = new JGraphPanel();
                backgroundPnl.add(graph, BorderLayout.CENTER);

                JPanel pnl = new JPanel(new FlowLayout());
                JLabel fileName = new JLabel(csvFile.getName());
                JButton refresh = new JButton("Refresh");
                refresh.addActionListener((ActionEvent e) -> graph.updateData());
                JButton changeFile = new JButton("Open...");
                changeFile.addActionListener((ActionEvent e) ->
                {
                    if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                        csvFile = fc.getSelectedFile();

                    fileName.setText(csvFile.getName());

                    graph.updateData();
                });
                JSlider detail = new JSlider(1, 60, 1);
                detail.setMajorTickSpacing(10);
                detail.setMinorTickSpacing(1);
                detail.setSnapToTicks(true);
                detail.setPaintTicks(true);
                detail.setToolTipText("Graph detail (" + detail.getValue() + ")");
                detail.addChangeListener((ChangeEvent e) -> { if (!detail.getValueIsAdjusting()) { graph.detail = detail.getValue(); graph.repaint(); detail.setToolTipText("Graph detail (" + detail.getValue() + ")");}});
                JSlider limit = new JSlider(0, 1000, 0);
                limit.setMajorTickSpacing(100);
                limit.setMinorTickSpacing(10);
                limit.setSnapToTicks(true);
                limit.setPaintTicks(true);
                limit.setToolTipText("Limit (" + limit.getValue() + ")");
                limit.addChangeListener((ChangeEvent e) -> { if (!limit.getValueIsAdjusting()) { graph.largestRateSlid = limit.getValue(); graph.repaint(); limit.setToolTipText("Limit (" + limit.getValue() + ")");}});

                pnl.add(fileName);
                pnl.add(refresh);
                pnl.add(changeFile);
                pnl.add(detail);
                pnl.add(limit);
                backgroundPnl.add(pnl, BorderLayout.NORTH);

                frame.add(graph, BorderLayout.CENTER);
                frame.add(pnl, BorderLayout.NORTH);

                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        }
        catch (InterruptedException | InvocationTargetException e) {}

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> graph.updateData(), 0, 15, TimeUnit.SECONDS);
    }
    
    public static List<JTable> getDescendantsOfType(Container container)
    {
        List<JTable> tList = new ArrayList<>();
        for (Component component : container.getComponents())
        {
            if (JTable.class.isAssignableFrom(component.getClass()))
            {
                tList.add(JTable.class.cast(component));
            }
            if (!JTable.class.isAssignableFrom(component.getClass()))
            {
                tList.addAll(getDescendantsOfType((Container) component));
            }
        }
        return tList;
    }
}