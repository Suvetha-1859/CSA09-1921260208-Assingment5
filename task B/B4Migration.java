import java.io.*;
import java.sql.*;
import java.util.*;

public class B4Migration {

    public static void main(String[] args) {

        String fileName = "passes.csv";

        String url = "jdbc:mysql://localhost:3306/transport_db";
        String user = "root";
        String password = "YOUR_PASSWORD";

        String sql = "INSERT INTO bus_pass VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
            Connection con = DriverManager.getConnection(url, user, password);
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            PreparedStatement ps = con.prepareStatement(sql);
            PrintWriter report = new PrintWriter("failed_records.txt")
        ) {

            con.setAutoCommit(false);

            String line;
            int lineNo = 0;
            int count = 0;

            while ((line = br.readLine()) != null) {

                lineNo++;

                try {
                    String[] p = line.split(",", -1);

                    if (p.length != 7) {
                        throw new Exception("Invalid field count");
                    }

                    int route = Integer.parseInt(p[3]);
                    double amount = Double.parseDouble(p[6]);

                    if (amount < 0 || route <= 0) {
                        throw new Exception("Invalid values");
                    }

                    ps.setString(1, p[0]);
                    ps.setString(2, p[1]);
                    ps.setString(3, p[2]);
                    ps.setInt(4, route);
                    ps.setString(5, p[4]);
                    ps.setDate(6, Date.valueOf(p[5]));
                    ps.setDouble(7, amount);

                    ps.addBatch();
                    count++;

                    if (count % 1000 == 0) {
                        ps.executeBatch();
                    }

                } catch (Exception e) {
                    report.println("Line " + lineNo + ": "
                            + line + " - " + e.getMessage());
                }
            }

            ps.executeBatch();
            con.commit();

            System.out.println("Migration Completed");
            System.out.println("Records Loaded: " + count);

            DatabaseMetaData md = con.getMetaData();

            System.out.println("Database: " +
                    md.getDatabaseProductName());

            System.out.println("Driver Version: " +
                    md.getDriverVersion());

            ResultSet tables = md.getTables(
                    null, null, "%", new String[]{"TABLE"});

            System.out.println("Tables:");

            while (tables.next()) {
                System.out.println(tables.getString("TABLE_NAME"));
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
