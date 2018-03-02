/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shanooshkarma;

import com.nametagedit.plugin.NametagEdit;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import sun.security.ssl.Debug;

/**
 *
 * @author kightmare
 */
public class ShanooshKarma extends JavaPlugin implements Listener {

    public static Chat chat = null;
    private Connection connection;
    private String host, database, username, password;
    private int port;
    public static Plugin plugin;
    FileConfiguration config = this.getConfig();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        createConfig();
        this.getCommand("reloadKarma").setExecutor(new ReloadConfig(this));
        this.getCommand("KNameReload").setExecutor(new ReloadNames(this));

        host = config.getString("host");
        port = config.getInt("port");
        database = config.getString("database");
        username = config.getString("username");
        password = config.getString("password");
        try {
            openConnection();
            Statement statement = connection.createStatement();
            createTable();
            makeHash();
            setupChat();

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true&useSSL=false", this.username, this.password);
        }

    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }

        return (chat != null);
    }

    public static Chat getChat() {
        return chat;
    }

    private void createTable() throws SQLException {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS Karma"
                + "  (players VARCHAR(255), KarmaPoints DOUBLE, Lawfulness DOUBLE);";

        if (connection == null) {
            try {
                openConnection();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Statement stmt = connection.createStatement();
        stmt.execute(sqlCreate);
    }
    HashMap<Player, Double> karmas = new HashMap<Player, Double>();

    public void makeHash() throws SQLException {
        if (connection == null) {
            try {
                openConnection();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Karma;");
        while (rs.next()) {

            UUID p = UUID.fromString(rs.getString("players"));
            Player playa = Bukkit.getPlayer(p);
            if (!karmas.containsKey(p)) {
                karmas.put(playa, rs.getDouble("KarmaPoints"));
            }

        }
    }
    HashMap<Player, Double> laws = new HashMap<Player, Double>();

    public void makeHashLaw() throws SQLException {
        if (connection == null) {
            try {
                openConnection();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Karma;");
        while (rs.next()) {

            UUID p = UUID.fromString(rs.getString("players"));
            Player playa = Bukkit.getPlayer(p);
            if (!laws.containsKey(p)) {
                laws.put(playa, rs.getDouble("Lawfulness"));
            }

        }
    }

    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("Config.yml not found, creating!");

                saveDefaultConfig();

            } else {
                getLogger().info("Config.yml found, loading!");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) throws SQLException {
        if (connection == null) {
            try {
                openConnection();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM Karma WHERE players = '" + e.getPlayer().getUniqueId().toString() + "';");
        if (!rs.next()) {
            stmt.execute("INSERT INTO Karma (players,KarmaPoints,Lawfulness) VALUES ('" + e.getPlayer().getUniqueId().toString() + "', 0,0);");
        }
        makeHash();
        makeHashLaw();
        getChat().setPlayerSuffix(e.getPlayer(), ChatColor.translateAlternateColorCodes('&', getPlayerName(e.getPlayer())));
        setAboveNames();
        //e.getPlayer().setDisplayName(e.getPlayer().getDisplayName() + getPlayerName(e.getPlayer()));
    }
    ArrayList<Player> ls = new ArrayList<Player>();

    @EventHandler
    public void Attack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return;
        }
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        Player pDamage = (Player) e.getDamager();
        //light up attacker
        if (ls.contains(pDamage)) {
            return;
        }
        if (!ls.contains(pDamage)) {
            ls.add(pDamage);
            //add to list and runnable to delete later
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    ls.remove(pDamage);
                }

            }.runTaskLater(this, 1200);
        }

    }

    @EventHandler
    public void playerDeath(PlayerDeathEvent e) throws SQLException {
        if (e.getEntity().getKiller() == null) {
            return;
        }
        if (e.getEntity() == null) {
            return;
        }
        Player killed = e.getEntity().getPlayer();
        Player killer = e.getEntity().getKiller();

        makeHash();
        makeHashLaw();
        double killedkarma = karmas.get(e.getEntity().getPlayer());
        double killerkarma = karmas.get(e.getEntity().getKiller());
        //do check if killer is attacker and reward killer
        if (ls.contains(killed)) {
            changePlayerKarma(killer.getUniqueId(), 1);
            ls.remove(killed);
        } else {
            changePlayerKarma(killer.getUniqueId(), -1);
        }

    }

    public void changePlayerKarma(UUID u, double d) {
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (connection == null) {
                        try {
                            openConnection();
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT * FROM Karma WHERE players = '" + u.toString() + "'");
                    double get;
                    double total;
                    if (rs.next()) {
                        get = rs.getDouble("KarmaPoints");
                        total = get + d;

                    } else {
                        return;
                    }

                    if (d >= 1) {
                        Player p = Bukkit.getPlayer(u);
                        p.sendMessage("You're Karma went up! It is now " + total);

                    } else {
                        Player p = Bukkit.getPlayer(u);
                        p.sendMessage("You're Karma went down! It is now " + total);
                    }
                    statement.execute("UPDATE Karma SET KarmaPoints =" + total + " WHERE players = '" + u.toString() + "'");
                    Player play = Bukkit.getPlayer(u);
                    //play.setDisplayName(play.getDisplayName() + getPlayerName(play));
                    getChat().setPlayerSuffix(play, ChatColor.translateAlternateColorCodes('&', getPlayerName(play)));
                    setAboveNames();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };

        r.runTaskAsynchronously(this);
    }

    public void changePlayerLaw(UUID u, double d) {
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (connection == null) {
                        try {
                            openConnection();
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT * FROM Karma WHERE players = '" + u.toString() + "'");
                    double get;
                    double total;
                    if (rs.next()) {
                        get = rs.getDouble("Lawfulness");
                        total = get + d;

                    } else {
                        return;
                    }

                    if (d >= 0.1) {
                        Player p = Bukkit.getPlayer(u);
                        //p.sendMessage("You're Lawfulness went up! It is now " + total);

                    } else {
                        Player p = Bukkit.getPlayer(u);
                        //p.sendMessage("You're Lawfulness went down! It is now "+total);
                    }
                    statement.execute("UPDATE Karma SET Lawfulness =" + total + " WHERE players = '" + u.toString() + "'");
                    Player play = Bukkit.getPlayer(u);
                    //play.setDisplayName(play.getDisplayName() + getPlayerName(play));
                    getChat().setPlayerSuffix(play, ChatColor.translateAlternateColorCodes('&', getPlayerName(play)));
                    setAboveNames();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };

        r.runTaskAsynchronously(this);
    }

    public String getPlayerName(Player p) {

        try {
            if (connection == null) {
                try {
                    openConnection();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            makeHash();
            makeHashLaw();
            double get = karmas.get(p);

            // Debug.println("The karma is", "double: " + get);
            if (get == 0) {
                //Debug.println("changed name","neutral");
                return getPlayerLaw(p) + getConfig().getString("NeutralName");

            }
            if (get > getConfig().getDouble("MaxLevel")) {
                //return current name
                get = getConfig().getDouble("MaxLevel");
            }
            if (get < getConfig().getDouble("MinLevel")) {
                //return current name
                get = getConfig().getDouble("MinLevel");
            }

            LinkedHashMap<String, Double> names = new LinkedHashMap<String, Double>();
            if (get <= (double) -1) {

                Debug.println("Playername -1 ", "-1");
                for (String rawData : getConfig().getStringList("BanditRoles")) {
                    String[] raw = rawData.split(":");
                    names.put(String.valueOf(raw[0]), Double.valueOf(raw[1]));
                    //Debug.println("putting: " +String.valueOf(raw[0]) , "Putting: "+Double.valueOf(raw[1]));
                }
                String name = getConfig().getString("NeutralName");
                for (Entry<String, Double> Data : names.entrySet()) {
                    Debug.println("Wjats popping up", Data.getValue() + " " + Data.getKey());
                    if (get < (Double) Data.getValue()) {
                        name = (String) Data.getKey();
                        //Debug.println("changing name",name);
                    }
                    if (get >= (Double) Data.getValue()) {
                        name = (String) Data.getKey();
                        //Debug.println("changed name",name);
                        return getPlayerLaw(p) + name;
                    }
                }

            }

            if (get >= (double) 1) {
                // Debug.println("player name +1", "+1");
                for (String rawData : getConfig().getStringList("HeroRoles")) {
                    String[] raw = rawData.split(":");
                    names.put(String.valueOf(raw[0]), Double.valueOf(raw[1]));
                    // Debug.println("putting: " +String.valueOf(raw[0]) , "Putting: "+Double.valueOf(raw[1]));

                }
            }
            String name = getConfig().getString("NeutralName");
            for (Entry<String, Double> Data : names.entrySet()) {
                // Debug.println("Wjats popping up", Data.getValue() + " " + Data.getKey());
                if (get > (Double) Data.getValue()) {
                    name = (String) Data.getKey();
                    // Debug.println("Changing name",name);
                }
                if (get <= (Double) Data.getValue()) {
                    name = (String) Data.getKey();
                    //Debug.println("changed name",name);
                    return getPlayerLaw(p) + name;

                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return " Somethings messed up";
    }

    public String curname(Player p) {
        return getChat().getPlayerSuffix(p);
    }

    public String getPlayerLaw(Player p) {

        try {
            if (connection == null) {
                try {
                    openConnection();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ShanooshKarma.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            makeHash();
            makeHashLaw();
            double get = laws.get(p);

            // Debug.println("The karma is", "double: " + get);
            if (get < 1 && get > -1) {
                Debug.println("changed name", "neutral");
                return getConfig().getString("NeutralLaw");

            }
            if (get > getConfig().getDouble("MaxLaw")) {
                //return current name
                get = getConfig().getDouble("MaxLaw");
            }
            if (get < getConfig().getDouble("MinLaw")) {
                //return current name
                get = getConfig().getDouble("MinLaw");
            }

            LinkedHashMap<String, Double> names = new LinkedHashMap<String, Double>();
            if (get <= (double) -1) {

                //Debug.println("Playername -1 ", "-1");
                for (String rawData : getConfig().getStringList("Chaotic")) {
                    String[] raw = rawData.split(":");
                    names.put(String.valueOf(raw[0]), Double.valueOf(raw[1]));
                    //Debug.println("putting: " +String.valueOf(raw[0]) , "Putting: "+Double.valueOf(raw[1]));
                }
                String name = getConfig().getString("NeutralLaw");
                get = Math.ceil(get);
                for (Entry<String, Double> Data : names.entrySet()) {
                    // Debug.println("Wjats popping up", Data.getValue() + " " + Data.getKey());
                    if (get < (Double) Data.getValue()) {
                        name = (String) Data.getKey();
                        // Debug.println("changing name",name);
                    }

                    if (get >= (Double) Data.getValue()) {
                        name = (String) Data.getKey();
                        // Debug.println("changed name",name);
                        return name;
                    }
                }

            }

            if (get >= (double) 1) {
                // Debug.println("player name +1", "+1");
                for (String rawData : getConfig().getStringList("LawfulNess")) {
                    String[] raw = rawData.split(":");
                    names.put(String.valueOf(raw[0]), Double.valueOf(raw[1]));
                    // Debug.println("putting: " +String.valueOf(raw[0]) , "Putting: "+Double.valueOf(raw[1]));

                }
            }
            String name = getConfig().getString("NeutralLaw");
            get = Math.floor(get);
            for (Entry<String, Double> Data : names.entrySet()) {
                //  Debug.println("Wjats popping up", Data.getValue() + " " + Data.getKey());
                if (get > (Double) Data.getValue()) {
                    name = (String) Data.getKey();
                    //  Debug.println("Changing name",name);
                }
                if (get <= (Double) Data.getValue()) {
                    name = (String) Data.getKey();
                    // Debug.println("changed name",name);
                    return name;

                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return " Somethings messed up";
    }

    @EventHandler
    public void treeGrowth(StructureGrowEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        Player p = e.getPlayer();
        changePlayerLaw(p.getUniqueId(), getConfig().getDouble("TreeGrow"));
    }

    @EventHandler
    public void killPassiveMob(EntityDeathEvent e) {
        if (e.getEntity() == null) {
            return;
        }
        if (e.getEntity().getType().equals(EntityType.PLAYER)) {
            return;
        }
        if (e.getEntity().getKiller() == null) {
            return;
        }
        if (!e.getEntity().getKiller().getType().equals(EntityType.PLAYER)) {
            return;
        }
        LivingEntity ent = e.getEntity();
        Player p = (Player) e.getEntity().getKiller();
        boolean found = false;
        for (String entity : getConfig().getStringList("BadMobs")) {
            try {
                if (e.getEntity().getType().equals(EntityType.valueOf(entity.toUpperCase()))) {
                    found = true;
                    System.out.print("Entity found" + entity);
                    changePlayerLaw(p.getUniqueId(), getConfig().getDouble("BadMobKills"));
                    return;
                }

            } catch (Exception exc) {
                System.out.print(exc);
            } finally {
                System.out.print("Please refer to Spigot EntityType documentation for correct names. " + entity);
            }
        }

        if (!found) {
            for (String entity : getConfig().getStringList("GoodMobs")) {
                try {
                    if (e.getEntity().getType().equals(EntityType.valueOf(entity.toUpperCase()))) {
                        System.out.print("Entity found" + entity);
                        changePlayerLaw(p.getUniqueId(), getConfig().getDouble("GoodMobKills"));
                        return;
                    }

                } catch (Exception exc) {
                    System.out.print(exc);
                } finally {
                    System.out.print("Please refer to Spigot EntityType documentation for correct names. " + entity);
                }
            }
            return;
        }

    }

    @EventHandler
    public void Arson(BlockIgniteEvent e) {
        if (e.getPlayer() == null) {
            return;
        }
        changePlayerLaw(e.getPlayer().getUniqueId(), getConfig().getDouble("Arson"));
    }
    

    public void setAboveNames() {
      

        for (Player online : Bukkit.getOnlinePlayers()) {
            
            NametagEdit.getApi().setPrefix(online,ChatColor.translateAlternateColorCodes('&',getChat().getPlayerPrefix(online)));
             NametagEdit.getApi().setSuffix(online,ChatColor.translateAlternateColorCodes('&',getChat().getPlayerSuffix(online)));
            

        }

    }
    @EventHandler
    public void planting(BlockPlaceEvent e){
        if(e.getPlayer() == null){
            return;
        }
        if(e.getBlockPlaced() == null){
            return;
        }
        for(String crops : getConfig().getStringList("Plants")){
            if(e.getBlockPlaced().getType().equals(Material.valueOf(crops.toUpperCase()))){
                changePlayerLaw(e.getPlayer().getUniqueId(), getConfig().getDouble("Planting"));
            }
        }
    }
}
