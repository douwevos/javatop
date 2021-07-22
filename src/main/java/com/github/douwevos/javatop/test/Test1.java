package com.github.douwevos.javatop.test;

public class Test1 {

	
	public static void main(String[] args) {
		Test1 test1 = new Test1();
		test1.run();
	}
	
	private void run() {
		long a = 0;
		long sum = 0;
		while(true) {
			a = (a*61)/60+10;
			if (a>50000l) {
				a = -49990l;
			}
			if (a<0) {
				a = a & 0x7fffL;
			}
			for(long s=0; s<a; s++) {
				sum += a-s;
			}
			
			if ((a%5) == 0) {
				System.out.println("a="+a+", sum="+sum);
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
			}
		}
	}
	
}
