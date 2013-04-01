package org.libjpegturbo.turbojpeg;

public class TJRect {
	public TJRect() {
	}
	public TJRect(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	}
	public TJRect(TJRect r) {
		this.x = r.x;
		this.y = r.y;
		this.width = r.width;
		this.height = r.height;
	}
	public int x;
	public int y;
	public int width;
	public int height;
}
