package compling.gui.grammargui.ui.editors;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

public class EcgDocumentSetupParticipant implements IDocumentSetupParticipant {

	public void setup(IDocument document) {
		IDocumentPartitioner partitioner = new FastPartitioner(EcgPartitionScanner.instance(),
				EcgPartitionScanner.ECG_PARTITION_TYPES);
		((IDocumentExtension3) document).setDocumentPartitioner(EcgPartitionScanner.ECG_PARTITIONING, partitioner);
		partitioner.connect(document);
	}

}
