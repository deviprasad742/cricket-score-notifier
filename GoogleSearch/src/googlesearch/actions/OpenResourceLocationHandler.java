package googlesearch.actions;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import googlesearch.Activator;

public class OpenResourceLocationHandler extends AbstractHandler {
	
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object selectedObject = getSelectedObject();
		IResource resource = null;
		Shell activeShell = Display.getDefault().getActiveShell();
		Shell workbenchShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		if (activeShell == workbenchShell) {
			if (selectedObject instanceof IResource) {
				resource = (IResource) selectedObject;
			} else if (selectedObject instanceof IAdaptable) {
				resource = (IResource) ((IAdaptable) selectedObject).getAdapter(IResource.class);
			}
		}
		URI locationURI = null;
		if (resource != null) {
			if (resource.getType() == IResource.FILE) {
				resource = resource.getParent();
			}
			locationURI = resource.getLocationURI();
		} else {
			File file = null;
			if (selectedObject instanceof File) {
				file = (File) selectedObject;
			} else if (selectedObject instanceof String && selectedObject.toString().trim().length() > 0) {
				file = new File(selectedObject.toString());
			} else {
				Control focusControl = Display.getDefault().getFocusControl();
				if (focusControl instanceof Text) {
					String text = ((Text) focusControl).getSelectionText();
					if (text.length() > 0) {
						file = new File(text);
					}
				} else {
					String fileLocation = null;
					Control control = Display.getDefault().getFocusControl();
					if (control instanceof Text) {
						fileLocation = ((Text) control).getSelectionText().trim();
						if (fileLocation.isEmpty()) {
							fileLocation = ((Text) control).getText().trim();
						}
					} else if (control instanceof Label) {
						fileLocation = ((Label) control).getText();
					}

					// get from clipboard
					if (fileLocation == null) {
						Clipboard clipboard = null;
						try {
							clipboard = new Clipboard(Display.getDefault());
							Object contents = clipboard.getContents(TextTransfer.getInstance());
							if (contents == null || contents.toString().trim().length() == 0) {
								contents = clipboard.getContents(FileTransfer.getInstance());
								if (contents instanceof String[]) {
									String[] fileLocations = (String[]) contents;
									if (fileLocations.length > 0) {
										fileLocation = fileLocations[0];
									}
								}
							} else {
								fileLocation = contents.toString().trim();
							}
						} finally {
							if (clipboard != null) {
								clipboard.dispose();
							}
						}
					}

					if (fileLocation != null) {
						file = new File(fileLocation);
					}
				}
				
			}
			if (file != null && file.exists()) {
				if (file.isFile()) {
					file = file.getParentFile();
				}
				locationURI = file.toURI();
			} 
		} 
		
		
		if (locationURI == null) {
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (editor != null) {
            	IEditorInput editorInput = editor.getEditorInput();
            	if (editorInput instanceof IURIEditorInput) {
            		URI uri = ((IURIEditorInput) editorInput).getURI();
            		File file = new File(uri).getParentFile();
            		locationURI = file.toURI();
            	}
            }
		}
		

		if (locationURI != null) {
			try {
				Desktop.getDesktop().browse(locationURI);
			} catch (IOException e) {
				Activator.getDefault().logAndShowException(e);
			}
		}
		return null;
	}
	
	private Object getSelectedObject() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		ISelection selection = activePage.getSelection();
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			return element;
		} else if (selection instanceof ITextSelection) {
			return ((ITextSelection) selection).getText();
		}
		return null;
	}

}