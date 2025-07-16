import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;

public class AdminGUI extends JFrame {

    Connection conn;
    DefaultTableModel tableModel;
    JTable table;
    JTextField nameField, categoryField, priceField, qtyField;
    JTextField restockIdField, restockQtyField, deleteIdField, searchField;
    JLabel totalStockLabel;

    // Clean color scheme
    private final Color PRIMARY = new Color(52, 152, 219);
    private final Color SUCCESS = new Color(46, 204, 113);
    private final Color WARNING = new Color(241, 196, 15);
    private final Color DANGER = new Color(231, 76, 60);
    private final Color LIGHT_BG = new Color(248, 249, 250);

    public AdminGUI() {
        initDatabase();
        initComponents();
        loadTable();
        setVisible(true);
    }

    private void initDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/akz", "root", "rootp");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Database connection failed!");
            System.exit(1);
        }
    }

    private void initComponents() {
        setTitle("🍭 Sweet Shop Admin");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(LIGHT_BG);

        // Header
        add(createHeader(), BorderLayout.NORTH);
        
        // Table
        add(createTablePanel(), BorderLayout.CENTER);
        
        // Actions
        add(createActionsPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Title
        JLabel title = new JLabel("🍭 Sweet Shop Admin Panel");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(PRIMARY);

        // Search & Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        controls.setOpaque(false);

        searchField = createTextField(20);
        addLiveSearch();
        
        JButton reloadBtn = createButton("🔄 Reload", PRIMARY);
        reloadBtn.addActionListener(e -> loadTable());

        totalStockLabel = new JLabel("Total Stock: ₹0.00");
        totalStockLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalStockLabel.setForeground(PRIMARY);

        controls.add(new JLabel("🔍 Search:"));
        controls.add(searchField);
        controls.add(reloadBtn);
        controls.add(totalStockLabel);

        panel.add(title, BorderLayout.NORTH);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 20, 0, 20));
        panel.setOpaque(false);

        // Table setup
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Price (₹)", "Quantity"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3 || col == 4; // Only price and quantity editable
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 50));

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setOpaque(false);

        panel.add(createCard("➕ Add Sweet", createAddPanel(), SUCCESS));
        panel.add(createCard("🔄 Restock", createRestockPanel(), WARNING));
        panel.add(createCard("🗑️ Delete", createDeletePanel(), DANGER));
        panel.add(createCard("💾 Save", createSavePanel(), PRIMARY));

        return panel;
    }

    private JPanel createCard(String title, JPanel content, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(color, 2));

        // Header
        JLabel headerLabel = new JLabel(title, SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerLabel.setForeground(color);
        headerLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        card.add(headerLabel, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = createTextField(12);
        categoryField = createTextField(12);
        priceField = createTextField(12);
        qtyField = createTextField(12);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        panel.add(categoryField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        panel.add(priceField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        panel.add(qtyField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        
        JButton addBtn = createButton("Add", SUCCESS);
        addBtn.addActionListener(e -> addSweet());
        panel.add(addBtn, gbc);

        return panel;
    }

    private JPanel createRestockPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        restockIdField = createTextField(12);
        restockQtyField = createTextField(12);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Sweet ID:"), gbc);
        gbc.gridx = 1;
        panel.add(restockIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Add Qty:"), gbc);
        gbc.gridx = 1;
        panel.add(restockQtyField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        
        JButton restockBtn = createButton("Restock", WARNING);
        restockBtn.addActionListener(e -> restockSweet());
        panel.add(restockBtn, gbc);

        return panel;
    }

    private JPanel createDeletePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        deleteIdField = createTextField(12);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Sweet ID:"), gbc);
        gbc.gridx = 1;
        panel.add(deleteIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        
        JButton deleteBtn = createButton("Delete", DANGER);
        deleteBtn.addActionListener(e -> deleteSweet());
        panel.add(deleteBtn, gbc);

        return panel;
    }

    private JPanel createSavePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));
        panel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton saveBtn = createButton("Save All Changes", PRIMARY);
        saveBtn.addActionListener(e -> saveChanges());
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(saveBtn, gbc);

        return panel;
    }

    private JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 200, 200)),
            new EmptyBorder(5, 8, 5, 8)
        ));
        return field;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ----------------- Database Actions -----------------
    void addSweet() {
        try {
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "⚠️ Please enter sweet name!");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sweets (name, category, price, quantity) VALUES (?,?,?,?)");
            ps.setString(1, nameField.getText().trim());
            ps.setString(2, categoryField.getText().trim());
            ps.setDouble(3, Double.parseDouble(priceField.getText().trim()));
            ps.setInt(4, Integer.parseInt(qtyField.getText().trim()));
            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "✅ Sweet added successfully!");
            clearFields();
            loadTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        }
    }

    void restockSweet() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE sweets SET quantity = quantity + ? WHERE id = ?");
            ps.setInt(1, Integer.parseInt(restockQtyField.getText().trim()));
            ps.setInt(2, Integer.parseInt(restockIdField.getText().trim()));
            ps.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "✅ Restocked successfully!");
            restockIdField.setText("");
            restockQtyField.setText("");
            loadTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        }
    }

    void deleteSweet() {
        try {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this sweet?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM sweets WHERE id = ?");
                ps.setInt(1, Integer.parseInt(deleteIdField.getText().trim()));
                ps.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "🗑️ Deleted successfully!");
                deleteIdField.setText("");
                loadTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        }
    }

    void saveChanges() {
        try {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int id = Integer.parseInt(tableModel.getValueAt(i, 0).toString());
                double price = Double.parseDouble(tableModel.getValueAt(i, 3).toString());
                int qty = Integer.parseInt(tableModel.getValueAt(i, 4).toString());

                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sweets SET price = ?, quantity = ? WHERE id = ?");
                ps.setDouble(1, price);
                ps.setInt(2, qty);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            
            JOptionPane.showMessageDialog(this, "💾 Changes saved successfully!");
            loadTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        }
    }

    void loadTable() {
        try {
            tableModel.setRowCount(0);
            double total = 0.0;

            String input = searchField.getText().trim();
            String sql = "SELECT * FROM sweets WHERE 1=1";
            PreparedStatement ps;

            if (!input.isEmpty()) {
                if (input.matches("\\d+-\\d+")) {
                    String[] p = input.split("-");
                    sql += " AND price BETWEEN ? AND ?";
                    ps = conn.prepareStatement(sql);
                    ps.setDouble(1, Double.parseDouble(p[0]));
                    ps.setDouble(2, Double.parseDouble(p[1]));
                } else {
                    sql += " AND (name LIKE ? OR category LIKE ?)";
                    ps = conn.prepareStatement(sql);
                    ps.setString(1, "%" + input + "%");
                    ps.setString(2, "%" + input + "%");
                }
            } else {
                ps = conn.prepareStatement(sql);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String cat = rs.getString("category");
                double price = rs.getDouble("price");
                int qty = rs.getInt("quantity");
                total += price * qty;

                tableModel.addRow(new Object[]{id, name, cat, String.format("%.2f", price), qty});
            }

            totalStockLabel.setText("Total Stock: ₹" + String.format("%.2f", total));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error loading data: " + e.getMessage());
        }
    }

    void addLiveSearch() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadTable(); }
            public void removeUpdate(DocumentEvent e) { loadTable(); }
            public void changedUpdate(DocumentEvent e) { loadTable(); }
        });
    }

    void clearFields() {
        nameField.setText("");
        categoryField.setText("");
        priceField.setText("");
        qtyField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminGUI());
    }
}