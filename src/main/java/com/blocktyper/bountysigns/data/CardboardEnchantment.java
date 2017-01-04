package com.blocktyper.bountysigns.data;

import java.io.Serializable;

import org.bukkit.enchantments.Enchantment;

public class CardboardEnchantment  implements Serializable {
    private static final long serialVersionUID = 201611061552L;
 
    private final String name;
 
	public CardboardEnchantment(Enchantment enchantment) {
        this.name = enchantment.getName();
    }
 
	public Enchantment unbox() {
        return Enchantment.getByName(name);
    }
	
	public String getName() {
        return name;
    }
	
	public static CardboardEnchantment fromName(String name){
		return new CardboardEnchantment(Enchantment.getByName(name));
	}
}
