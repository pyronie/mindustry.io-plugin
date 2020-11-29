package mindustry.plugin.utils;

import arc.util.Log;
import mindustry.plugin.BotThread;
import mindustry.plugin.discord.Loader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class Database {
    public static Connection con;

    public static void init(){
        Statement stmt = null;

        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/io","root", Loader.bt.data.getString("db_pass"));

            Log.info("<.io> Connected to postgres server");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
