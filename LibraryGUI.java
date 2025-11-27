import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LibraryGUI extends JFrame {

    // ALWAYS LOAD THE MYSQL DRIVER
    static {
        try {
            Class.forName("\"C:\\Users\\RAJ KUMAR\\Downloads\\mysql-connector-j-9.5.0\\mysql-connector-j-9.5.0\\mysql-connector-j-9.5.0.jar\"");   // MySQL 8 driver
        } catch (Exception e) {
            System.out.println("Driver Load Error: " + e.getMessage());
        }
    }

    // CONNECTION METHOD (Fully compatible URL)
    private static Connection getConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            String user = "root";              // change
            String pass = "your_password";     // change
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB Error: " + e.getMessage());
            return null;
        }
    }

    // UI FIELDS
    private JTextField titleField, authorField, qtyField;
    private JTextField studentNameField, studentEmailField;
    private JTextField issueBookIdField, issueStudentIdField, issueIdReturnField;

    private JTable booksTable, issuedTable;
    private DefaultTableModel booksModel, issuedModel;
    private String selectedImagePath = null;
    private JLabel coverPreviewLabel;

    public LibraryGUI() {

        setTitle("Library Management System (Java Swing + MySQL)");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // Books Tab
        JPanel bookPanel = new JPanel(new BorderLayout());
        bookPanel.add(bookForm(), BorderLayout.NORTH);
        bookPanel.add(booksTablePanel(), BorderLayout.CENTER);
        tabs.add("Books", bookPanel);

        // Students & Issue Tab
        JPanel issuePanel = new JPanel(new BorderLayout());
        issuePanel.add(studentForm(), BorderLayout.NORTH);
        issuePanel.add(issueReturnPanel(), BorderLayout.CENTER);
        tabs.add("Issue / Return", issuePanel);

        // Issued List Tab
        JPanel issuedPanel = new JPanel(new BorderLayout());
        issuedPanel.add(issuedTablePanel(), BorderLayout.CENTER);
        tabs.add("Issued Books", issuedPanel);

        add(tabs);

        // Load tables
        loadBooks();
        loadIssued();
        
        setVisible(true);
    }

    // ----------------- BOOK FORM PANEL -----------------
    private JPanel bookForm() {

        JPanel p = new JPanel(new GridLayout(5, 2, 5, 5));
        p.setBorder(BorderFactory.createTitledBorder("Add Book"));

        p.add(new JLabel("Title:"));
        titleField = new JTextField(); p.add(titleField);

        p.add(new JLabel("Author:"));
        authorField = new JTextField(); p.add(authorField);

        p.add(new JLabel("Quantity:"));
        qtyField = new JTextField(); p.add(qtyField);

        p.add(new JLabel("Cover Image:"));
        JButton chooseImageBtn = new JButton("Choose Image");
        chooseImageBtn.addActionListener(e -> chooseImage());
        p.add(chooseImageBtn);

        JButton addBtn = new JButton("Add Book");
        addBtn.addActionListener(e -> addBook());
        p.add(addBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadBooks());
        p.add(refreshBtn);

        return p;
    }

    private JSplitPane booksTablePanel() {
        booksModel = new DefaultTableModel(new Object[]{"ID","Title","Author","Qty"}, 0);
        booksTable = new JTable(booksModel);

        // preview label for cover
        coverPreviewLabel = new JLabel();
        coverPreviewLabel.setPreferredSize(new Dimension(180, 250));
        coverPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coverPreviewLabel.setBorder(BorderFactory.createTitledBorder("Cover"));

        JScrollPane scroll = new JScrollPane(booksTable);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, coverPreviewLabel);
        split.setResizeWeight(0.8);

        // show cover when selecting a row
        booksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = booksTable.getSelectedRow();
                if (row >= 0) {
                    Object idObj = booksModel.getValueAt(row, 0);
                    if (idObj instanceof Integer) {
                        int id = (Integer) idObj;
                        showCoverForId(id);
                    }
                }
            }
        });

        return split;
    }

    // ----------------- STUDENT FORM -----------------
    private JPanel studentForm() {

        JPanel p = new JPanel(new GridLayout(3, 2, 5, 5));
        p.setBorder(BorderFactory.createTitledBorder("Add Student"));

        p.add(new JLabel("Name:"));
        studentNameField = new JTextField(); p.add(studentNameField);

        p.add(new JLabel("Email:"));
        studentEmailField = new JTextField(); p.add(studentEmailField);

        JButton addStd = new JButton("Add Student");
        addStd.addActionListener(e -> addStudent());
        p.add(addStd);

        JButton refresh = new JButton("Refresh Issued");
        refresh.addActionListener(e -> loadIssued());
        p.add(refresh);

        return p;
    }

    // ----------------- ISSUE / RETURN FORM -----------------
    private JPanel issueReturnPanel() {

        JPanel p = new JPanel(new GridLayout(4, 2, 5, 5));
        p.setBorder(BorderFactory.createTitledBorder("Issue / Return Book"));

        p.add(new JLabel("Book ID:"));
        issueBookIdField = new JTextField(); p.add(issueBookIdField);

        p.add(new JLabel("Student ID:"));
        issueStudentIdField = new JTextField(); p.add(issueStudentIdField);

        JButton issueBtn = new JButton("Issue Book");
        issueBtn.addActionListener(e -> issueBook());
        p.add(issueBtn);

        p.add(new JLabel("Issue ID (Return):"));
        issueIdReturnField = new JTextField(); p.add(issueIdReturnField);

        JButton returnBtn = new JButton("Return Book");
        returnBtn.addActionListener(e -> returnBook());
        p.add(returnBtn);

        return p;
    }

    private JScrollPane issuedTablePanel() {
        issuedModel = new DefaultTableModel(new Object[]{"IssueID","BookID","StudentID","IssueDate","ReturnDate"}, 0);
        issuedTable = new JTable(issuedModel);
        return new JScrollPane(issuedTable);
    }

    // ----------------- ACTIONS -----------------
    private void addBook() {
        try (Connection con = getConnection()) {

            String sql = "INSERT INTO books(title,author,quantity) VALUES(?,?,?)";
            PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, titleField.getText());
            pst.setString(2, authorField.getText());
            pst.setInt(3, Integer.parseInt(qtyField.getText()));
            pst.executeUpdate();

            int generatedId = -1;
            try (ResultSet gk = pst.getGeneratedKeys()) {
                if (gk != null && gk.next()) generatedId = gk.getInt(1);
            } catch (SQLException ignored) {}

            // if an image was chosen, copy it to images/book_<id>.<ext>
            if (selectedImagePath != null && generatedId > 0) {
                try {
                    java.nio.file.Path imagesDir = java.nio.file.Paths.get("images");
                    if (!java.nio.file.Files.exists(imagesDir)) java.nio.file.Files.createDirectories(imagesDir);
                    String fn = java.nio.file.Paths.get(selectedImagePath).getFileName().toString();
                    String ext = "";
                    int dot = fn.lastIndexOf('.');
                    if (dot >= 0) ext = fn.substring(dot);
                    java.nio.file.Path dest = imagesDir.resolve("book_" + generatedId + ext);
                    java.nio.file.Files.copy(java.nio.file.Paths.get(selectedImagePath), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    selectedImagePath = null;
                } catch (Exception ex) {
                    // non-fatal
                    System.err.println("Could not copy book image: " + ex.getMessage());
                }
            }

            JOptionPane.showMessageDialog(this, "Book Added!");
            loadBooks();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void chooseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File f = fc.getSelectedFile();
            selectedImagePath = f.getAbsolutePath();
            try {
                if (coverPreviewLabel != null) {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                    Image scaled = img.getScaledInstance(coverPreviewLabel.getWidth(), coverPreviewLabel.getHeight(), Image.SCALE_SMOOTH);
                    coverPreviewLabel.setIcon(new ImageIcon(scaled));
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Cannot load image: " + e.getMessage());
            }
        }
    }

    private void showCoverForId(int id) {
        try {
            String[] exts = {"jpg", "jpeg", "png", "gif"};
            for (String ext : exts) {
                java.nio.file.Path p = java.nio.file.Paths.get("images", "book_" + id + "." + ext);
                if (java.nio.file.Files.exists(p)) {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(p.toFile());
                    Image scaled = img.getScaledInstance(coverPreviewLabel.getWidth(), coverPreviewLabel.getHeight(), Image.SCALE_SMOOTH);
                    coverPreviewLabel.setIcon(new ImageIcon(scaled));
                    return;
                }
            }
        } catch (Exception ignored) {}
        if (coverPreviewLabel != null) coverPreviewLabel.setIcon(null);
    }

    private void addStudent() {
        try (Connection con = getConnection()) {

            String sql = "INSERT INTO students(name,email) VALUES(?,?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, studentNameField.getText());
            pst.setString(2, studentEmailField.getText());
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Student Added!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void issueBook() {
        try (Connection con = getConnection()) {
            String sql = "INSERT INTO issued_books(book_id, student_id, issue_date) VALUES(?,?,CURDATE())";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, Integer.parseInt(issueBookIdField.getText()));
            pst.setInt(2, Integer.parseInt(issueStudentIdField.getText()));
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Book Issued!");
            loadIssued();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void returnBook() {
        try (Connection con = getConnection()) {
            String sql = "UPDATE issued_books SET return_date = CURDATE() WHERE issue_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, Integer.parseInt(issueIdReturnField.getText()));
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Book Returned!");
            loadIssued();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    // ----------------- LOAD TABLES -----------------
    private void loadBooks() {
        booksModel.setRowCount(0);
        try (Connection con = getConnection()) {
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM books");
            while (rs.next()) {
                booksModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("quantity")
                });
            }
        } catch (Exception ignored) {}
    }

    private void loadIssued() {
        issuedModel.setRowCount(0);
        try (Connection con = getConnection()) {
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM issued_books");
            while (rs.next()) {
                issuedModel.addRow(new Object[]{
                        rs.getInt("issue_id"),
                        rs.getInt("book_id"),
                        rs.getInt("student_id"),
                        rs.getString("issue_date"),
                        rs.getString("return_date")
                });
            }
        } catch (Exception ignored) {}
    }

    // ----------------- MAIN -----------------
    public static void main(String[] args) {
        new LibraryGUI();
    }
}
