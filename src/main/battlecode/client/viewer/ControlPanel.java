package battlecode.client.viewer;

import battlecode.client.resources.ResourceLoader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import java.util.Observable;
import java.util.Observer;

public class ControlPanel extends JPanel
        implements ActionListener, ChangeListener, Controller {

    private static final long serialVersionUID = 0; // don't serialize
    private MatchPlayer player;
    private final JPanel panel;
    private final JLabel label;
    private final JButton start;
    private final JButton play;
    private final JButton end;
    private final JButton next;
    private final JButton back;
    private final JButton step;
    private final JFormattedTextField stepSizeField;
    private final NumberFormat stepSizeFmt = NumberFormat.getNumberInstance();
    private final JSlider slider;
    private boolean setSliderPrecise = false;
    private final String matchCount = "";
    private final ImageIcon playIcon;
    private final ImageIcon pauseIcon;
    private InfoPanel infoPanel = null;
    private final Observer timelineObserver = new Observer() {

        public void update(Observable o, Object obj) {
            GameStateTimeline gst = (GameStateTimeline) o;
            if (gst.isActive()) {
                int round = gst.getRound();
                if (round >= 0) {
                    updateRoundLabel(round, gst.getNumRounds());
                    setSliderValue(round);
                }
            } else {
                next.setEnabled(false);
                slider.setEnabled(false);
                setSliderValue(0);
            }
        }
    };

    public ControlPanel() {
        label = new JLabel(matchCount + "Round 0 of 0");
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        playIcon = new ImageIcon(ResourceLoader.getUrl("art/icons/playback-start.png"));
        pauseIcon = new ImageIcon(ResourceLoader.getUrl("art/icons/playback-pause.png"));

        start = createButton("art/icons/skip-backward.png", "start");
        play = createButton(pauseIcon, "pause");
        end = createButton("art/icons/skip-forward.png", "end");
        next = createButton("art/icons/go-next.png", "next");
        next.setEnabled(false);

        back = createButton("art/icons/seek-backward.png", "back");
        step = createButton("art/icons/seek-forward.png", "step");

        stepSizeFmt.setGroupingUsed(false);
        stepSizeField = new JFormattedTextField(stepSizeFmt);
        stepSizeField.setValue(1);
        stepSizeField.setColumns(5);
        stepSizeField.setMinimumSize(new Dimension(50, stepSizeField
                .getHeight()));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(start);
        panel.add(play);
        panel.add(end);
        panel.add(next);
        panel.add(back);
        panel.add(step);
        panel.add(stepSizeField);
        panel.add(new JLabel(" rounds"));

        slider = new JSlider(0, 1);
        setSliderValue(0);
        slider.setEnabled(false);
        infoPanel = new InfoPanel();

        setAlignmentX(CENTER_ALIGNMENT);

        /*
        setLayout(new TableLayout(LAYOUT));
        add(label, "0, 0, 0, 0, c, f");
        add(panel, "0, 1, 0, 1, f, t");
        add(slider, "0, 2, 0, 2, f, f");
        add(infoPanel, "2, 0, 2, 2, c, f");
         */

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);
        gbc.gridy = 1;
        add(panel, gbc);
        gbc.gridy = 2;
        add(slider, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        add(infoPanel, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.gridheight = 1;
        add(new JPanel());


        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setMaximumSize(getSize());
                removeComponentListener(this);
            }
        });


    }

    private JButton createButton(String iconPath, String cmd) {
        return createButton(new ImageIcon(ResourceLoader.getUrl(iconPath)), cmd);
    }

    private JButton createButton(ImageIcon icon, String cmd) {
        JButton button = new JButton(icon);
        button.setActionCommand(cmd);
        button.addActionListener(this);
        return button;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    public int getStepSize() {
        return ((Number) stepSizeField.getValue()).intValue();
    }

    public void setPlayEnabled(boolean enabled) {
        if (enabled) {
            play.setActionCommand("play");
            play.setIcon(playIcon);
        } else {
            play.setActionCommand("pause");
            play.setIcon(pauseIcon);
        }
    }

    public void enableNext() {
        // we need to use invokeLater to avoid a deadlock
        // in the 3d client
        SwingUtilities.invokeLater(() -> next.setEnabled(true));
    }

    public void setPlayer(MatchPlayer player) {
        if (this.player == null) {
            slider.addChangeListener(this);
        }
        this.player = player;
        GameStateTimeline gst = player.getTimeline();
        gst.addObserver(timelineObserver);
        gst.getMatch().addMatchListener(new MatchListener() {

            public void headerReceived(BufferedMatch match) {
                slider.setMaximum(match.getHeader().getMap().getRounds());
                slider.setEnabled(true);
            }
        });
    }

    public void updateRoundLabel(int round, int max) {
        if (round >= 0) {
            label.setText("Round " + round + " of " + max);
        }
        if (max >= slider.getMaximum()) {
            slider.setMaximum(max);
        }
    }

    public void updateRoundLabel(GameStateTimeline gst) {
        updateRoundLabel(gst.getRound(), gst.getNumRounds());
    }

    private void setSliderValue(int round) {
        setSliderPrecise = true;
        slider.setValue(round);
    }

    public void actionPerformed(ActionEvent e) {
        if (player != null) {
            player.actionPerformed(e);
        }
    }

    public void stateChanged(ChangeEvent e) {
        GameStateTimeline gst = player.getTimeline();
        if (slider.getValueIsAdjusting() && !setSliderPrecise) {
            int round = slider.getValue();
            gst.setRound(round - (round % gst.getRoundsPerKey()));
        }
        setSliderPrecise = false;
        if (slider.getValue() > gst.getNumRounds()) {
            setSliderValue(gst.getNumRounds());
        }
    }
}
