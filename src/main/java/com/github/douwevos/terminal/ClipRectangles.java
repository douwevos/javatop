package com.github.douwevos.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClipRectangles implements Clip {

	
	private final List<Rectangle> rectangles;
	
	public ClipRectangles() {
		rectangles = new ArrayList<>();
	}

	public ClipRectangles(Rectangle rectangle) {
		rectangles = new ArrayList<>();
		rectangles.add(rectangle);
	}

	
	private ClipRectangles(List<Rectangle> rectangles) {
		this.rectangles = rectangles;
	}
	
	@Override
	public Clip clipIntersect(Rectangle other) {
		if (other == null || rectangles.isEmpty()) {
			return new ClipRectangles();
		}
		
		List<Rectangle> newClipRects = rectangles.stream().map(s -> s.intersection(other)).filter(Objects::nonNull).collect(Collectors.toList());
		return new ClipRectangles(newClipRects);
	}

	public ClipRectangles clipExclude(Rectangle screenRectDialog) {
		List<Rectangle> newRects = rectangles.stream().flatMap(c -> exclude(c, screenRectDialog)).collect(Collectors.toList());
		return new ClipRectangles(newRects);
	}
	
	private Stream<Rectangle> exclude(Rectangle rectangle, Rectangle toExclude) {
		int rLeft = rectangle.x;
		int rRight = rectangle.x+rectangle.width;
		int rTop = rectangle.y;
		int rBottom = toExclude.y+toExclude.height;
		int eLeft = toExclude.x;
		int eRight = toExclude.x+toExclude.width;
		int eTop = toExclude.y;
		int eBottom = toExclude.y+toExclude.height;
		
		
		if ((rLeft>=eRight) || (eLeft>=rRight) 
				|| (rTop>=eBottom) || (eTop>=rBottom)) { 
			return Stream.of(rectangle);
		}
		
		if ((eLeft<=rLeft) && (eRight>=rRight)
				&& (eTop<=rTop) && (eBottom>=rBottom)) {
			return Stream.empty();
		}
		
		int lTop = rTop;
		List<Rectangle> cut = new ArrayList<>();
		if (eTop > rTop) {
			// top block
			cut.add(new Rectangle(rLeft, rTop, rectangle.width, eTop-rTop));
			lTop = eTop;
		}
		
		int lBottom = rBottom;
		if (eBottom<rBottom) {
			// bottom block
			cut.add(new Rectangle(rLeft, eBottom, rectangle.width, rBottom-eBottom));
			lBottom = eBottom;
		}
		
		if (eLeft > rLeft) {
			// left block
			cut.add(new Rectangle(rLeft, lTop, eLeft-rLeft, lBottom-lTop));
		}
		
		if (eRight < rRight) {
			// left block
			cut.add(new Rectangle(eRight, lTop, rRight-eRight, lBottom-lTop));
		}
		return cut.stream();
	}

	
	@Override
	public boolean inside(int x, int y) {
		return rectangles.stream().anyMatch(r -> r.contains(x, y));
	}
	
	@Override
	public boolean intersects(Rectangle rectanlge) {
		return rectangles.stream().anyMatch(r -> r.intersects(rectanlge));
	}
	
	
	@Override
	public Rectangle getOuterBounds() {
		if (rectangles.isEmpty()) {
			return null;
		}
		if (rectangles.size()==1) {
			return rectangles.get(0);
		}
		
		Rectangle result = null;
		for(Rectangle r : rectangles) {
			if (result==null) {
				result = r;
			} else {
				result = result.union(r);
			}
		}
		return result;
	}

	
}
