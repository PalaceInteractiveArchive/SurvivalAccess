package network.palace.survivalaccess;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class SurvivalAccess extends JavaPlugin implements Listener {
    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> playerCollection;

    @Override
    public void onEnable() {
        connectDatabase();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        disconnectDatabase();
    }

    private void connectDatabase() {
        Bukkit.getLogger().warning("Connecting to Palace database...");

        FileConfiguration config = getConfig();
        try {
            ConfigurationSection sec = config.getConfigurationSection("db");
            MongoClientURI connectionString = new MongoClientURI("mongodb://" + sec.getString("username") + ":" +
                    sec.getString("password") + "@" + sec.getString("address") + "/" + sec.getString("database"));
            client = new MongoClient(connectionString);
            this.database = client.getDatabase(config.getString("db.database"));
            playerCollection = database.getCollection("players");
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to connect to Palace database: " + e.getMessage());
            e.printStackTrace();
            if (config != null && !config.contains("db")) {
                ConfigurationSection sec = config.createSection("db");
                sec.set("username", "username");
                sec.set("password", "password");
                sec.set("address", "address");
                sec.set("database", "database");
                config.set("db", sec);
            }
            saveConfig();
            return;
        }

        Bukkit.getLogger().warning("Connected to Palace database!");
    }

    private void disconnectDatabase() {
        Bukkit.getLogger().warning("Disconnecting from Palace database...");
        playerCollection = null;
        database = null;
        if (client != null) client.close();
        client = null;
        Bukkit.getLogger().warning("Disconnected from Palace database!");
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        try {
            Document doc = playerCollection.find(Filters.eq("uuid", uuid.toString())).first();

            Rank rank = Rank.fromString(doc.getString("rank"));

            if (rank == null || (!rank.equals(Rank.SHAREHOLDER) && rank.getRankId() < Rank.MOD.getRankId())) {
                Bukkit.getLogger().info("SurvivalAccess > Blocked login for " + uuid.toString());
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(ChatColor.AQUA + "This server is only available to " + ChatColor.GREEN + "Staff Members " +
                        ChatColor.AQUA + "and " + ChatColor.LIGHT_PURPLE + "Shareholders!\n" + ChatColor.AQUA + "Upgrade to " +
                        ChatColor.LIGHT_PURPLE + "Shareholder " + ChatColor.AQUA + "at " + ChatColor.YELLOW + "https://store.palace.network");
            } else {
                // allowed
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
                Bukkit.getLogger().info("SurvivalAccess > Allowed login of " + rank.getDBName() + " " + uuid.toString());
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("SurvivalAccess > Failed to check login for " + uuid.toString() + ", blocked login");
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(ChatColor.LIGHT_PURPLE + "SurvivalAccess" + ChatColor.AQUA + " > " + ChatColor.RED + "Error verifying access, please try again in a few minutes.");
        }
    }
}
