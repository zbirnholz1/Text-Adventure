package textadventure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class BattleCalculator
{
	public static final int MAX_PROXIMITY=4;

	public static final Weapon BASIC_ATTACK=new Weapon(1, 1, 0.70, 0.65, 0.0, null, false);

	public static final Map<AttackType, Map<ArmorType, Double>> MULTIPLIERS=initializeMultipliers();

	public static int calculateDamage(TACharacter attacker, TACharacter defender, Weapon attack)
	{
		double probability = attack.getAccuracy() - (attack.getRangeSlope())*(Math.abs(attacker.getProximity() - defender.getProximity())-1) - (defender.getSpeed() /*- defender.getArmor().getWeight()*/)/200.0;
		if(defender.getArmor()!=null)
			probability+=defender.getArmor().getWeight()/200;
		double roll = Math.random();
		int hit = 0;
		if (roll < probability)
		{
			if((!attack.isMelee() && probability < .57) || (attack.isMelee() && probability < .695)) { //if the crit chance is less than 7%, it becomes 7%.
				if(Math.random() < 0.07)
					hit = -2;
				else
					hit = 1;
			}
			else {
				if ((!attack.isMelee() && probability - roll > .5)||(attack.isMelee() && probability - roll > .625))
				{
					hit = -2; //negative means it was a critical hit
				}
				else hit = 1;
			}
		}
		if (hit == 0)
			return 0; //0 damage means that the attack missed
		double typeMultiplier=getMultiplier(attack, defender.getArmor());
		double variable=Math.random()*0.15 + 0.925;
		int armor=0;
		if(defender.getArmor()!=null)
			armor=defender.getArmor().getRating();
		int damage=0;
		if(attack.isASpell())
			damage = (int)Math.round((attack.getDamage() + attacker.getIntelligence() - armor - defender.getIntelligence()/4) * hit * typeMultiplier * variable);
		else
			damage = (int)Math.round((attack.getDamage() + attacker.getStrength() - armor - defender.getStrength()/4) * hit * typeMultiplier * variable);
		if(damage<1 && hit>0)
			damage=Math.max(damage, 1); //every attack must do at least 1 damage
		return damage;
	}

	private static double getMultiplier(Weapon attack, Armor armor) {
		if(attack==null||attack.getType()==null)
			return 1.0;
		else if(armor==null||armor.getType()==null)
			return 1.0;
		return MULTIPLIERS.get(attack.getType()).get(armor.getType());
	}

	public static Map<AttackType, Map<ArmorType, Double>> initializeMultipliers() {
		Map<AttackType, Map<ArmorType, Double>> map=new HashMap<AttackType, Map<ArmorType, Double>>();
		BufferedReader reader=new BufferedReader(new InputStreamReader(BattleCalculator.class.getClass().getResourceAsStream("/typechart.taf")));
		try {
			String line=reader.readLine();
			StringTokenizer armorTokenizer=new StringTokenizer(line);
			ArmorType[] armorTypes=new ArmorType[armorTokenizer.countTokens()];
			for(int i=0; i<armorTypes.length; i++)
				armorTypes[i]=ArmorType.valueOf(armorTokenizer.nextToken());
			line=reader.readLine();
			while(line!=null) {
				StringTokenizer tokenizer=new StringTokenizer(line);
				AttackType attackType=AttackType.valueOf(tokenizer.nextToken());
				Map<ArmorType, Double> armorMap=new HashMap<ArmorType, Double>();
				for(int i=0; i<armorTypes.length; i++)
					armorMap.put(armorTypes[i], Double.parseDouble(tokenizer.nextToken()));
				map.put(attackType, armorMap);
				line=reader.readLine();
			}
			reader.close();
		} catch(IOException e) {
			System.out.println("Something went wrong: "+e);
			e.printStackTrace();
		}
		return map;
	}

	public static void beginCombat(boolean afterPlayer) {
		beginCombat(Main.game.getPlayer().getRoom().getCharactersBySpeed(), afterPlayer);
	}

	public static void beginCombat(List<TACharacter> fighters, boolean afterPlayer) {
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
					totalSpeed+=Main.game.getPlayer().getSpeed();
					startIndex++;
					afterPlayer=false;
				}
				for(int i=startIndex; i<fighters.size(); i++) {
					totalSpeed+=fighters.get(i).getSpeed();
					double probability=fighters.get(i).getSpeed()/(((double)totalSpeed)/(i+1));
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
			makeMoves(fighters, turns);
		}
	}

	public static void makeMoves(List<TACharacter> fighters, int[] turns) {
		for(int i=0; i<turns.length; i++) {
			if(Main.game.getPlayer().getHP()==0)
				return;
			if(fighters.get(i) instanceof Player)
				continue;
			if(turns[i]==0) {
				Main.game.getView().printlnNPC("{1000}"+fighters.get(i).getFullName()+" doesn't have a chance to react.");
			}
			else {
				fighters.get(i).attack(Main.game.getPlayer());
				for(int t=1; t<turns[i]; t++) {
					if(Main.game.getPlayer().getHP()==0)
						return;
					//TODO implement allied characters?!????????
					Main.game.getView().printlnNPC("{1000}You don't have a chance to react to "+fighters.get(i).getFullName()+".{1000}");
					fighters.get(i).attack(Main.game.getPlayer());
				}
			}
		}
	}
}
