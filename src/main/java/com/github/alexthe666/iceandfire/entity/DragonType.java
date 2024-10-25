package com.github.alexthe666.iceandfire.entity;

public abstract class DragonType {
    public static final DragonType FIRE = new FireDragonType();
    public static final DragonType ICE = new IceDragonType();
    public static final DragonType LIGHTNING = new LightningDragonType();
    public static final DragonType BLACKFROST = new BlackFrostDragonType();

    private String name;
    private boolean piscivore;

    public abstract int getIntValue();

    private static class FireDragonType extends DragonType {
        public int getIntValue() {
            return 0;
        }
    }

    private static class IceDragonType extends DragonType {
        public int getIntValue() {
            return 1;
        }
    }

    private static class LightningDragonType extends DragonType {
        public int getIntValue() {
            return 2;
        }
    }

    private static class BlackFrostDragonType extends DragonType {
        public int getIntValue() {
            return 3;
        }
    }

    public static int getIntFromType(DragonType type) {
        return type.getIntValue();
    }

    public static String getNameFromInt(int type){
        if(type == 2){
            return "lightning";
        }else if (type == 1){
            return "ice";
        } else if (type == 3){
            return "black_frost";
        }else{
            return "fire";
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPiscivore() {
        return piscivore;
    }

    public DragonType setPiscivore() {
        piscivore = true;
        return this;
    }
}