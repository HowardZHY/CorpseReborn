package org.golde.bukkit.corpsereborn.nms.nmsclasses.packetlisteners;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelInboundHandlerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.golde.bukkit.corpsereborn.ConfigData;
import org.golde.bukkit.corpsereborn.CorpseAPI.events.CorpseClickEvent;
import org.golde.bukkit.corpsereborn.Main;
import org.golde.bukkit.corpsereborn.Util;
import org.golde.bukkit.corpsereborn.nms.Corpses.CorpseData;
import org.golde.bukkit.corpsereborn.nms.TypeOfClick;

import java.lang.reflect.Field;

public class PcktIn_v1_7_R4 extends ChannelInboundHandlerAdapter {

	public Player p;

	public PcktIn_v1_7_R4(Player p) {
		this.p = p;
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg instanceof C02PacketUseEntity) {
			final C02PacketUseEntity packet = (C02PacketUseEntity) msg;
			Bukkit.getServer().getScheduler()
			.runTask(Main.getPlugin(), new Runnable() {
				public void run() {
					if (packet.func_149565_c() == C02PacketUseEntity.Action.INTERACT) {
						for (CorpseData cd : Main.getPlugin().corpses.getAllCorpses()) {
							if (cd.getEntityId() == getId(packet)) {
								CorpseClickEvent cce = new CorpseClickEvent(cd, p, TypeOfClick.UNKNOWN);
								Util.callEvent(cce);
								if (ConfigData.hasLootingInventory()) {
									if(!cce.isCancelled()) {
										InventoryView view = p.openInventory(cd.getLootInventory());
										cd.setInventoryView(view);
									}
									break;
								}
							}
						}
					}
				}
			});
		}
		super.channelRead(ctx, msg);
	}

	private int getId(C02PacketUseEntity packet) {
		try {
			Field afield = packet.getClass().getDeclaredField("field_149567_a");
			afield.setAccessible(true);
			int id = afield.getInt(packet);
			afield.setAccessible(false);
			return id;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public static void registerListener(Player p) {
		Channel c = getChannel(p);
		if (c == null) {
			throw new NullPointerException("Couldn't get channel??");
		}
		c.pipeline().addBefore("packet_handler", "packet_in_listener", new PcktIn_v1_7_R4(p));
	}

	public static Channel getChannel(Player p) {
		NetworkManager nm = ((CraftPlayer) p).getHandle().field_71135_a.field_147371_a;
		try {
			Field ifield = nm.getClass().getDeclaredField("field_150746_k");
			ifield.setAccessible(true);
			return (Channel) ifield.get(nm);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
