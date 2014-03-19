/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package compling.gui.grammargui.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
//import org.eclipse.update.ui.UpdateJob;
//import org.eclipse.update.ui.UpdateManagerUI;

/**
 * Action to invoke the Update install wizard.
 * 
 * @since 3.0
 */
@SuppressWarnings("deprecation")
public class UpdateAction extends Action implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public static final String ID = "compling.gui.grammargui.commands.Update";

	public UpdateAction() {
		super();
		setId(ID);
		setActionDefinitionId(ID);
		setText("&Software Updates...");
		setToolTipText("Search for Updates to ECG Workbench");
	}

//	public void run() {
//		startUpdateJob(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
//	}
//
//	public void run(IAction action) {
//		startUpdateJob(window);
//	}

//	private void startUpdateJob(final IWorkbenchWindow window) {
//		BusyIndicator.showWhile(window.getShell().getDisplay(), new Runnable() {
//			public void run() {
//				UpdateJob job = new UpdateJob("Searching for Updates", false, false);
//				UpdateManagerUI.openInstaller(window.getShell(), job);
//			}
//		});
//	}

	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

	public void dispose() {
		// do nothing
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	@Override
	public void run(IAction action) {
		// TODO Auto-generated method stub
		
	}
}
