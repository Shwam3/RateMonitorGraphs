package ratemonitorgrapher;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

public class JGraphPanel extends JPanel
{
    public  Map<String, List<Integer>> graphData = new HashMap<>();
    // MVT, RTPPM, VSTP, TSR
    private String[] headerNames = {};
    private final Color[] headerColours = {Color.LIGHT_GRAY, Color.ORANGE, Color.DARK_GRAY, Color.BLUE, Color.RED};
    private int largestRate = 10;
    public  int largestRateSlid = 0;
    public  int detail = 1;
    public  int xPos = -1;
    public  int yPos = -1;

    public JGraphPanel()
    {
        super();

        setFont(new Font(Font.MONOSPACED, Font.TRUETYPE_FONT, 12));
        setSize(Math.max(RateMonitorGrapher.frame.getContentPane().getSize().width, 600), Math.max(RateMonitorGrapher.frame.getContentPane().getSize().height, 400));

        addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent evt) { mouseMoved(evt); }

            @Override
            public void mouseMoved(MouseEvent evt)
            {
                xPos = evt.getX();
                yPos = evt.getY();

                if (xPos < 50 || xPos > getWidth()-20 || yPos > getHeight()-30 || yPos < 20)
                {
                    xPos = -1;
                    yPos = -1;
                }

                repaint();
            }
        });
    }

    public void updateData()
    {
        Map<String, List<Integer>> graphDataNew = new HashMap<>();

        List<String> fileContents = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(RateMonitorGrapher.csvFile)))
        {
            String line;
            while ((line = br.readLine()) != null)
                fileContents.add(line);
        }
        catch (IOException e) { e.printStackTrace(); }

        headerNames = fileContents.get(0).split(",");
        fileContents.remove(0);
        for (String header : headerNames)
            graphDataNew.put(header, new ArrayList<>());

        int tmpLargest = 10;

        for (String line : fileContents)
        {
            String[] splitLine = line.split(",");
            for (int i = 0; i < splitLine.length; i++)
            {
                int value;

                if (i != 0)
                {
                    value = Integer.parseInt(splitLine[i]);

                    if (value > tmpLargest)
                        tmpLargest = Math.max(value, tmpLargest);
                }
                else
                {
                    String[] timeParts = splitLine[i].split(":");
                    value  = Integer.parseInt(timeParts[0]) * 3600;
                    value += Integer.parseInt(timeParts[1]) * 60;
                    value += Integer.parseInt(timeParts[2]);
                }
                graphDataNew.get(headerNames[i]).add(value);
            }
        }

        graphData = graphDataNew;

        largestRate = (int) (Math.ceil(tmpLargest / 10f) * 10);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        if (headerNames == null)
            return;

        int width = getWidth();
        int height = getHeight();

        float largest = largestRateSlid == 0 ? largestRate : largestRateSlid;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        g2d.fillRect(50, 20, width - 70, height - 70);
        g2d.setColor(Color.WHITE);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

        //<editor-fold defaultstate="collapsed" desc="Markings">
        float yStep = (height - 70) / 100f;
        float numStep = largest / 100f;
        for (int i = 0; i <= 100; i++)
        {
            int rate = (int) (i * numStep);
            if (i % 10 == 0)
            {
                g2d.drawString((rate < 10 ? "  " : rate < 100 ? " " : "") + rate, 25, (height - 50) - yStep * i +4);
                g2d.drawLine(47, (int) ((height - 50) - (yStep * i)), width - 20, (int) ((height - 50) - (yStep * i)));
            }
            else
                g2d.drawLine(49, (int) ((height - 50) - (yStep * i)), 50, (int) ((height - 50) - (yStep * i)));

        }

        float xStep = (width - 70) / 96f;
        for (int i = 0; i <= 96; i++)
        {
            if (i % 4 == 0)
            {
                g2d.drawString("" + i/4, 47 + (int) (xStep * i) + (i/4 >= 10 ? -4 : 0), (height - 50) + 15);
                g2d.drawLine(50 + (int) (i * xStep), (height - 50), 50 + (int) (i * xStep), (height - 50)+4);
            }
            else
                g2d.drawLine(50 + (int) (i * (xStep)), (height - 50), 50 + (int) (i * xStep), (height - 50)+1);
        }
        //</editor-fold>

        float oneSec = (width - 70) / 86400f; // Width of one second
        float rateStep = (height - 70) / largest; // Height of one msg/min
        
        if (graphData != null && !graphData.isEmpty())
        {
            //<editor-fold defaultstate="collapsed" desc="Graph Data">
            int col = -1;

            List<Integer> times = graphData.get("time");

            for (String heading : headerNames)
            {
                if (heading.equalsIgnoreCase("time"))
                    continue;

                g2d.setColor(headerColours[++col]);

                List<Integer> rates = graphData.get(heading);

                int i;
                for (i = 0; i < rates.size()-detail; i += detail)
                {
                    g2d.drawLine(50 + (int)(oneSec * times.get(i)),
                            (height - 50) - (int) (rateStep * rates.get(i)),
                            50 + (int)(oneSec * times.get(i+detail)),
                            (height - 50) - (int) (rateStep * rates.get(i+detail)));
                }
                g2d.drawLine(50 + (int)(oneSec * times.get(i)),
                        (height - 50) - (int) (rateStep * rates.get(i)),
                        50 + (int)(oneSec * times.get(rates.size()-1)),
                        (height - 50) - (int) (rateStep * rates.get(rates.size()-1)));
            }
            //</editor-fold>

            g2d.setColor(Color.WHITE);
            g2d.drawLine(47, height-50, width-30, height-50);
            g2d.drawLine(50, 20, 50, height-50);

            //<editor-fold defaultstate="collapsed" desc="Info Box">
            if (xPos >= 0 && yPos >= 0)
            {
                int infoWidth = 195;
                int x = xPos + 10;

                if (x+infoWidth+10 >= width)
                    x -= infoWidth+20;

                g2d.drawLine(xPos, 20, xPos, height-50);

                g2d.setColor(Color.BLACK);
                g2d.fillRect(x, 25, infoWidth, 90);

                g2d.setColor(Color.WHITE);
                g2d.drawRect(x, 25, infoWidth, 90);

                float secsForX = (xPos-50) / oneSec;
                int index = graphData.get("time").size()-1;
                for (int i = 0; i < headerNames.length; i++)
                {
                    g2d.drawString(headerNames[i].replace("/topic/", "") + ":", x+12, 35 + 15*i);
                    if (i == 0)
                    {
                        for (int j = 0; j < graphData.get("time").size(); j++)
                            if (graphData.get("time").get(j) >= secsForX)
                            {
                                index = j;
                                break;
                            }

                        secsForX = graphData.get("time").get(index);
                        int hrs = (int) Math.floor(secsForX / 3600f);
                        int min = (int) Math.floor((secsForX - hrs*3600) / 60f);
                        int sec = (int) (secsForX - hrs*3600 - min*60);

                        g2d.drawString((hrs < 10 ? "0" + hrs : hrs) + ":"
                                + (min < 10 ? "0" + min : min)
                                + ":" + (sec < 10 ? "0" + sec : sec),
                                x+137, 35 + 15*i);
                    }
                    else
                    {
                        g2d.setColor(headerColours[i-1]);
                        g2d.drawLine(x+4, 30+15*i, x+8, 30+15*i);

                        g2d.setColor(Color.WHITE);
                        g2d.drawString(graphData.get(headerNames[i]).get(index).toString(), x+137, 35 + 15*i);
                    }
                }
            }
            //</editor-fold>
        }

        g2d.setColor(Color.WHITE);
        g2d.drawString("Time (hrs)", 50 + (width - 130) / 2, height - 20);

        g2d.rotate(-Math.PI/2);
        g2d.drawString("Rate (msg/min)", -((height+60) / 2), 20);

        g2d.dispose();
    }
}