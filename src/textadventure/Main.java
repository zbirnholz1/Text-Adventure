package textadventure;
/* 
 * Credits:
 * 
 * Music copyright (c) 2001-2007 Capcom:
 * 		Phoenix Wright: Ace Attorney (2001), Phoenix Wright, Justice for All (2002),
 * 		Phoenix Wright: Trials and Tribulations (2004), Apollo Justice: Ace Attorney (2007)
 * Music copyright (c) ??? Nintendo:
 * 		INSERT POKEMON GAMES THAT I'M USING MUSIC FROM HERE!!!
 * JSON copyright (c) 2012 Douglas Crockford: Used with permission
 */

public class Main {
	public static Game game;
	
	public static void main(String[] args) {
		game=new Game();
		game.play();
	}
}
