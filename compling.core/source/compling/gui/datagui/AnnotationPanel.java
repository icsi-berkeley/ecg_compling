package compling.gui.datagui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import compling.annotation.AnnotationException;
import compling.annotation.childes.ChildesAnnotation;
import compling.annotation.childes.ChildesAnnotation.VernacularTier;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.annotation.childes.ChildesUtilities;
import compling.utterance.UtteranceAnnotation;

/*
 * TODO christine this class may just be a sandbox: a temporary place to try out things. (esp. considering the event
 * action/listening responses)
 */
public abstract class AnnotationPanel extends JPanel implements ChildesBrowserConstants {

	private static ChildesUtilities.TextChildesFormatter textFormatter = new ChildesUtilities.TextChildesFormatter();
	protected Action goNext, goPrev;

	public static AnnotationPanel newInstance(ChildesClause clause, Action prevAction, Action nextAction) {
		return new ClauseAnnotationPanel(clause, prevAction, nextAction);
	}

	public void setNavigation(Action prevAction, Action nextAction) {
		goNext = nextAction;
		goPrev = prevAction;
	}

	public void updateAnnotation() {
	}

	// TODO testing nav panel all AnnotationPanel use BoxLayout or something
	// where just add something
	protected JPanel getNavigationPanel() {
		if ((goNext != null) && (goPrev != null)) {
			JPanel buttons = new JPanel();
			JButton button = new JButton(goNext); // TODO testing
			buttons.add(button);
			button = new JButton(goPrev); // TODO testing
			buttons.add(button);
			return buttons;
		}
		return null;
	}

	static class ClauseAnnotationPanel extends AnnotationPanel {

		private static final long serialVersionUID = 5765412436987238727L;

		JTextField vernField;
		ChildesClause clause;

		protected ClauseAnnotationPanel(ChildesClause clause, Action prevAction, Action nextAction) {
			super();
			this.clause = clause;
			setNavigation(prevAction, nextAction);
			// setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setLayout(new GridBagLayout());

			JLabel clauseLabel = new JLabel();
			vernField = new JTextField(VERN_FIELD_SIZE);

			// TODO why getAnnotation need to return UttAnno ? the MaxEnt stuff
			// (doesn't compile)
			ChildesAnnotation currentAnnotation = (ChildesAnnotation) clause.getAnnotation(UtteranceAnnotation.CHILDES);
			// clauseLabel.setText(currentAnnotation.getSpeakerID()+":
			// "+ChildesUtilities.getTextFormatter().format(clause));
			clauseLabel.setText(clause.getSpeaker() + ": " + textFormatter.format(clause));
			vernField.setText(currentAnnotation.getVernacularTier().getContent());

			// JPanel vernPanel = new JPanel();
			JPanel vernGroup = new JPanel(new FlowLayout());
			vernGroup.add(new JLabel(VERN_LABEL));
			vernGroup.add(vernField);

			/* GridBagLayout */
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 0.5; // all
			c.anchor = GridBagConstraints.NORTH;

			c.gridx = 0;
			c.gridy = 0;
			add(clauseLabel, c);

			c.gridx = 0;
			c.gridy = 1;
			add(vernGroup, c);

			c.gridx = 0;
			c.gridy = 2;
			add(getNavigationPanel(), c);
		}

		public void updateAnnotation() {
			String vern = vernField.getText();
			try {
				ChildesAnnotation ca = null;
				if (clause.hasAnnotation(UtteranceAnnotation.CHILDES)) {
					ca = (ChildesAnnotation) clause.getAnnotation(UtteranceAnnotation.CHILDES);
				}
				else {
					ca = new ChildesAnnotation(clause);
				}
				if (vern != null) {
					ca.setAnnotationTier(new VernacularTier(vern));
				}
			}
			catch (AnnotationException ae) {
				// TODO exception model. alertMessage(ae.getMessage());
			}
		}

	}
}
