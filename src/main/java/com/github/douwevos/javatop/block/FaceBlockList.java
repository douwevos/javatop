package com.github.douwevos.javatop.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.douwevos.javatop.block.JvmBlockSampler.BlockClassDetails;
import com.github.douwevos.javatop.block.JvmBlockSampler.BlockInfo;
import com.github.douwevos.javatop.block.JvmBlockSampler.BlockInfoMap;
import com.github.douwevos.javatop.block.JvmBlockSampler.LockKey;
import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.Rectangle;
import com.github.douwevos.terminal.UpdateContext;
import com.github.douwevos.terminal.KeyEvent.KeyCode;
import com.github.douwevos.terminal.component.FaceComponentScrollable;

public class FaceBlockList extends FaceComponentScrollable {

	private final Observer observer;
	private final Model model = new Model();
	
	private List<BlockInfoMap> snapshot;
	
	private int selectRow = 0;
	private int selectColumn = 0;
	
	public FaceBlockList(Observer observer) {
		this.observer = observer;
	}
	
	public void setSnapshot(List<BlockInfoMap> snapshot) {
		if (this.snapshot == snapshot) {
			return;
		}
		
		model.update(snapshot);
		
		// move selection with new snapshot
		if (this.snapshot != null) {
			BlockInfoMap selectedInfoMap = this.snapshot.get(selectRow);
			int newIndex = snapshot.indexOf(selectedInfoMap);
			if (newIndex<0) {
				
				if (selectedInfoMap.hasBlocked() || selectRow>=snapshot.size()) {
					selectRow = 0;
					selectColumn = 0;
				} else {
					BlockInfoMap old = snapshot.get(selectRow);
					if (selectColumn>=old.getMap().size()) {
						selectColumn = old.getMap().size()-1;
					}
				}
			} else {
				selectRow = newIndex;
			}
		}
		
		this.snapshot = snapshot;
		validate();
		markUpdateFlag(UF_PAINT);
		if (this.snapshot == null) {
			markUpdateFlag(UF_CLEAR);
		}
	}

	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		KeyCode code = keyEvent.getCode();
		if (code != null) {
			switch(code) {
				case CursorDown : {
					if (selectRow+1<model.rowCount()) {
						setSelection(selectRow+1, selectColumn);
						ensureItemInView();
					}
					return true;
				} 
				
				case CursorUp : {
					if (selectRow>0) {
						setSelection(selectRow-1, selectColumn);
						ensureItemInView();
					}
					return true;
				}
				
				case CursorRight : 
					moveSelectionRight();
					return true;

				case CursorLeft : 
					moveSelectionLeft();
					return true;
				
				default :
					break;
			}
		}
				
