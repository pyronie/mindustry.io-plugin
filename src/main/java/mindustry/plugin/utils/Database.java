package mindustry.plugin.utils;

import arc.util.Log;
import mindustry.plugin.BotThread;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.Loader;

import java.sql.*;
import java.util.Properties;

public class Database {
    public static Connection con;

    public static void init(){
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/io","root", Loader.bt.data.getString("db_pass"));

            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public.users;");
            rs.next();

            Log.info("<.io> Connected to postgres server, rows: " + rs.getString("count"));

            rs.close();
            stmt.close();
            
            Log.info("row exists? 'uuid' " + rowExists("uuid") + ", 'test' " + rowExists("test"));

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static boolean rowExists(String uuid){
        try {
            Statement stmt = con.createStatement();
            PreparedStatement query = con.prepareStatement("SELECT EXISTS(SELECT 1 FROM public.users WHERE uuid=?);");

            query.setString(1, uuid);

            ResultSet rs = query.executeQuery();
            rs.next();

            boolean result = rs.getBoolean("exists");

            rs.close();
            stmt.close();

            return result;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static void createRow(PlayerData data){
        try {
            Statement stmt = con.createStatement();
            PreparedStatement query = con.prepareStatement("INSERT INTO public.users(uuid, highest_wave, rank, playtime, buildings, games, verified, banned, banneduntil, banreason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

            query.setString(1, data.uuid);
            query.setInt(2, data.highestWave);
            query.setInt(3, data.rank);
            query.setInt(4, data.playTime);
            query.setInt(5, data.buildingsBuilt);
            query.setInt(6, data.gamesPlayed);
            query.setBoolean(7, data.banned);
            query.setLong(8, data.bannedUntil);
            query.setString(9, data.banReason);

            ResultSet rs = query.executeQuery();
            rs.next();

            Log.info(rs.getObject(1));

            rs.close();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
