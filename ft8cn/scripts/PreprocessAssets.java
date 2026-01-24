// Java tool to pre-process JSON assets to SQLite databases
// Run this during build to convert JSON files to SQLite for faster loading

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class PreprocessAssets {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: PreprocessAssets <assets_dir> <output_dir>");
            System.exit(1);
        }
        
        String assetsDir = args[0];
        String outputDir = args[1];
        
        System.out.println("Starting asset preprocessing...");
        System.out.println("Assets dir: " + assetsDir);
        System.out.println("Output dir: " + outputDir);
        
        try {
            // Process ITU zone JSON
            String ituJsonPath = assetsDir + "/ituzone.json";
            if (new File(ituJsonPath).exists()) {
                System.out.println("Processing ituzone.json...");
                String dbPath = outputDir + "/ituzone.db";
                processItuZoneJson(ituJsonPath, dbPath);
                System.out.println("Created: ituzone.db");
            }
            
            // Process CQ zone JSON
            String cqJsonPath = assetsDir + "/cqzone.json";
            if (new File(cqJsonPath).exists()) {
                System.out.println("Processing cqzone.json...");
                String dbPath = outputDir + "/cqzone.db";
                processCqZoneJson(cqJsonPath, dbPath);
                System.out.println("Created: cqzone.db");
            }
            
            // Process DXCC JSON
            String dxccJsonPath = assetsDir + "/dxcc_list.json";
            if (new File(dxccJsonPath).exists()) {
                System.out.println("Processing dxcc_list.json...");
                String dbPath = outputDir + "/dxcc_list.db";
                processDxccJson(dxccJsonPath, dbPath);
                System.out.println("Created: dxcc_list.db");
            }
            
            System.out.println("Asset preprocessing complete!");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void processItuZoneJson(String jsonPath, String dbPath) throws Exception {
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
        JSONObject json = new JSONObject(jsonContent);
        
        // Delete existing database
        new File(dbPath).delete();
        
        // Create SQLite database
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        try {
            stmt.execute("CREATE TABLE ituList (itu TEXT, grid TEXT)");
            
            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO ituList (itu, grid) VALUES (?, ?)");
            
            JSONArray names = json.names();
            for (int i = 0; i < names.length(); i++) {
                String itu = names.getString(i);
                JSONObject ituObj = json.getJSONObject(itu);
                JSONArray mh = ituObj.getJSONArray("mh");
                
                for (int j = 0; j < mh.length(); j++) {
                    insertStmt.setString(1, itu);
                    insertStmt.setString(2, mh.getString(j));
                    insertStmt.executeUpdate();
                }
            }
            
            insertStmt.close();
            conn.commit();
        } finally {
            stmt.close();
            conn.close();
        }
    }
    
    private static void processCqZoneJson(String jsonPath, String dbPath) throws Exception {
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
        JSONObject json = new JSONObject(jsonContent);
        
        // Delete existing database
        new File(dbPath).delete();
        
        // Create SQLite database
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        try {
            stmt.execute("CREATE TABLE cqzoneList (cqzone TEXT, grid TEXT)");
            
            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO cqzoneList (cqzone, grid) VALUES (?, ?)");
            
            JSONArray names = json.names();
            for (int i = 0; i < names.length(); i++) {
                String cqzone = names.getString(i);
                JSONObject cqObj = json.getJSONObject(cqzone);
                JSONArray mh = cqObj.getJSONArray("mh");
                
                for (int j = 0; j < mh.length(); j++) {
                    insertStmt.setString(1, cqzone);
                    insertStmt.setString(2, mh.getString(j));
                    insertStmt.executeUpdate();
                }
            }
            
            insertStmt.close();
            conn.commit();
        } finally {
            stmt.close();
            conn.close();
        }
    }
    
    private static void processDxccJson(String jsonPath, String dbPath) throws Exception {
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
        JSONObject json = new JSONObject(jsonContent);
        
        // Delete existing database
        new File(dbPath).delete();
        
        // Create SQLite database
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        try {
            stmt.execute("CREATE TABLE dxccList (" +
                "id INTEGER, dxcc INTEGER, cc TEXT, ccc TEXT, name TEXT, " +
                "continent TEXT, ituzone TEXT, cqzone TEXT, timezone INTEGER, " +
                "ccode INTEGER, aname TEXT, pp TEXT, lat REAL, lon REAL)");
            
            stmt.execute("CREATE TABLE dxcc_prefix (dxcc INTEGER, prefix TEXT)");
            stmt.execute("CREATE TABLE dxcc_grid (dxcc INTEGER, grid TEXT)");
            
            PreparedStatement insertDxcc = conn.prepareStatement(
                "INSERT INTO dxccList (id, dxcc, cc, ccc, name, continent, ituzone, " +
                "cqzone, timezone, ccode, aname, pp, lat, lon) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            PreparedStatement insertPrefix = conn.prepareStatement(
                "INSERT INTO dxcc_prefix (dxcc, prefix) VALUES (?, ?)");
            
            PreparedStatement insertGrid = conn.prepareStatement(
                "INSERT INTO dxcc_grid (dxcc, grid) VALUES (?, ?)");
            
            JSONArray names = json.names();
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                if (key.equals("-1")) continue;
                
                int id = Integer.parseInt(key);
                JSONObject dxccObj = json.getJSONObject(key);
                
                int dxcc = dxccObj.getInt("dxcc");
                
                // Insert main record
                insertDxcc.setInt(1, id);
                insertDxcc.setInt(2, dxcc);
                insertDxcc.setString(3, dxccObj.optString("cc", ""));
                insertDxcc.setString(4, dxccObj.optString("ccc", ""));
                insertDxcc.setString(5, dxccObj.optString("name", ""));
                insertDxcc.setString(6, dxccObj.optString("continent", ""));
                
                // Process ITU zone
                Object ituZone = dxccObj.get("ituzone");
                String ituZoneStr = "";
                if (ituZone instanceof JSONArray) {
                    JSONArray ituArr = (JSONArray) ituZone;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < ituArr.length(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append(ituArr.getString(j));
                    }
                    ituZoneStr = sb.toString();
                } else {
                    ituZoneStr = ituZone.toString().replaceAll("[\\[\\]\"]", "");
                }
                insertDxcc.setString(7, ituZoneStr);
                
                // Process CQ zone
                Object cqZone = dxccObj.get("cqzone");
                String cqZoneStr = "";
                if (cqZone instanceof JSONArray) {
                    JSONArray cqArr = (JSONArray) cqZone;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < cqArr.length(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append(cqArr.getString(j));
                    }
                    cqZoneStr = sb.toString();
                } else {
                    cqZoneStr = cqZone.toString().replaceAll("[\\[\\]\"]", "");
                }
                insertDxcc.setString(8, cqZoneStr);
                
                insertDxcc.setInt(9, dxccObj.optInt("timezone", 0));
                insertDxcc.setInt(10, dxccObj.optInt("ccode", 0));
                insertDxcc.setString(11, dxccObj.optString("aname", ""));
                insertDxcc.setString(12, dxccObj.optString("pp", ""));
                insertDxcc.setDouble(13, dxccObj.optDouble("lat", 0.0));
                insertDxcc.setDouble(14, dxccObj.optDouble("lon", 0.0));
                insertDxcc.executeUpdate();
                
                // Insert prefixes
                String pp = dxccObj.optString("pp", "");
                if (!pp.isEmpty()) {
                    String[] prefixes = pp.split(",");
                    for (String prefix : prefixes) {
                        insertPrefix.setInt(1, dxcc);
                        insertPrefix.setString(2, prefix.trim());
                        insertPrefix.executeUpdate();
                    }
                }
                
                // Insert grids
                JSONArray mh = dxccObj.optJSONArray("mh");
                if (mh != null) {
                    for (int j = 0; j < mh.length(); j++) {
                        insertGrid.setInt(1, dxcc);
                        insertGrid.setString(2, mh.getString(j));
                        insertGrid.executeUpdate();
                    }
                }
            }
            
            insertDxcc.close();
            insertPrefix.close();
            insertGrid.close();
            conn.commit();
        } finally {
            stmt.close();
            conn.close();
        }
    }
}
