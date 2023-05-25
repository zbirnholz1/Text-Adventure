package textadventure;
/* 
 * Credits:
 * 
 * Music is all licensed under Creative Commons.
 * JSON copyright (c) 2012 Douglas Crockford. JSON is licensed (and used) for good and not evil.
 */

public class Main {
	public static Game game;
	
	public static void main(String[] args) {
		game=new Game();
		game.play();
	}
}
