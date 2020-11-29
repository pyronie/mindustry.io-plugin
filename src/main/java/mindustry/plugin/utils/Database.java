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

    public static void createRow(String uuid, PlayerData data){
        try {
            Statement stmt = con.createStatement();
            PreparedStatement query = con.prepareStatement("INSERT INTO public.users(uuid, highest_wave, rank, playtime, buildings, games, verified, banned, banneduntil, banreason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

            query.setString(1, uuid);
            query.setInt(2, data.highestWave);
            query.setInt(3, data.rank);
            query.setInt(4, data.playTime);
            query.setInt(5, data.buildingsBuilt);
            query.setInt(6, data.gamesPlayed);
            query.setBoolean(7, data.verified);
            query.setBoolean(8, data.banned);
            query.setLong(9, data.bannedUntil);
            query.setString(10, data.banReason);

            ResultSet rs = query.executeQuery();
            rs.next();

            Log.info(rs.getObject(1));

            rs.close();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static PlayerData getData(String uuid){
        try{
            Statement stmt = con.createStatement();
            PreparedStatement query = con.prepareStatement("SELECT * from public.users WHERE uuid=?;");

            query.setString(1, uuid);

            ResultSet rs = query.executeQuery();

            PlayerData pd = null;
            while(rs.next()) {
                pd = new PlayerData();
                pd.discord_id = rs.getString("discord_id");
                pd.highestWave = rs.getInt("highest_wave");
                pd.rank = rs.getInt("rank");
                pd.playTime = rs.getInt("playtime");
                pd.buildingsBuilt = rs.getInt("buildings");
                pd.gamesPlayed = rs.getInt("games");
                pd.verified = rs.getBoolean("verified");
                pd.banned = rs.getBoolean("banned");
                pd.bannedUntil = rs.getInt("banneduntil");
                pd.banReason = rs.getString("banreason");
            }

            rs.close();
            stmt.close();

            return pd;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static void updateData(String uuid, PlayerData pd){
        try{
            Statement stmt = con.createStatement();
            PreparedStatement query = con.prepareStatement("UPDATE public.users SET discord_id = ?, highest_wave = ?, rank = ?, playtime = ?, buildings = ?, games = ?, verified = ?, banned = ?, banneduntil = ?, banreason = ? WHERE uuid=?;");

            query.setString(1, pd.discord_id);
            query.setInt(2, pd.highestWave);
            query.setInt(3, pd.rank);
            query.setInt(4, pd.playTime);
            query.setInt(5, pd.buildingsBuilt);
            query.setInt(6, pd.gamesPlayed);
            query.setBoolean(7, pd.verified);
            query.setBoolean(8, pd.banned);
            query.setLong(9, pd.bannedUntil);
            query.setString(10, pd.banReason);
            query.setString(11, uuid);

            query.execute();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void updateDataWithBuffer(String uuid, PlayerData buffer){
        try{
            Statement stmt = con.createStatement();
            PreparedStatement query = con.prepareStatement("UPDATE public.users SET buildings = buildings + ?, playtime = playtime + ?, games = games + ? WHERE uuid=?;");

            Log.info("buffer buildings: " + buffer.buildingsBuilt);
            
            query.setInt(1, buffer.buildingsBuilt);
            query.setInt(2, buffer.playTime);
            query.setInt(3, buffer.gamesPlayed);
            query.setString(4, uuid);

            query.execute();
            stmt.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
