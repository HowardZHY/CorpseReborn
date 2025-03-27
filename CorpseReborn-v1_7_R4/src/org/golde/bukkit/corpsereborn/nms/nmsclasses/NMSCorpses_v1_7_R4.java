package org.golde.bukkit.corpsereborn.nms.nmsclasses;

import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.*;
import net.minecraft.util.MathHelper;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.golde.bukkit.corpsereborn.ConfigData;
import org.golde.bukkit.corpsereborn.Main;
import org.golde.bukkit.corpsereborn.Util;
import org.golde.bukkit.corpsereborn.nms.Corpses;
import org.golde.bukkit.corpsereborn.nms.NmsBase;
import org.golde.bukkit.corpsereborn.nms.nmsclasses.packetlisteners.PcktIn_v1_7_R4;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unused")
public class NMSCorpses_v1_7_R4 extends NmsBase implements Corpses {

	public List<CorpseData> corpses;

	public NMSCorpses_v1_7_R4() {
		corpses = new ArrayList<>();
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), new Run(), 0L, 1L);
	}

	public class Run implements Runnable {
		public void run() {
			tick();
		}
	}

	public static DataWatcher clonePlayerDatawatcher(Player player, int currentEntId) {
		EntityPlayer h = new EntityCorpse(((CraftWorld) player.getWorld()).getHandle(), ((CraftPlayer) player).getProfile());
		try {
			h.func_145769_d(currentEntId);
			return h.func_70096_w();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new DataWatcher(h);
	}

	public GameProfile cloneProfileWithRandomUUID(GameProfile gameProfile, String name) {
		GameProfile newProf = new GameProfile(UUID.randomUUID(), name);
		Map<String, Collection<Property>> originalProps = gameProfile.getProperties().asMap();
		for (Map.Entry<String, Collection<Property>> entry : originalProps.entrySet()) {
			for (Property prop : entry.getValue()) {
				newProf.getProperties().put(entry.getKey(), prop);
			}
		}
		return newProf;
	}

	public Location getNonClippableBlockUnderPlayer(Location loc, int addToYPos) {
		if (loc.getBlockY() < 0) {
			return null;
		}
		for (int y = loc.getBlockY(); y >= 0; y--) {
			Material m = loc.getWorld()
					.getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType();
			if (m.isSolid()) {
				return new Location(loc.getWorld(), loc.getX(), y + addToYPos,
						loc.getZ());
			}
		}
		return null;
	}

	public CorpseData spawnCorpse(Player p, String overrideUsername, Location loc, Inventory inv, int facing) {
		int entityId = getNextEntityId();
		GameProfile prof = cloneProfileWithRandomUUID(((CraftPlayer) p).getProfile(), ConfigData.showTags() ? ConfigData.getUsername(p.getName(), overrideUsername) : "");
		DataWatcher dw = clonePlayerDatawatcher(p, entityId);
		//dw.func_75692_b(10, ((CraftPlayer) p).getHandle().func_70096_w().func_75683_a(10));
		Location locUnder = getNonClippableBlockUnderPlayer(loc, 1);
		Location used = locUnder != null ? locUnder : loc;
		used.setYaw(loc.getYaw());
		used.setPitch(loc.getPitch());
		NMSCorpseData data = new NMSCorpseData(prof, used, dw, entityId, ConfigData.getCorpseTime() * 20, inv, facing);
		if (p.getKiller() != null) {
			data.killerName = p.getKiller().getName();
			data.killerUUID = p.getKiller().getUniqueId();
		}
		data.corpseName = p.getName();
		data.player = p;
		corpses.add(data);
		spawnSlimeForCorpse(data);
		return data;
	}
	
	@Override
	public CorpseData loadCorpse(String gpName, String gpJSON, Location loc, Inventory items, int facing) {
		return null;
	}
	
	public static DataWatcher clonePlayerDatawatcher(GameProfile gp, World world, int currentEntId) {
		EntityPlayer h = new EntityCorpse(((CraftWorld) world).getHandle(), gp);
		h.func_145769_d(currentEntId);
		return h.func_70096_w();
	}

	public void removeCorpse(CorpseData data) {
		corpses.remove(data);
		data.destroyCorpseFromEveryone();
		if (data.getLootInventory() != null) {
			data.getLootInventory().clear();
			List<HumanEntity> close = new ArrayList<HumanEntity>(data
					.getLootInventory().getViewers());
			for (HumanEntity p : close) {
				p.closeInventory();
			}
		}
		deleteSlimeForCorpse(data);
	}

	public int getNextEntityId() {
		try {
			Field entityCount = Entity.class.getDeclaredField("field_70152_a");
			entityCount.setAccessible(true);
			int id = entityCount.getInt(null);
			entityCount.setInt(null, id + 1);
			return id;
		} catch (Exception e) {
			e.printStackTrace();
			return (int) Math.round(Math.random() * Integer.MAX_VALUE * 0.25);
		}
	}

	@SuppressWarnings("all")
	public class NMSCorpseData implements CorpseData {

		public String corpseName;
		private Map<Player, Boolean> canSee;
		private Map<Player, Integer> tickLater;
		private GameProfile prof;
		private Location loc;
		private DataWatcher metadata;
		private int entityId;
		private int ticksLeft;
		private Inventory items;
		private InventoryView iv;
		private Player player;
		private int slot;
		private int rotation;
		
		private String killerName;
		private UUID killerUUID;

		public NMSCorpseData(GameProfile prof, Location loc,
				DataWatcher metadata, int entityId, int ticksLeft,
				Inventory items, int rotation) {
			this.prof = prof;
			this.loc = loc;
			this.metadata = metadata;
			this.entityId = entityId;
			this.ticksLeft = ticksLeft;
			this.canSee = new HashMap<Player, Boolean>();
			this.tickLater = new HashMap<Player, Integer>();
			this.items = items;
			this.rotation = rotation;
			if(rotation >3 || rotation < 0) {
				this.rotation = 0;
			}
		}


		@Override
		public int getRotation() {
			return rotation;
		}

		public ItemStack convertBukkitToMc(org.bukkit.inventory.ItemStack stack){
			return CraftItemStack.asNMSCopy(stack);
			/*if(stack == null){
				return null;	
			}
			ItemStack temp = new ItemStack(Item.getById(stack.getTypeId()), stack.getAmount());
			temp.setData((int)stack.getData().getData());
			if(stack.getEnchantments().size() >= 1) {
				temp.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);//Dummy enchantment
			}
			return temp;*/
		}

		public void setCanSee(Player p, boolean canSee) {
			this.canSee.put(p, canSee);
		}

		public boolean canSee(Player p) {
			return canSee.get(p);
		}

		public void removeFromMap(Player p) {
			canSee.remove(p);
		}

		public boolean mapContainsPlayer(Player p) {
			return canSee.containsKey(p);
		}

		public Set<Player> getPlayersWhoSee() {
			return canSee.keySet();
		}

		public void removeAllFromMap(Collection<Player> players) {
			canSee.keySet().removeAll(players);
		}

		public void setTicksLeft(int ticksLeft) {
			this.ticksLeft = ticksLeft;
		}

		public int getTicksLeft() {
			return ticksLeft;
		}

		public S0CPacketSpawnPlayer getSpawnPacket() {
			S0CPacketSpawnPlayer packet = new S0CPacketSpawnPlayer();
			try {
				Field a = packet.getClass().getDeclaredField("field_148957_a");
				a.setAccessible(true);
				a.set(packet, entityId);
				Field b = packet.getClass().getDeclaredField("field_148955_b");
				b.setAccessible(true);
				b.set(packet, prof);
				Field c = packet.getClass().getDeclaredField("field_148956_c");
				c.setAccessible(true);
				c.setInt(packet, MathHelper.func_76128_c(loc.getX() * 32.0D));
				Field d = packet.getClass().getDeclaredField("field_148953_d");
				d.setAccessible(true);
				d.setInt(packet, MathHelper.func_76128_c((loc.getY() + 2.1) * 32.0D));
				Field e = packet.getClass().getDeclaredField("field_148954_e");
				e.setAccessible(true);
				e.setInt(packet, MathHelper.func_76128_c(loc.getZ() * 32.0D));
				Field f = packet.getClass().getDeclaredField("field_148951_f");
				f.setAccessible(true);
				f.setByte(packet, (byte) (int) (loc.getYaw() * 256.0F / 360.0F));
				Field g = packet.getClass().getDeclaredField("field_148952_g");
				g.setAccessible(true);
				g.setByte(packet, (byte) (int) (loc.getPitch() * 256.0F / 360.0F));
				Field i = packet.getClass().getDeclaredField("field_148960_i");
				i.setAccessible(true);
				i.set(packet, metadata);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return packet;
		}

		public S0APacketUseBed getBedPacket() {
			S0APacketUseBed packet = new S0APacketUseBed();
			try {
				Field a = packet.getClass().getDeclaredField("field_149097_a");
				a.setAccessible(true);
				a.setInt(packet, entityId);

				Field b = packet.getClass().getDeclaredField("field_149095_b");
				b.setAccessible(true);
				b.setInt(packet, loc.getBlockX());

				Field c = packet.getClass().getDeclaredField("field_149096_c");
				c.setAccessible(true);
				c.setInt(packet, Util.bedLocation());

				Field d = packet.getClass().getDeclaredField("field_149094_d");
				d.setAccessible(true);
				d.setInt(packet, loc.getBlockZ());

			} catch (Exception e) {
				e.printStackTrace();
			}
			return packet;
		}

		public S14PacketEntity.S15PacketEntityRelMove getMovePacket() {
			return new S14PacketEntity.S15PacketEntityRelMove(entityId, (byte) 0, (byte) (-60.8), (byte) 0);
		}

		public S38PacketPlayerListItem getInfoPacket() {
			return new S38PacketPlayerListItem(); //S38PacketPlayerListItem.addPlayer(((CraftPlayer)player).getHandle());
		}

		public S38PacketPlayerListItem getRemoveInfoPacket() {
			return new S38PacketPlayerListItem(); //S38PacketPlayerListItem.removePlayer(((CraftPlayer)player).getHandle());
		}

		public Location getTrueLocation() {
			return loc.clone().add(0, 0.1, 0);
		}

		public S04PacketEntityEquipment getEquipmentPacket(int slot, ItemStack stack){
			if (stack == null){
				return null;
			}
			return new S04PacketEntityEquipment(entityId, slot, stack);
		}

		@SuppressWarnings("deprecation")
		public void resendCorpseToEveryone() {
			S0CPacketSpawnPlayer spawnPacket = getSpawnPacket();
			S0APacketUseBed bedPacket = getBedPacket();
			S14PacketEntity.S15PacketEntityRelMove movePacket = getMovePacket();
			S38PacketPlayerListItem infoPacket = getInfoPacket();
			final S38PacketPlayerListItem removeInfo = getRemoveInfoPacket();
			final List<Player> toSend = loc.getWorld().getPlayers();
			final S04PacketEntityEquipment helmetInfo = getEquipmentPacket(4, convertBukkitToMc(items.getItem(1)));
			final S04PacketEntityEquipment chestplateInfo = getEquipmentPacket(3, convertBukkitToMc(items.getItem(2)));
			final S04PacketEntityEquipment leggingsInfo = getEquipmentPacket(2, convertBukkitToMc(items.getItem(3)));
			final S04PacketEntityEquipment bootsInfo = getEquipmentPacket(1, convertBukkitToMc(items.getItem(4)));
			final S04PacketEntityEquipment mainhandInfo = getEquipmentPacket(0, convertBukkitToMc(items.getItem(slot+45)));
			for (Player p : toSend) {
				NetHandlerPlayServer conn = ((CraftPlayer) p).getHandle().field_71135_a;
				p.sendBlockChange(Util.bedLocation(loc), Material.BED_BLOCK, (byte) rotation);
				//conn.func_147359_a(infoPacket);
				conn.func_147359_a(spawnPacket);
				conn.func_147359_a(bedPacket);
				conn.func_147359_a(movePacket);
				if(ConfigData.shouldRenderArmor()) {
					if(helmetInfo != null){
						conn.func_147359_a(helmetInfo);
					}
					if(chestplateInfo != null){
						conn.func_147359_a(chestplateInfo);
					}
					if(leggingsInfo != null){
						conn.func_147359_a(leggingsInfo);
					}
					if(bootsInfo != null){
						conn.func_147359_a(bootsInfo);
					}
					if(mainhandInfo != null){
						conn.func_147359_a(mainhandInfo);
					}
				}
			}
			/*Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {
				public void run() {
					for (Player p : toSend) {
						((CraftPlayer) p).getHandle().field_71135_a.func_147359_a(removeInfo);
					}
				}
			}, 20L);*/
		}

		@SuppressWarnings("deprecation")
		public void resendCorpseToPlayer(final Player p) {
			S0CPacketSpawnPlayer spawnPacket = getSpawnPacket();
			S0APacketUseBed bedPacket = getBedPacket();
			S14PacketEntity.S15PacketEntityRelMove movePacket = getMovePacket();
			S38PacketPlayerListItem infoPacket = getInfoPacket();
			final S38PacketPlayerListItem removeInfo = getRemoveInfoPacket();
			final S04PacketEntityEquipment helmetInfo = getEquipmentPacket(4, convertBukkitToMc(items.getItem(1)));
			final S04PacketEntityEquipment chestplateInfo = getEquipmentPacket(3, convertBukkitToMc(items.getItem(2)));
			final S04PacketEntityEquipment leggingsInfo = getEquipmentPacket(2, convertBukkitToMc(items.getItem(3)));
			final S04PacketEntityEquipment bootsInfo = getEquipmentPacket(1, convertBukkitToMc(items.getItem(4)));
			final S04PacketEntityEquipment mainhandInfo = getEquipmentPacket(0, convertBukkitToMc(items.getItem(slot+45)));
			NetHandlerPlayServer conn = ((CraftPlayer) p).getHandle().field_71135_a;
			p.sendBlockChange(Util.bedLocation(loc),
					Material.BED_BLOCK, (byte) rotation);
			//conn.func_147359_a(infoPacket);
			conn.func_147359_a(spawnPacket);
			conn.func_147359_a(bedPacket);
			conn.func_147359_a(movePacket);
			if(ConfigData.shouldRenderArmor()) {
				if(helmetInfo != null){
					conn.func_147359_a(helmetInfo);
				}
				if(chestplateInfo != null){
					conn.func_147359_a(chestplateInfo);
				}
				if(leggingsInfo != null){
					conn.func_147359_a(leggingsInfo);
				}
				if(bootsInfo != null){
					conn.func_147359_a(bootsInfo);
				}
				if(mainhandInfo != null){
					conn.func_147359_a(mainhandInfo);
				}
			}
			/*Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {
				public void run() {
					((CraftPlayer) p).getHandle().field_71135_a.func_147359_a(removeInfo);
				}
			}, 20L);*/
		}

		@SuppressWarnings("deprecation")
		public void destroyCorpseFromPlayer(Player p) {
			S13PacketDestroyEntities packet = new S13PacketDestroyEntities(entityId);
			((CraftPlayer) p).getHandle().field_71135_a.func_147359_a(packet);
			Block b = Util.bedLocation(loc).getBlock();
			boolean removeBed = true;
			for (CorpseData cd : getAllCorpses()) {
				if (cd != this
						&& Util.bedLocation(cd.getOrigLocation())
						.getBlock().getLocation()
						.equals(b.getLocation())) {
					removeBed = false;
					break;
				}
			}
			if (removeBed) {
				p.sendBlockChange(b.getLocation(), b.getType(), b.getData());
			}
		}

		public Location getOrigLocation() {
			return loc;
		}

		@SuppressWarnings("deprecation")
		public void destroyCorpseFromEveryone() {
			S13PacketDestroyEntities packet = new S13PacketDestroyEntities(entityId);
			Block b = Util.bedLocation(loc).getBlock();
			boolean removeBed = true;
			for (CorpseData cd : getAllCorpses()) {
				if (cd != this
						&& Util.bedLocation(cd.getOrigLocation())
						.getBlock().getLocation()
						.equals(b.getLocation())) {
					removeBed = false;
					break;
				}
			}
			for (Player p : loc.getWorld().getPlayers()) {
				((CraftPlayer) p).getHandle().field_71135_a.func_147359_a(packet);
				if (removeBed) {
					p.sendBlockChange(b.getLocation(), b.getType(), b.getData());
				}
			}
		}

		public void tickPlayerLater(int ticks, Player p) {
			tickLater.put(p, ticks);
		}

		public int getPlayerTicksLeft(Player p) {
			return tickLater.get(p);
		}

		public void stopTickingPlayer(Player p) {
			tickLater.remove(p);
		}

		public boolean isTickingPlayer(Player p) {
			return tickLater.containsKey(p);
		}

		public Set<Player> getPlayersTicked() {
			return tickLater.keySet();
		}

		public Inventory getItemsInventory() {
			return items;
		}

		public int getEntityId() {
			return entityId;
		}

		public Inventory getLootInventory() {
			return items;
		}

		@Override
		public void setInventoryView(InventoryView iv) {
			this.iv = iv;
		}

		@Override
		public InventoryView getInventoryView() {
			return iv;
		}

		@Override
		public int getSelectedSlot() {
			return slot;
		}

		@Override
		public CorpseData setSelectedSlot(int slot) {
			this.slot = slot;
			return this;
		}


		@Override
		public String getCorpseName() {
			return corpseName;
		}


		@Override
		public String getKillerUsername() {
			return killerName;
		}


		@Override
		public UUID getKillerUUID() {
			return killerUUID;
		}

		@Override
		public String getProfilePropertiesJson() {
			/*PropertyMap pmap = prof.getProperties();
			net.minecraft.util.com.google.gson.JsonElement element = new PropertyMap.Serializer().serialize(pmap, null, null);
			return element.toString();*/
			return null;
		}

	}

	public void tick() {
		List<CorpseData> toRemoveCorpses = new ArrayList<CorpseData>();
		for (CorpseData data : corpses) {
			List<Player> worldPlayers = data.getOrigLocation().getWorld().getPlayers();
			for (Player p : worldPlayers) {
				if (data.isTickingPlayer(p)) {
					int ticks = data.getPlayerTicksLeft(p);
					if (ticks > 0) {
						data.tickPlayerLater(ticks - 1, p);
						continue;
					} else {
						data.stopTickingPlayer(p);
					}
				}
				if (data.mapContainsPlayer(p)) {
					if (isInViewDistance(p, data) && !data.canSee(p)) {
						data.resendCorpseToPlayer(p);
						data.setCanSee(p, true);
					} else if (!isInViewDistance(p, data) && data.canSee(p)) {
						data.destroyCorpseFromPlayer(p);
						data.setCanSee(p, false);
					}
				} else if (isInViewDistance(p, data)) {
					data.resendCorpseToPlayer(p);
					data.setCanSee(p, true);
				} else {
					data.setCanSee(p, false);
				}
			}
			if (data.getTicksLeft() >= 0) {
				if (data.getTicksLeft() == 0) {
					toRemoveCorpses.add(data);
				} else {
					data.setTicksLeft(data.getTicksLeft() - 1);
				}
			}
			List<Player> toRemove = new ArrayList<Player>();
			for (Player pl : data.getPlayersWhoSee()) {
				if (!worldPlayers.contains(pl)) {
					toRemove.add(pl);
				}
			}
			data.removeAllFromMap(toRemove);
			toRemove.clear();
			Set<Player> set = data.getPlayersTicked();
			for (Player pl : set) {
				if (!worldPlayers.contains(pl)) {
					toRemove.add(pl);
				}
			}
			set.removeAll(toRemove);
			toRemove.clear();
		}
		for (CorpseData data : toRemoveCorpses) {
			removeCorpse(data);
		}
	}

	public List<CorpseData> getAllCorpses() {
		return corpses;
	}

	public void registerPacketListener(Player p) {
		PcktIn_v1_7_R4.registerListener(p);
	}

	@Override
	protected void addNbtTagsToSlime(LivingEntity slime) {
		Entity entity = ((CraftEntity)slime).getHandle();
		NBTTagCompound tag = new NBTTagCompound();
		entity.c(tag);
		tag.func_74768_a("Silent", 1);
		tag.func_74768_a("Invulnerable", 1);
		tag.func_74768_a("NoAI", 1);
		tag.func_74768_a("NoGravity", 1);
		entity.f(tag);
	}

}
