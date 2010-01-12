package com.predic8.plugin.membrane.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.predic8.plugin.membrane.views.BrowserView;
import com.predic8.plugin.membrane.views.ExchangesView;
import com.predic8.plugin.membrane.views.RequestView;
import com.predic8.plugin.membrane.views.ResponseView;
import com.predic8.plugin.membrane.views.RuleDetailsView;
import com.predic8.plugin.membrane.views.RuleStatisticsView;
import com.predic8.plugin.membrane.views.RulesView;

public class MembranePerspective implements IPerspectiveFactory {

	public static final String PERSPECTIVE_ID = "com.predic8.plugin.membrane.perspectives.MembranePerspective";
	
	public boolean showSingle;
	
	public void createInitialLayout(IPageLayout layout) {
		
		layout.setEditorAreaVisible(false);
		
		if (showSingle) {
			IFolderLayout centerLayoutFolder = layout.createFolder("center folder", IPageLayout.TOP, 1.0f, IPageLayout.ID_EDITOR_AREA);
			centerLayoutFolder.addPlaceholder(RequestView.VIEW_ID);
			centerLayoutFolder.addPlaceholder(ResponseView.VIEW_ID);
			centerLayoutFolder.addPlaceholder(BrowserView.VIEW_ID);
			centerLayoutFolder.addPlaceholder(RuleStatisticsView.VIEW_ID);
			centerLayoutFolder.addPlaceholder(ExchangesView.VIEW_ID);
		} else {
			IFolderLayout topLayoutFolder = layout.createFolder("top folder", IPageLayout.TOP, 0.50f, IPageLayout.ID_EDITOR_AREA);
			topLayoutFolder.addPlaceholder(RuleDetailsView.VIEW_ID);
			topLayoutFolder.addView(ExchangesView.VIEW_ID);
			topLayoutFolder.addPlaceholder(BrowserView.VIEW_ID);
			topLayoutFolder.addPlaceholder(RuleStatisticsView.VIEW_ID);
			
			
			IFolderLayout topLeftLayoutFolder = layout.createFolder("top left folder", IPageLayout.LEFT, 0.25f, "top folder");
			topLeftLayoutFolder.addView(RulesView.VIEW_ID);
			
			IFolderLayout southLayoutFolder = layout.createFolder("south folder", IPageLayout.BOTTOM, 0.50f, IPageLayout.ID_EDITOR_AREA);
			southLayoutFolder.addView(RequestView.VIEW_ID);
			southLayoutFolder.addView(ResponseView.VIEW_ID);
		}
		
		layout.setFixed(true);
	}

	public boolean isShowSingle() {
		return showSingle;
	}

	public void setShowSingle(boolean showSingle) {
		this.showSingle = showSingle;
	}

}
