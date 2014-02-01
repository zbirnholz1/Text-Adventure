package textadventure;

import java.util.List;

public class BattleCalculator
{
	public static final int MAX_PROXIMITY=4;
	
    public static int calculateDamage(TACharacter attacker, TACharacter defender, Weapon attack)
    {
        double probability = attack.getAccuracy() - (attack.getRangeSlope())*Math.abs(attacker.getProximity() - defender.getProximity()) - (defender.getSpeed() - defender.getArmor().getWeight())/200;
        double roll = Math.random();
        int hit = 0;
        if (roll < probability)
        {
            if (probability - roll > .5)
            {
                hit = 2;
            }
            else hit = 1;
        }
        if (hit == 0)
        	return 0; //0 damage means that the attack missed
        int typeMultiplier=getMultiplier(attack, defender.getArmor());
        int damage = (attack.getDamage() + attacker.getStrength() - defender.getArmor().getRating()) * hit * typeMultiplier;
        //TODO account for Material and Structure
        return Math.max (1, damage); //every attack must do at least 1 damage
    }
    
    private static int getMultiplier(Weapon attack, Armor armor) {
    	return 1;
    }
    
    public static void beginCombat(boolean afterPlayer) {
		List<TACharacter> fighters=Main.game.getPlayer().getRoom().getCharactersBySpeed();
		if(fighters.size()>1) {
			int[] turns=new int[fighters.size()];
			boolean stop=false;
			while(!stop) {
				int startIndex=0;
				double totalSpeed=0;
				if(afterPlayer) {
					for(int j=0; !(fighters.get(j) instanceof Player); j++) {
						totalSpeed+=fighters.get(j).getSpeed();
						startIndex++;
					}
					startIndex++;
					afterPlayer=false;
				}
				for(int i=startIndex; i<fighters.size(); i++) {
					totalSpeed+=fighters.get(i).getSpeed();
					double probability=fighters.get(i).getSpeed()/(totalSpeed/(i+1));
					if(fighters.get(i).equals(Main.game.getPlayer())) {
						if(Math.random()<probability) {
							stop=true;
							break;
						}
					}
					else {
						if(Math.random()<probability)
							turns[i]++;
					}
				}
			}
			makeMoves(turns);
		}
	}

	public static void makeMoves(int[] turns) {
		List<TACharacter> fighters=Main.game.getPlayer().getRoom().getCharactersBySpeed();
		for(int i=0; i<turns.length; i++) {
			if(turns[i]==0)
				Main.game.getView().println("The "+fighters.get(i).getFullName()+" doesn't have a chance to react.");
			else {
				fighters.get(i).attack(Main.game.getPlayer());
				for(int t=1; t<turns[i]; t++) {
					//TODO implement allied characters?!????????
					Main.game.getView().println("You don't have a chance to react to the "+fighters.get(i).getFullName());
					fighters.get(i).attack(Main.game.getPlayer());
					if(Main.game.getPlayer().getHP()==0)
						return;
				}
			}
		}
	}
}
        