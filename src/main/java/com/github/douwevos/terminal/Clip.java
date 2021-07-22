package com.github.douwevos.terminal;

public interface Clip {

	boolean inside(int x, int y);
	
	boolean intersects(Rectangle rectanlge);

	Rectangle getOuterBounds();
//	
//	Clip merge(Clip clip);

	Clip clipIntersect(Rectangle rectangle);
	
}
