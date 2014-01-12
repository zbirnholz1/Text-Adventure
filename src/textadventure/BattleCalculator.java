package textadventure;

public class BattleCalculator
{
    public static int calculateDamage(TACharacter attacker, TACharacter defender, Weapon attack)
    {
        double probability = attack.getAccuracy() - (attack.getSlope())*Math.abs(attacker.getProximity() - defender.getProximity()) - (defender.getSpeed() - defender.getArmor().getWeight())/200;
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
        int damage = (attack.getDamage() + attacker.getStrength() - defender.getArmor().getRating()) * hit;
        //TODO account for Material and Structure
        return Math.max (1, damage); //every attack must do at least 1 damage
    }
}
        