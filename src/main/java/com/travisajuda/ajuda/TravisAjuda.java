package com.travisajuda.ajuda;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.lang.reflect.Field;
import java.util.*;

public class TravisAjuda extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Boolean> aguardandoDuvida = new HashMap<>();
    private final Map<String, UUID> duvidasParaResposta = new LinkedHashMap<>();
    private final Map<UUID, UUID> aguardandoResposta = new HashMap<>();
    private final Map<UUID, String> respostaEmAberto = new HashMap<>();
    private final Set<String> duvidasBloqueadas = new HashSet<>();

    private String permStaff;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        permStaff = getConfig().getString("permissoes.staff", "ajuda.staff");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ajuda").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length == 0) {
            abrirGuiAjuda(p);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("painel")) {
            if (!p.hasPermission(permStaff)) {
                p.sendMessage(color(getConfig().getString("mensagens.sem_permissao")));
                return true;
            }
            abrirPainelStaff(p);
        }

        return true;
    }

    private void abrirGuiAjuda(Player p) {
        Inventory inv = Bukkit.createInventory(null,
                getConfig().getInt("menu.tamanho", 27),
                color(getConfig().getString("menu.titulo")));

        ItemStack itemAjuda = criarItemMenu("menu.itens.ajuda", p);
        inv.setItem(getConfig().getInt("menu.itens.ajuda.slot", 13), itemAjuda);

        p.openInventory(inv);
    }

    private ItemStack criarItemMenu(String path, Player p) {
        if (getConfig().getBoolean(path + ".CustomSkull", false)) {
            String urlHash = getConfig().getString(path + ".URL", "");
            if (urlHash == null || urlHash.isEmpty())
                return criarItemMaterial(path, p);
            return criarSkullPorUrl(urlHash, path, p);
        }
        return criarItemMaterial(path, p);
    }

    private ItemStack criarItemMaterial(String path, Player p) {
        Material mat = Material.getMaterial(getConfig().getString(path + ".material", "PAPER"));
        if (mat == null) mat = Material.PAPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String nome = getConfig().getString(path + ".nome");
        if (nome != null) meta.setDisplayName(color(nome.replace("%jogador%", p.getName())));

        List<String> loreRaw = getConfig().getStringList(path + ".lore");
        if (!loreRaw.isEmpty()) {
            meta.setLore(colorListReplace(loreRaw, "%jogador%", p.getName()));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack criarSkullPorUrl(String urlHash, String path, Player p) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        String url = "http://textures.minecraft.net/texture/" + urlHash;
        String base64Encoded = Base64.getEncoder().encodeToString(
                String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url).getBytes());

        profile.getProperties().put("textures", new Property("textures", base64Encoded));
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String nome = getConfig().getString(path + ".nome");
        if (nome != null) skullMeta.setDisplayName(color(nome.replace("%jogador%", p.getName())));

        List<String> loreRaw = getConfig().getStringList(path + ".lore");
        if (!loreRaw.isEmpty()) {
            skullMeta.setLore(colorListReplace(loreRaw, "%jogador%", p.getName()));
        }

        skull.setItemMeta(skullMeta);
        return skull;
    }

    private void abrirPainelStaff(Player p) {
        Inventory inv = Bukkit.createInventory(null,
                getConfig().getInt("painel.tamanho", 27),
                color(getConfig().getString("painel.titulo")));

        String nomeTemplate = getConfig().getString("painel.item_duvida.nome", "&c%duvida%");
        List<String> loreRaw = getConfig().getStringList("painel.item_duvida.lore");

        int slot = 0;
        int maxSlot = getConfig().getInt("painel.tamanho", 27);
        for (Map.Entry<String, UUID> entry : duvidasParaResposta.entrySet()) {
            if (slot >= maxSlot) break;

            String duvida = entry.getKey();
            UUID uuidJogador = entry.getValue();

            ItemStack item;
            Player jogador = Bukkit.getPlayer(uuidJogador);

            if (jogador != null && jogador.isOnline()) {
                item = criarCabeçaJogador(jogador, nomeTemplate.replace("%duvida%", duvida), loreRaw, duvida);
            } else {
                item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(color(nomeTemplate.replace("%duvida%", duvida)));
                if (!loreRaw.isEmpty()) meta.setLore(colorListReplace(loreRaw, "%duvida%", duvida));
                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        p.openInventory(inv);
    }

    private ItemStack criarCabeçaJogador(Player jogador, String nome, List<String> loreRaw, String duvida) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(jogador.getName());
        meta.setDisplayName(color(nome));
        if (!loreRaw.isEmpty()) meta.setLore(colorListReplace(loreRaw, "%duvida%", duvida));
        skull.setItemMeta(meta);
        return skull;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (inv == null || e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

        String tituloMenu = color(getConfig().getString("menu.titulo"));
        String tituloPainel = color(getConfig().getString("painel.titulo"));

        String invTitle = inv.getTitle();
        if (invTitle.equals(tituloMenu)) {
            e.setCancelled(true);
            int ajudaSlot = getConfig().getInt("menu.itens.ajuda.slot", 13);
            if (e.getSlot() == ajudaSlot) {
                p.closeInventory();
                aguardandoDuvida.put(p.getUniqueId(), true);
                p.sendMessage(color(getConfig().getString("mensagens.digite_duvida")));
            }
        } else if (invTitle.equals(tituloPainel)) {
            e.setCancelled(true);
            String duvida = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

            if (duvidasBloqueadas.contains(duvida)) {
                p.sendMessage(color(getConfig().getString("mensagens.duvida_em_atendimento")));
                p.closeInventory();
                return;
            }

            UUID jogadorDuvida = duvidasParaResposta.get(duvida);
            if (jogadorDuvida == null) {
                p.sendMessage(color(getConfig().getString("mensagens.duvida_nao_encontrada")));
                p.closeInventory();
                return;
            }

            duvidasBloqueadas.add(duvida);
            p.closeInventory();
            p.sendMessage(color(getConfig().getString("mensagens.responder_duvida").replace("%duvida%", duvida)));

            aguardandoResposta.put(p.getUniqueId(), jogadorDuvida);
            respostaEmAberto.put(p.getUniqueId(), duvida);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (aguardandoDuvida.containsKey(uuid)) {
            e.setCancelled(true);
            String duvida = e.getMessage();

            if (duvida.equalsIgnoreCase("cancelar")) {
                aguardandoDuvida.remove(uuid);
                p.sendMessage(color(getConfig().getString("mensagens.ajuda_cancelada")));
                return;
            }

            aguardandoDuvida.remove(uuid);
            duvidasParaResposta.put(duvida, uuid);
            p.sendMessage(color(getConfig().getString("mensagens.duvida_enviada")));

            String staffPerm = permStaff;
            String msgStaff = color(getConfig().getString("mensagens.nova_duvida_staff"));
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(staffPerm)) {
                    staff.sendMessage(msgStaff.replace("%jogador%", p.getName()).replace("%duvida%", duvida));
                    staff.playSound(staff.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                }
            }
            return;
        }

        if (aguardandoResposta.containsKey(uuid)) {
            e.setCancelled(true);
            String resposta = e.getMessage();

            UUID jogadorRespondido = aguardandoResposta.get(uuid);
            String duvidaRespondida = respostaEmAberto.get(uuid);

            Player jogador = Bukkit.getPlayer(jogadorRespondido);
            if (jogador != null && jogador.isOnline()) {
                jogador.sendMessage(color(getConfig().getString("mensagens.resposta_msg").replace("%resposta%", resposta)));
                jogador.playSound(jogador.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }

            duvidasParaResposta.remove(duvidaRespondida);
            duvidasBloqueadas.remove(duvidaRespondida);
            aguardandoResposta.remove(uuid);
            respostaEmAberto.remove(uuid);

            p.sendMessage(color(getConfig().getString("mensagens.resposta_enviada")));
        }
    }

    private List<String> colorListReplace(List<String> list, String placeholder, String replace) {
        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) {
            colored.add(color(line.replace(placeholder, replace)));
        }
        return colored;
    }

    private String color(String msg) {
        return msg == null ? "" : ChatColor.translateAlternateColorCodes('&', msg);
    }
}