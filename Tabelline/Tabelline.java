class Calcolatore implements Runnable {
	int base;
	Calcolatore(int base) {
		this.base = base;
	}
	public void run() {
		for (int i = 1; i < 10; i++) {
			System.out.println(Thread.currentThread().getName() + ": " + base + " * " + i + " = " + base*i);
		}
	}
}

public class Tabelline {
	public static void main(String args[]) {
		for (int i = 1; i <= 10; i++) {
			Thread thread = new Thread(new Calcolatore(i));
			thread.start();
		}
		System.out.println("Threads avviati");
	}
}
