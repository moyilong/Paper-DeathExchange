package deathexchange.deathexchange;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public final class Deathexchange extends JavaPlugin implements Listener {

    static boolean isEnabled = false;
    static int exchangeMinutes = 5;
    static long LastChangeTime;
    static List<String> ExchangeExcludeUser = new ArrayList<>();

    static boolean isRandomTime=false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        resetTimer();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    void resetTimer() {
        LastChangeTime = new Date().getTime();
        if (isRandomTime && isEnabled) {
            exchangeMinutes = LimitRandom(6) + 1;
            SendBroadcastMessage("随机时间:" + exchangeMinutes);
        }
    }

    //获取存活的玩家
    List<Player> GetAlivePlayer() {
        var allPlayers = Bukkit.getOnlinePlayers();
        List<Player> exchangeIncludeUser = new ArrayList<>();
        // 检查用户是否参与交换
        for (Player i : Bukkit.getOnlinePlayers()) {
            boolean addon = true;
            for (String b : ExchangeExcludeUser) {
                if (i.getUniqueId().toString().equals(b)) {
                    addon = false;
                    break;
                }
            }
            if (addon) {
                exchangeIncludeUser.add(i);
            }
        }
        return exchangeIncludeUser;
    }

    void SendBroadcastMessage(String message) {
        for (Player i : Bukkit.getOnlinePlayers()) {
            i.sendMessage(message);
        }
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(message);
    }
    int LimitRandom(int Max) {
        Random rand = new Random();
        var ret = rand.nextInt(Max );
        System.out.println("Random:" + Max + " / " + ret);
        return ret;
    }
    void RunExchange() {
        resetTimer();
        List<Player> exchangeIncludeUser = GetAlivePlayer();

        //匹配交换
        Dictionary<Player, Player> randomIDPair = new Hashtable<>();
        Random rand = new Random();
        while (exchangeIncludeUser.size() > 0) {
            if (exchangeIncludeUser.size() == 1) {
                break;
            } else {
                Player p1 = exchangeIncludeUser.get(LimitRandom(exchangeIncludeUser.size()));
                exchangeIncludeUser.remove(p1);

                Player p2 = exchangeIncludeUser.get(LimitRandom(exchangeIncludeUser.size()));
                exchangeIncludeUser.remove(p2);
                randomIDPair.put(p1, p2);
            }
        }

        for (Iterator<Player> it = randomIDPair.keys().asIterator(); it.hasNext(); ) {

            Player a = it.next();
            Player b = randomIDPair.get(a);

                var la = a.getLocation();
                var lb = b.getLocation();

                a.teleport(lb);
                b.teleport(la);


                a.sendMessage("你和" + b.getName() + "发生了交换!");
                b.sendMessage("你和" + a.getName() + "发生了交换!");
        }
        if (exchangeIncludeUser.size() > 0){
            var player = exchangeIncludeUser.get(0);
            player.sendMessage("你不发生交换，惊不惊喜，意不意外？");
        }
        SendBroadcastMessage("本轮交换已经完成!");

    }

    void ExchangeCaller() {
        if (isEnabled) {
            var players = Bukkit.getOnlinePlayers();
            long nowTime = new Date().getTime();
            double elaspedMinute = (nowTime - LastChangeTime) / 1000 / 60; // 剩余时间/分钟
            if (elaspedMinute < 0) {
             RunExchange();
            } else if (elaspedMinute < 10) {
                SendBroadcastMessage("交换剩余时间:" + (int) elaspedMinute);
            }
        }
    }

    @EventHandler
    public void listener(PlayerDeathEvent playerDeathEvent) {
        if (isEnabled) {
            Player user = playerDeathEvent.getEntity();
            String userId = user.getUniqueId().toString();
            boolean alreadExclude = false;
            for (String i : ExchangeExcludeUser) {
                if (i.equals(userId)) {
                    alreadExclude = true;
                    break;
                }
            }
            if (!alreadExclude) {
                ExchangeExcludeUser.add(userId);
                SendBroadcastMessage(user.getName() + " 死亡!");
                user.setGameMode(GameMode.SPECTATOR);
                var alive = GetAlivePlayer();
                if (alive.size() == 1) {
                    SendBroadcastMessage("游戏结束! 获胜者是:" + alive.get(0).getName());
                    ResetPlugins();
                } else {
                    SendBroadcastMessage("游戏继续！还有" + alive.size() + "个玩家");
                }
            }
        }
    }

    void ResetPlugins() {
        SendBroadcastMessage("插件重置");

        isEnabled = false;
        ExchangeExcludeUser.clear();
        for (Player i : Bukkit.getOnlinePlayers()) {
            i.setGameMode(GameMode.SURVIVAL);
        }
        resetTimer();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("OP Permission require!");
            return false;
        }
        String cmd = "x";
        try {
            if (args.length > 0) {
                cmd = args[0];
                switch (cmd) {
                    case "start":
                        resetTimer();
                        isEnabled = true;
                        SendBroadcastMessage("游戏开始! 每 " + exchangeMinutes + " 分钟进行一次随即交换");
                        return true;
                    case "stop":
                        isEnabled = false;
                        SendBroadcastMessage("游戏结束!");
                        ResetPlugins();
                        return true;
                    case "set-time":
                        try {
                            if (args[args.length-1].equals( "???")) {
                                isRandomTime = true;
                                resetTimer();
                            } else {
                                int time = Integer.parseInt(args[args.length-1]);
                                isRandomTime = false;
                                exchangeMinutes = time;
                                sender.sendMessage("交换时间已修改");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return false;
                        }
                        return true;
                    case "reset":
                        ResetPlugins();
                        return true;
                    case "exchange":
                        RunExchange();
                        return true;
                }
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }

        String[] str = {
                "未知命令:" + cmd,
                "Label:" + label,
                "Count:" +  args.length,
                "/de start # 开始游戏",
                "/de stop # 结束游戏",
                "/de reset # 重置插件/用户状态",
                "/de exchange # 立即交换",
                "/de set-time <minute int> # 设置交换时间 使用'???'表示随机时间",
        };

        for (String i : str) {
            sender.sendMessage(i);
        }
        return false;
    }
}