import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VNLibrary {
    private static DefaultListModel<File> list; //Behind the scenes list for files in Library
    private static JList<File> guiList; //List for displaying files in GUI
    private static JFrame window; //GUI window
    private static FileNameExtensionFilter exeFilter = new FileNameExtensionFilter("Executable Files", "exe");
    private static FileNameExtensionFilter lnkFilter = new FileNameExtensionFilter("Shortcut Files", "lnk");
    private static Color listColor = new java.awt.Color(80,63,51);
    private static Color buttonsColor = new java.awt.Color(111,85,66);
    private static Color borderColor = new java.awt.Color(54,40,29);
    private static final Map<File, Icon> shortcutIcons = new HashMap<>();

    public static void main(String[] args) {
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); //Makes Swing look more modern
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initGUI();
    }

    public static void initGUI(){
        window = new JFrame("VN Library"); //Initializes window
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(600,500);
        window.setIconImage(new ImageIcon("Assets/icon.png").getImage());
        window.setLocationRelativeTo(null);
    
        list = new DefaultListModel<>();
        guiList = new JList<>(list);
        guiList.setCellRenderer(new FileListCellRenderer());
        guiList.setBackground(listColor);
        
        initList();
    
        JButton addFile = new JButton("Add");
        JButton removeFile = new JButton("Remove");
        JButton readFile = new JButton("Read");

        addFile.setBorderPainted(false);
        addFile.setFocusPainted(false);
        removeFile.setBorderPainted(false);
        removeFile.setFocusPainted(false);
        readFile.setBorderPainted(false);
        readFile.setFocusPainted(false);
    
        JScrollPane scrollPane = new JScrollPane(guiList);
        scrollPane.setBorder(null);
    
        addFile.addActionListener(e -> addExecutable());
        removeFile.addActionListener(e -> removeExecutable());
        readFile.addActionListener(e -> readExecutable());
    
        JPanel buttons = new JPanel();
        buttons.add(addFile);
        buttons.add(removeFile);
        buttons.add(readFile);
        buttons.setBackground(buttonsColor);
        buttons.setBorder(BorderFactory.createMatteBorder(0,0,2,0, borderColor));
    
        window.add(buttons, BorderLayout.NORTH);
        window.add(scrollPane, BorderLayout.CENTER);
    
        window.setVisible(true);
    }

    public static void initList(){
        try{
            List<String> files = Files.readAllLines(Paths.get("Library.txt"));
            for(String exe : files){
                File file = new File(exe);
                if(file.exists() && file.isFile()) { list.addElement(file); }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "There was an error loading the library!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void readExecutable(){
        if(guiList.getSelectedIndex() != -1){
            File file = list.get(guiList.getSelectedIndex());
            try{
                ProcessBuilder pb;
                if(file.getName().toLowerCase().endsWith(".lnk")){
                    pb = new ProcessBuilder("cmd.exe", "/c", file.getAbsolutePath());
                } else {
                    pb = new ProcessBuilder(file.getAbsolutePath());
                }
                pb.directory(file.getParentFile());
                pb.start();
                System.exit(0);
            } catch (IOException e) { 
                e.printStackTrace(); 
                JOptionPane.showMessageDialog(null, "Failed to launch executable! Make sure the file is still there!", "Error", JOptionPane.ERROR_MESSAGE); 
            }
        } else {
            JOptionPane.showMessageDialog(null, "Must select a file to read!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void removeExecutable(){
        if(guiList.getSelectedIndex() != -1){
            list.remove(guiList.getSelectedIndex());
            saveFiles();
        } else { JOptionPane.showMessageDialog(null, "Must select a file to remove!", "Error", JOptionPane.ERROR_MESSAGE); }
    }

    private static void addExecutable(){ //Adds executable to library
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(exeFilter);
        fileChooser.addChoosableFileFilter(lnkFilter);
        int chosen = fileChooser.showOpenDialog(null);
        if(chosen == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            if(!list.contains(file)){
                list.addElement(file);
                
                sortList();
                saveFiles();

                guiList.revalidate();
                guiList.repaint();
            } else { JOptionPane.showMessageDialog(null, "This file is already in your library!", "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private static void sortList(){
        Object[] array = list.toArray();
        for(int i = 0; i < array.length; i++){
            array[i] = array[i].toString().substring(array[i].toString().lastIndexOf("\\"), array[i].toString().lastIndexOf("."));
        }
        Arrays.sort(array);
        for(int q = 0; q < array.length; q++){
            for(int v = 0; v < list.size(); v++){
                String listEle = list.elementAt(v).toString();
                if(listEle.substring(listEle.lastIndexOf("\\"), listEle.lastIndexOf(".")).equals(array[q].toString())){
                    array[q] = list.elementAt(v);
                }
            }
        }
        list.clear();
        for(Object element : array){
            list.addElement((File)element);
        }
    }

    private static void saveFiles(){
        try{
            PrintWriter writer = new PrintWriter(new FileWriter("Library.txt"));
            for(int i = 0; i < list.size(); i++){ //Writes newly added file to hard storage library
                writer.println(list.get(i).getAbsolutePath());
            }
            writer.close();
            window.revalidate();
            window.repaint();
        } catch (IOException e){
            JOptionPane.showMessageDialog(null, "There was an error saving to your library", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class FileListCellRenderer extends DefaultListCellRenderer { //Renderer for each entry in library
        private Color textColor = new java.awt.Color(219,219,219);
        private Color selectColor = new java.awt.Color(87,71,69);
        private Font textFont = new Font("Tahoma", 0, 20);
        private AudioInputStream fx;
        private Clip clip;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(value instanceof File){
                File file = (File) value;
                label.setText(file.getName().substring(0, file.getName().lastIndexOf(".")));
                if(!shortcutIcons.containsKey(file)){
                    shortcutIcons.put(file, resizeIcon(FileSystemView.getFileSystemView().getSystemIcon(file), 48, 48));
                }
                label.setIcon(shortcutIcons.get(file));
                label.setForeground(textColor);
                label.setBackground(null);
                label.setBorder(null);
                label.setFont(textFont);

                if(isSelected){
                    label.setBackground(selectColor);
                    label.setBorder(BorderFactory.createEmptyBorder(0,15,0,0));
                    
                    new Thread(() -> {
                        try{
                            if(clip != null && clip.isRunning()){
                                clip.stop();
                                //clip.close();
                            }
                            if(clip != null){
                                clip.setFramePosition(0);
                                clip.start();
                            }
                            else{
                                fx = AudioSystem.getAudioInputStream(VNLibrary.class.getResource("Assets/pull.wav"));
                                clip = AudioSystem.getClip();
                                clip.open(fx);
                                clip.start();
                            }
                        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) { System.out.println("Error: Sound effect broken"); }
                    }).start();
                } else {
                    label.setBackground(null);
                    label.setBorder(null);
                }
            }
            return label;
        }
        public Icon resizeIcon(Icon icon, int width, int height){
            BufferedImage buffered = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D buffImg = buffered.createGraphics();
            icon.paintIcon(null, buffImg, 0, 0);
            buffImg.dispose();

            Image scaled = buffered.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            return new ImageIcon(scaled);
        }
    }
}