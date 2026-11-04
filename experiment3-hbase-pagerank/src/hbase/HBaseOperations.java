import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseOperations implements AutoCloseable {
    private final Connection connection;
    private final Admin admin;

    public HBaseOperations() throws Exception {
        Configuration config = HBaseConfiguration.create();
        connection = ConnectionFactory.createConnection(config);
        admin = connection.getAdmin();
    }

    private static String familyOf(String field) {
        int colon = field.indexOf(':');
        return colon < 0 ? field : field.substring(0, colon);
    }

    private static String qualifierOf(String field) {
        int colon = field.indexOf(':');
        return colon < 0 ? "value" : field.substring(colon + 1);
    }

    public void listTables() throws Exception {
        for (TableName name : admin.listTableNames()) System.out.println(name.getNameAsString());
    }

    public void createTable(String tableName, String[] fields) throws Exception {
        TableName name = TableName.valueOf(tableName);
        if (admin.tableExists(name)) {
            if (admin.isTableEnabled(name)) admin.disableTable(name);
            admin.deleteTable(name);
        }
        Set<String> families = new LinkedHashSet<>();
        for (String field : fields) families.add(familyOf(field));
        TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(name);
        for (String family : families) {
            builder.setColumnFamily(ColumnFamilyDescriptorBuilder.of(family));
        }
        admin.createTable(builder.build());
    }

    public void addRecord(String tableName, String row, String[] fields, String[] values)
            throws Exception {
        if (fields.length != values.length) throw new IllegalArgumentException("fields/values length mismatch");
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Put put = new Put(Bytes.toBytes(row));
            for (int i = 0; i < fields.length; i++) {
                put.addColumn(Bytes.toBytes(familyOf(fields[i])),
                              Bytes.toBytes(qualifierOf(fields[i])), Bytes.toBytes(values[i]));
            }
            table.put(put);
        }
    }

    public void scanTable(String tableName) throws Exception {
        try (Table table = connection.getTable(TableName.valueOf(tableName));
             ResultScanner scanner = table.getScanner(new Scan())) {
            for (Result result : scanner) System.out.println(result);
        }
    }

    public void scanColumn(String tableName, String column) throws Exception {
        Scan scan = new Scan();
        try (Table table = connection.getTable(TableName.valueOf(tableName));
             ResultScanner scanner = table.getScanner(scan)) {
            for (Result result : scanner) {
                String row = Bytes.toString(result.getRow());
                byte[] family = Bytes.toBytes(familyOf(column));
                if (column.contains(":")) {
                    byte[] value = result.getValue(family, Bytes.toBytes(qualifierOf(column)));
                    System.out.println(row + " => " + (value == null ? "null" : Bytes.toString(value)));
                } else {
                    NavigableMap<byte[], byte[]> values = result.getFamilyMap(family);
                    if (values == null || values.isEmpty()) {
                        System.out.println(row + " => null");
                    } else {
                        StringBuilder text = new StringBuilder();
                        for (Map.Entry<byte[], byte[]> item : values.entrySet()) {
                            if (text.length() > 0) text.append(", ");
                            text.append(Bytes.toString(item.getKey())).append('=')
                                .append(Bytes.toString(item.getValue()));
                        }
                        System.out.println(row + " => " + text);
                    }
                }
            }
        }
    }

    public void modifyData(String tableName, String row, String column) throws Exception {
        System.out.print("请输入新值: ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        modifyData(tableName, row, column, reader.readLine());
    }

    public void modifyData(String tableName, String row, String column, String newValue)
            throws Exception {
        addRecord(tableName, row, new String[]{column}, new String[]{newValue});
    }

    public void deleteRow(String tableName, String row) throws Exception {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.delete(new Delete(Bytes.toBytes(row)));
        }
    }

    public void deleteColumn(String tableName, String row, String column) throws Exception {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Delete delete = new Delete(Bytes.toBytes(row));
            delete.addColumns(Bytes.toBytes(familyOf(column)), Bytes.toBytes(qualifierOf(column)));
            table.delete(delete);
        }
    }

    public void addColumnFamily(String tableName, String family) throws Exception {
        admin.addColumnFamily(TableName.valueOf(tableName), ColumnFamilyDescriptorBuilder.of(family));
    }

    public void deleteColumnFamily(String tableName, String family) throws Exception {
        admin.deleteColumnFamily(TableName.valueOf(tableName), Bytes.toBytes(family));
    }

    public void clearTable(String tableName) throws Exception {
        TableName name = TableName.valueOf(tableName);
        if (admin.isTableEnabled(name)) admin.disableTable(name);
        admin.truncateTable(name, true);
    }

    public long countRows(String tableName) throws Exception {
        long count = 0;
        try (Table table = connection.getTable(TableName.valueOf(tableName));
             ResultScanner scanner = table.getScanner(new Scan())) {
            for (Result ignored : scanner) count++;
        }
        return count;
    }

    private void loadDemoData() throws Exception {
        createTable("Student", new String[]{"Info:S_No", "Info:S_Sex", "Info:S_Age", "Score:Math"});
        addRecord("Student", "Zhangsan",
                new String[]{"Info:S_No", "Info:S_Sex", "Info:S_Age", "Score:Math", "Score:English"},
                new String[]{"2015001", "male", "23", "86", "69"});
        addRecord("Student", "Mary",
                new String[]{"Info:S_No", "Info:S_Sex", "Info:S_Age", "Score:Computer Science", "Score:English"},
                new String[]{"2015002", "female", "22", "77", "99"});
        addRecord("Student", "Lisi",
                new String[]{"Info:S_No", "Info:S_Sex", "Info:S_Age", "Score:Math", "Score:Computer Science"},
                new String[]{"2015003", "male", "24", "98", "95"});

        createTable("Course", new String[]{"Info:C_Name", "Info:C_Credit"});
        addRecord("Course", "123001", new String[]{"Info:C_Name", "Info:C_Credit"}, new String[]{"Math", "2.0"});
        addRecord("Course", "123002", new String[]{"Info:C_Name", "Info:C_Credit"}, new String[]{"Computer Science", "5.0"});
        addRecord("Course", "123003", new String[]{"Info:C_Name", "Info:C_Credit"}, new String[]{"English", "3.0"});
    }

    @Override
    public void close() throws Exception {
        admin.close();
        connection.close();
    }

    public static void main(String[] args) throws Exception {
        try (HBaseOperations operations = new HBaseOperations()) {
            if (args.length == 0 || "demo".equalsIgnoreCase(args[0])) {
                operations.loadDemoData();
                System.out.println("全部表：");
                operations.listTables();
                System.out.println("Student 全表：");
                operations.scanTable("Student");
                System.out.println("Score:Math 列：");
                operations.scanColumn("Student", "Score:Math");
                operations.modifyData("Student", "Zhangsan", "Info:S_Age", "24");
                System.out.println("Student 行数=" + operations.countRows("Student"));
            } else {
                System.err.println("Usage: HBaseOperations demo");
            }
        }
    }
}