		return false;
	}
	

	private void moveSelectionRight() {
		ModelRow modelRow = model.getRow(selectRow);
		int max = modelRow.infoCount(modelRow.hasBlocked());
		if (selectColumn+1<max) {
			setSelection(selectRow, selectColumn+1);
			ensureItemInView();
		}
	}


	private void moveSelectionLeft() {
		if (selectColumn>0) {
			setSelection(selectRow, selectColumn-1);
			ensureItemInView();
		}
	}
	
	
	private void setSelection(int row, int column) {
		if ((this.selectRow==row) && (this.selectColumn== column)){
			return;
		}
		markUpdateFlag(UF_PAINT);
		
		selectRow = row;
		selectColumn = column;
		notifySelectChanged();
	}
	
	private void notifySelectChanged() {
		ModelItem itemSelected = null;
		if (selectRow<model.rowCount()) {
			ModelRow row = model.getRow(selectRow);
			if (selectColumn<row.columnCount()) {
				itemSelected = row.getColumn(selectColumn);
			}
		}
		observer.onNewSelection(itemSelected);
	}

	
	private void ensureItemInView() {
		Rectangle itemRectangle = model.getItemRectangle(selectRow, selectColumn);
		ensureRectangleInView(itemRectangle);
//		int viewTop = getViewY();
//		int viewBottom = viewTop+getHeight();
//
//		int topOfItem = highlightedItem*itemHeight;
//		int bottomOfItem = topOfItem + itemHeight;
//		
//		if (bottomOfItem>=viewBottom) {
//			setViewY(bottomOfItem-getHeight()+1);
//			viewTop = getViewY();
//		}
//		
//		if (topOfItem<viewTop) {
//			setViewY(topOfItem);
//		}
	}




	private void validate() {
		
		setViewWidth(model.getViewWidth(false));
		setViewHeight(model.getViewHeight(false));
		
	}


	@Override
	public void paint(UpdateContext context) {
		if (snapshot == null) {
			return;
		}
		
		int y = 0;
		
		int rowNr = 0;
		
		for(ModelRow modelRow : model) {
			
			boolean hasBlocked = modelRow.hasBlocked();
			
			int columnNr = 0;
			int x = 0;
			for(ModelItem item : modelRow) {
				
				boolean infoIsSelected = rowNr==selectRow && columnNr==selectColumn;
				
				columnNr++;
				
				if (hasBlocked && item.blockInfo.blockedCount()==0) {
					continue;
				}
				drawBlockInfo(context, x, y, item, infoIsSelected);
				
				Rectangle rectangle = new Rectangle(x+item.width, y, 1, modelRow.heightAll);
				rectangle = context.clipRectangle(rectangle);
				if (rectangle!=null) {
					context.clearRectangle(rectangle, context.getFormatDefault());
				}

				
				x += item.width+1;
			}
			
			int rowHeight = modelRow.getViewHeight(true);
			Rectangle rectangle = new Rectangle(x, y, getViewX()+getWidth(), rowHeight);
			context.clearRectangle(rectangle, context.getFormatDefault());

			y += rowHeight;
			rectangle = new Rectangle(0, y, getViewX()+getWidth(), 1);
			context.clearRectangle(rectangle, context.getFormatDefault());
			
			y++;
			rowNr++;
		}
	}


	private void drawBlockInfo(UpdateContext context, int x, int y, ModelItem modelItem, boolean asSelected) {
//		int left = blockIndex * (BLOCK_WIDTH+1);
		BlockInfo blockInfo = modelItem.blockInfo;
		int itemWidth = modelItem.width;
		int height = modelItem.height;
		
		Rectangle rectangle = new Rectangle(x, y, itemWidth, height);
		Rectangle visibleRect = context.clipRectangle(rectangle);
		if (visibleRect!=null) {
			int background = asSelected ? AnsiFormat.ANSI_BLUE : AnsiFormat.ANSI_GRAY;
			context.clearRectangle(rectangle, new AnsiFormat(background, -1, false));

			context.drawString(x, itemWidth, y, modelItem.blockInfo.getLockKey().name(), new AnsiFormat(background, AnsiFormat.ANSI_BRIGHT_WHITE, false), true);
			y++;

			if (blockInfo.getOwner()!=null) {
				String tid = ""+blockInfo.getOwner().getThreadId();
				int labelLength = ModelItem.LABEL_OWNER.length();
				context.drawString(x, labelLength, y, ModelItem.LABEL_OWNER, new AnsiFormat(background, AnsiFormat.ANSI_CYAN, false), true);
				context.drawString(x+labelLength, itemWidth-labelLength, y, tid, new AnsiFormat(background, AnsiFormat.ANSI_GREEN, false), true);
				y++;
			}

			if (modelItem.txtBlockedPids != null) {
				int labelLength = ModelItem.LABEL_BLOCKED.length();
				context.drawString(x, labelLength, y, ModelItem.LABEL_BLOCKED, new AnsiFormat(background, AnsiFormat.ANSI_CYAN, false), true);
				context.drawString(x+labelLength, itemWidth-labelLength, y, modelItem.txtBlockedPids, new AnsiFormat(background, AnsiFormat.ANSI_BRIGHT_RED, true), true);
				y++;
			}
		}
	}

	
	private static class Model implements Iterable<ModelRow> {
		
		private List<ModelRow> rows = new ArrayList<>();
		
		public void update(List<BlockInfoMap> snapshot) {
			if (snapshot == null) {
				rows.clear();
				return;
			}
			
			List<ModelRow> newRows = new ArrayList<>();
			for(BlockInfoMap infoMap : snapshot) {
				ModelRow modelRow = rows.stream().filter(s -> s.getBlockInfoMap()==infoMap).findAny().orElse(null);
				if (modelRow!=null) {
					newRows.add(modelRow);
					continue;
				}
				
				modelRow = new ModelRow(infoMap);
				newRows.add(modelRow);
			}
			rows = newRows;
			
		}

		public ModelRow getRow(int index) {
			return rows.get(index);
		}

		public Rectangle getItemRectangle(int row, int column) {
			int startY = 0;
			for(int idx=0; idx<row; idx++) {
				startY += rows.get(idx).heightAll;
				startY++;
			}
			
			ModelRow modelRow = rows.get(row);
			int startX = 0;
			int endX = 0;
			int endY = startY;
			for(ModelItem item : modelRow) {
				if (column==0) {
					endX = startX + item.width;
					// This is not correct ... should be the item height;
					endY += modelRow.heightAll;
					break;
				}
				startX += item.width+1;
				column--;
			}
			
			return new Rectangle(startX, startY, endX-startX, endY-startY);
		}

		public int rowCount() {
			return rows.size();
		}

		public int getViewHeight(boolean all) {
			return rows.stream().mapToInt(r -> r.getViewHeight(all)).sum();
		}

		public int getViewWidth(boolean all) {
			return rows.stream().mapToInt(r -> r.getViewWidth(all)).max().orElse(0);
		}
		
		
		@Override
		public Iterator<ModelRow> iterator() {
			return rows.iterator();
		}
		
		
	}
	
	private static class ModelRow implements Iterable<ModelItem> {
		
		private final BlockInfoMap blockInfoMap;
		private final List<ModelItem> items;

		private final int itemsWithBlocked;
		private final int widthAll;
		private final int heightAll;
		private final int widthBlocked;
		private final int heightBlocked;
		
		
		public ModelRow(BlockInfoMap blockInfoMap) {
			this.blockInfoMap = blockInfoMap;
			Map<LockKey, BlockInfo> map = blockInfoMap.getMap();
			List<BlockInfo> line = new ArrayList<>(map.values());
			Comparator<? super BlockInfo> c = (a,b) -> Integer.compare(b.blockedCount(), a.blockedCount());
			line.sort(c);
			items = line.stream().map(s -> new ModelItem(s)).collect(Collectors.toList());
			
			int itemsWithBlocked = 0;
			int widthAll = 0;
			int heightAll = 0;
			int widthBlocked = 0;
			int heightBlocked = 0;
			
			for(int idx=0; idx<items.size(); idx++) {
				ModelItem modelItem = items.get(idx);
				BlockInfo blockInfo = modelItem.blockInfo; 
				int blockedCount = blockInfo.blockedCount();
				int height = modelItem.height;
				int width = modelItem.width + (idx==0 ? 0 : 1);
				
				if (blockedCount>0) {
					widthBlocked += width;
					itemsWithBlocked++;
					if (heightBlocked<height) {
						heightBlocked = height;
					}
				}

				widthAll += width;
				if (heightAll<height) {
					heightAll = height;
				}
			}
			
			this.itemsWithBlocked = itemsWithBlocked;
			this.widthAll = widthAll;
			this.heightAll = heightAll;
			this.widthBlocked = widthBlocked;
			this.heightBlocked = heightBlocked;
		}

		public int columnCount() {
			return items.size();
		}

		public ModelItem getColumn(int index) {
			return items.get(index);
			
		}

		public boolean hasBlocked() {
			return blockInfoMap.hasBlocked();
		}

		public BlockInfoMap getBlockInfoMap() {
			return blockInfoMap;
		}
		
		public int infoCount(boolean all) {
			return all ? items.size() : itemsWithBlocked;
		}
		
		public int getViewWidth(boolean all) {
			return all ? widthAll : widthBlocked;
		}

		public int getViewHeight(boolean all) {
			return all ? heightAll : heightBlocked;
		}
		
		@Override
		public Iterator<ModelItem> iterator() {
			return items.iterator();
		}

	}

	public static class ModelItem {
		
		public static final String LABEL_OWNER = "owner  :";
		public static final String LABEL_BLOCKED = "blocked:";
		
		public final BlockInfo blockInfo;
		public final String txtBlockedPids;
		public final int width;
		public final int height;
		
		public ModelItem(BlockInfo blockInfo) {
			this.blockInfo = blockInfo;
			
			int width = 6;
			String name = blockInfo.getLockKey().name();
			if (name.length()>width) {
				width = name.length();
			}
			
			int height = 1;
			if (blockInfo.getOwner()!=null) {
				String j = LABEL_OWNER+blockInfo.getOwner().getThreadId();
				if (width<j.length()) {
					width = j.length();
				}
				height++;
			}
			if (blockInfo.blockedCount()>0) {
				height++;
				StringBuilder buf = new StringBuilder();
				for(BlockClassDetails bcd : blockInfo.getBlocked()) {
					if (buf.length()>0) {
						buf.append(',');
					}
					buf.append(bcd.getThreadId());
				}
				txtBlockedPids = buf.toString();
				int l = LABEL_BLOCKED.length() + txtBlockedPids.length();
				if (l>width) {
					width = l;
				}
				
			} else {
				txtBlockedPids = null;
			}

			this.width = width;
			this.height = height;

		}
	}

	
	public interface Observer {
		void onNewSelection(ModelItem item);
	}
	
}
