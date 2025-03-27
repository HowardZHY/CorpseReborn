package org.golde.bukkit.corpsereborn.nms.nmsclasses;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.com.mojang.authlib.GameProfile;

public class EntityCorpse extends EntityPlayer {

    public EntityCorpse(net.minecraft.world.World world, GameProfile gameprofile) {
        super(world, gameprofile);
    }

    protected void func_70088_a() {
        super.func_70088_a();
        this.field_70180_af.func_75682_a(10, (byte) 0);
    }

    public void sendMessage(IChatComponent arg0) {}

    public boolean func_70003_b(int arg0, String arg1) {
        return false;
    }

    @Override
    public ChunkCoordinates getChunkCoordinates() {
        return new ChunkCoordinates(0, 0, 0);
    }

}
