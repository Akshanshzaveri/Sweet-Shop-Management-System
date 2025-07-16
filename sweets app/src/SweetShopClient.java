import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SweetShopClient extends JFrame {
    Connection conn;
    DefaultTableModel sweetsModel, cartModel;
    JTable sweetsTable, cartTable;
    JTextField searchField;
    JLabel totalLabel, cartCountLabel;
    JButton clearCartBtn, checkoutBtn;

    List<CartItem> cartItems = new ArrayList<>();
    double cartTotal = 0.0;
    boolean isDarkMode = false;

    // Enhanced color scheme
    private final Color LIGHT_BG = new Color(255, 255, 255);
    private final Color LIGHT_CARD = new Color(250, 250, 250);
    private final Color LIGHT_ACCENT = new Color(0, 123, 255);
    private final Color LIGHT_SUCCESS = new Color(40, 167, 69);
    private final Color LIGHT_DANGER = new Color(220, 53, 69);
    
    private final Color DARK_BG = new Color(18, 18, 18);
    private final Color DARK_CARD = new Color(28, 28, 28);
    private final Color DARK_ACCENT = new Color(0, 123, 255);
    private final Color DARK_SUCCESS = new Color(40, 167, 69);
    private final Color DARK_DANGER = new Color(220, 53, 69);

    public SweetShopClient() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}

        // DB connection
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/akz", "root", "rootp");
        } catch (Exception e) {
            showStyledMessage("❌ Database Connection Failed", "Error connecting to database!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        initializeUI();
        loadSweetsTable();
        applyTheme();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("🍭 Sweet Shop - Premium Client");
        setSize(1400, 800);
        setLayout(new BorderLayout(15, 15));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add padding to main container
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        // Create top panel with enhanced search
        createTopPanel(mainPanel);
        
        // Create center panel with sweets table
        createCenterPanel(mainPanel);
        
        // Create right panel with cart
        createRightPanel(mainPanel);
    }

    private void createTopPanel(JPanel mainPanel) {
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        searchField = new JTextField(25);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setToolTipText("Search by name, category, or price range (e.g., 100-200)");
        
        JLabel hintLabel = new JLabel("💡 Try: 'chocolate', 'candy', or '100-200'");
        hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hintLabel.setForeground(Color.GRAY);

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(hintLabel);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        
        JButton reloadBtn = createStyledButton("🔄 Reload", LIGHT_ACCENT);
        reloadBtn.addActionListener(e -> loadSweetsTable());
        
        JButton modeBtn = createStyledButton("🌙 Dark Mode", Color.GRAY);
        modeBtn.addActionListener(e -> toggleTheme(modeBtn));
        
        actionPanel.add(reloadBtn);
        actionPanel.add(modeBtn);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        addLiveSearch(searchField);
    }

    private void createCenterPanel(JPanel mainPanel) {
        sweetsModel = new DefaultTableModel(new String[]{"🍭 Name", "📂 Category", "💰 Price (₹)", "📦 Stock", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };

        sweetsTable = new JTable(sweetsModel);
        sweetsTable.setRowHeight(50);
        sweetsTable.setFont(new Font("SansSerif", Font.PLAIN, 16));
        sweetsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));
        sweetsTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        sweetsTable.setSelectionBackground(new Color(0, 123, 255, 30));
        sweetsTable.setGridColor(new Color(200, 200, 200));
        sweetsTable.setShowGrid(true);
        
        // Set column widths
        sweetsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        sweetsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        sweetsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        sweetsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        sweetsTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        sweetsTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        sweetsTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane sweetsScroll = new JScrollPane(sweetsTable);
        sweetsScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createRaisedBevelBorder(), 
            "🍬 Available Sweets",
            0, 0, new Font("SansSerif", Font.BOLD, 18)
        ));
        sweetsScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        mainPanel.add(sweetsScroll, BorderLayout.CENTER);
    }

    private void createRightPanel(JPanel mainPanel) {
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setPreferredSize(new Dimension(350, 0));

        // Cart header with count
        JPanel cartHeader = new JPanel(new BorderLayout());
        cartCountLabel = new JLabel("🛒 Your Cart (0 items)");
        cartCountLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        cartHeader.add(cartCountLabel, BorderLayout.WEST);

        clearCartBtn = createStyledButton("🗑️ Clear", LIGHT_DANGER);
        clearCartBtn.setPreferredSize(new Dimension(80, 30));
        clearCartBtn.addActionListener(e -> clearCart());
        cartHeader.add(clearCartBtn, BorderLayout.EAST);

        // Cart table
        cartModel = new DefaultTableModel(new String[]{"Name", "Category", "Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(35);
        cartTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cartTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        cartTable.setSelectionBackground(new Color(255, 240, 245));
        
        // Add right-click context menu for cart
        addCartContextMenu();

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setPreferredSize(new Dimension(0, 300));

        // Total and checkout section
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        totalLabel = new JLabel("💰 Total: ₹0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        totalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(15, 10, 15, 10)
        ));

        checkoutBtn = createStyledButton("💳 Checkout", LIGHT_SUCCESS);
        checkoutBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        checkoutBtn.setPreferredSize(new Dimension(0, 50));
        checkoutBtn.addActionListener(e -> checkout());

        bottomPanel.add(totalLabel, BorderLayout.NORTH);
        bottomPanel.add(checkoutBtn, BorderLayout.SOUTH);

        rightPanel.add(cartHeader, BorderLayout.NORTH);
        rightPanel.add(cartScroll, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(rightPanel, BorderLayout.EAST);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }

    private void addCartContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("🗑️ Remove Item");
        removeItem.addActionListener(e -> removeFromCart());
        contextMenu.add(removeItem);
        
        cartTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = cartTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        cartTable.setRowSelectionInterval(row, row);
                        contextMenu.show(cartTable, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void removeFromCart() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow >= 0) {
            CartItem item = cartItems.get(selectedRow);
            cartItems.remove(selectedRow);
            cartModel.removeRow(selectedRow);
            cartTotal -= item.price;
            updateCartDisplay();
            showStyledMessage("✅ Item Removed", "Item removed from cart!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearCart() {
        if (cartItems.isEmpty()) {
            showStyledMessage("ℹ️ Cart Empty", "Your cart is already empty!", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear the entire cart?",
            "Clear Cart",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            cartItems.clear();
            cartModel.setRowCount(0);
            cartTotal = 0.0;
            updateCartDisplay();
            showStyledMessage("✅ Cart Cleared", "Cart has been cleared!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void checkout() {
        if (cartItems.isEmpty()) {
            showStyledMessage("ℹ️ Empty Cart", "Please add items to cart before checkout!", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String message = String.format(
            "💳 Checkout Summary\n\n" +
            "Items: %d\n" +
            "Total Amount: ₹%.2f\n\n" +
            "Proceed with payment?",
            cartItems.size(), cartTotal
        );
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            message,
            "Checkout Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Process checkout
            clearCart();
            showStyledMessage("🎉 Success!", "Thank you for your purchase!\nPayment processed successfully!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateCartDisplay() {
        totalLabel.setText(String.format("💰 Total: ₹%.2f", cartTotal));
        cartCountLabel.setText(String.format("🛒 Your Cart (%d items)", cartItems.size()));
        checkoutBtn.setEnabled(!cartItems.isEmpty());
        clearCartBtn.setEnabled(!cartItems.isEmpty());
    }

    void addLiveSearch(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadSweetsTable(); }
            public void removeUpdate(DocumentEvent e) { loadSweetsTable(); }
            public void changedUpdate(DocumentEvent e) { loadSweetsTable(); }
        });
    }

    void loadSweetsTable() {
        try {
            sweetsModel.setRowCount(0);
            String input = searchField.getText().trim();
            String sql = "SELECT * FROM sweets WHERE quantity > 0";

            if (!input.isEmpty()) {
                if (input.matches("\\d+-\\d+")) {
                    String[] range = input.split("-");
                    sql += " AND price BETWEEN ? AND ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setDouble(1, Double.parseDouble(range[0]));
                    ps.setDouble(2, Double.parseDouble(range[1]));
                    fillSweets(ps);
                    return;
                } else {
                    sql += " AND (name LIKE ? OR category LIKE ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, "%" + input + "%");
                    ps.setString(2, "%" + input + "%");
                    fillSweets(ps);
                    return;
                }
            }

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                sweetsModel.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("category"),
                        String.format("%.2f", rs.getDouble("price")),
                        rs.getInt("quantity"),
                        "BUY"
                });
            }
        } catch (Exception e) { 
            e.printStackTrace();
            showStyledMessage("❌ Error", "Failed to load sweets data!", JOptionPane.ERROR_MESSAGE);
        }
    }

    void fillSweets(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            sweetsModel.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getString("category"),
                    String.format("%.2f", rs.getDouble("price")),
                    rs.getInt("quantity"),
                    "BUY"
            });
        }
    }

    static class CartItem {
        String name, category;
        double price;
        public CartItem(String name, String category, double price) {
            this.name = name; this.category = category; this.price = price;
        }
    }

    void buySweet(int row) {
        try {
            String name = sweetsModel.getValueAt(row, 0).toString();
            String category = sweetsModel.getValueAt(row, 1).toString();
            String priceStr = sweetsModel.getValueAt(row, 2).toString();
            double price = Double.parseDouble(priceStr);
            int qty = Integer.parseInt(sweetsModel.getValueAt(row, 3).toString());

            if (qty <= 0) {
                showStyledMessage("❌ Out of Stock", "This item is currently out of stock!", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // FIXED: Show confirmation dialog BEFORE database update
            String confirmMessage = String.format(
                "🍭 Purchase Confirmation\n\n" +
                "Item: %s\n" +
                "Category: %s\n" +
                "Price: ₹%.2f\n\n" +
                "Add to cart?",
                name, category, price
            );
            
            int confirm = JOptionPane.showConfirmDialog(
                this,
                confirmMessage,
                "Confirm Purchase",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            // Only proceed if user confirms
            if (confirm == JOptionPane.YES_OPTION) {
                // Update database only after confirmation
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sweets SET quantity = quantity - 1 WHERE name = ? AND quantity > 0"
                );
                ps.setString(1, name);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    // Add to cart
                    cartItems.add(new CartItem(name, category, price));
                    cartModel.addRow(new Object[]{name, category, String.format("₹%.2f", price)});
                    cartTotal += price;
                    updateCartDisplay();
                    
                    showStyledMessage("✅ Added to Cart", 
                        String.format("%s has been added to your cart!", name), 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showStyledMessage("❌ Purchase Failed", "Item is no longer available!", JOptionPane.ERROR_MESSAGE);
                }
                
                loadSweetsTable(); // Refresh the table
            }
            // If user cancels, nothing happens - no database update, no cart addition
            
        } catch (Exception ex) { 
            ex.printStackTrace();
            showStyledMessage("❌ Error", "Failed to process purchase!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStyledMessage(String title, String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFocusPainted(false);
            setFont(new Font("SansSerif", Font.BOLD, 12));
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            try {
                int qty = Integer.parseInt(sweetsModel.getValueAt(row, 3).toString());
                setText("BUY");
                
                if (qty <= 0) {
                    setEnabled(false);
                    setBackground(new Color(169, 169, 169));
                    setForeground(Color.WHITE);
                    setText("SOLD OUT");
                } else {
                    setEnabled(true);
                    setBackground(isDarkMode ? DARK_SUCCESS : LIGHT_SUCCESS);
                    setForeground(Color.WHITE);
                }
            } catch (Exception e) {
                setEnabled(false);
                setBackground(Color.GRAY);
                setForeground(Color.WHITE);
                setText("ERROR");
            }
            
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("BUY");
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setBackground(isDarkMode ? DARK_SUCCESS : LIGHT_SUCCESS);
            button.setForeground(Color.WHITE);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                buySweet(row);
            }
            isPushed = false;
            return "BUY";
        }
    }

    void toggleTheme(JButton toggleBtn) {
        isDarkMode = !isDarkMode;
        toggleBtn.setText(isDarkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
        applyTheme();
    }

    void applyTheme() {
        Color bg = isDarkMode ? DARK_BG : LIGHT_BG;
        Color cardBg = isDarkMode ? DARK_CARD : LIGHT_CARD;
        Color fg = isDarkMode ? Color.WHITE : Color.BLACK;
        Color headerBg = isDarkMode ? new Color(73, 80, 87) : new Color(233, 236, 239);

        // Main background
        getContentPane().setBackground(bg);
        
        // Search field
        searchField.setBackground(cardBg);
        searchField.setForeground(fg);

        // Tables
        sweetsTable.setBackground(cardBg);
        sweetsTable.setForeground(fg);
        sweetsTable.getTableHeader().setBackground(headerBg);
        sweetsTable.getTableHeader().setForeground(fg);

        cartTable.setBackground(cardBg);
        cartTable.setForeground(fg);
        cartTable.getTableHeader().setBackground(headerBg);
        cartTable.getTableHeader().setForeground(fg);

        // Labels
        totalLabel.setForeground(fg);
        cartCountLabel.setForeground(fg);
        
        // Update button colors
        checkoutBtn.setBackground(isDarkMode ? DARK_SUCCESS : LIGHT_SUCCESS);
        clearCartBtn.setBackground(isDarkMode ? DARK_DANGER : LIGHT_DANGER);
        
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use Nimbus Look and Feel for consistent appearance
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                // Use default if Nimbus fails
                System.err.println("Could not set Nimbus look and feel, using default");
            }
            new SweetShopClient();
        });
    }
}