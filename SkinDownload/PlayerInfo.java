import static java.lang.String.*;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import b100.json.JsonParser;
import b100.json.element.JsonObject;

public class PlayerInfo {
	
	public static final int VERSION = 1;
	
	private static final String stringSkinFix = "[Skin Fix] ";
	
	private static final String stringSkin = "SKIN";
	private static final String stringCape = "CAPE";
	
	private static final String stringUrl = "url";
	private static final String stringTextures = "textures";

	private static final String urlUUID = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String urlSkin = "https://sessionserver.mojang.com/session/minecraft/profile/";
	
	private static final String msgPlayerSkin = "Skin URL for Player %s is %s";
	private static final String msgPlayerCape = "Cape URL for Player %s is %s";
	
	private static final String msgPlayerNoSkin = "Player %s doesn't have a skin";
	private static final String msgPlayerNoCape = "Player %s doesn't have a cape";
	
	private String username;
	private String uuid;
	private String skinUrl;
	private String capeUrl;
	
	private PlayerInfo(String username) {
		this.username = username;
		
		try {
			load();
		}catch (Throwable e) {
			e.printStackTrace();
		}
		
		playerInfos.add(this);
	}
	
	private void load() {
		if(this.username == null) throw new NullPointerException("Username is null!");
		this.uuid = getJsonFromURL(urlUUID + username).getString("id").value;
		
		JsonObject object = getJsonFromURL(urlSkin + uuid);
		object = new JsonParser().parse(decode(object.get("properties").getAsArray().query((e) -> {return e.getAsObject().has("name", stringTextures);}).getAsObject().getString("value").value));
		object = object.get(stringTextures).getAsObject();
		
		if(object.has(stringSkin)) {
			this.skinUrl = object.get(stringSkin).getAsObject().getString(stringUrl).value;
			log(format(msgPlayerSkin, username, skinUrl));
		}else {
			log(format(msgPlayerNoSkin, username));
		}
		if(object.has(stringCape)) {
			this.capeUrl = object.get(stringCape).getAsObject().getString(stringUrl).value;
			log(format(msgPlayerCape, username, capeUrl));
		}else {
			log(format(msgPlayerNoCape, username));
		}
	}
	
	//////////////////////////////////////////////////
	
	private static List<PlayerInfo> playerInfos = new ArrayList<>();
	
	public static PlayerInfo get(String username) {
		return new PlayerInfo(username);
	}
	
	public static String getSkinUrl(String username) {
		PlayerInfo info = getPlayerInfo(username);
		if(info != null) {
			return info.skinUrl;
		}else {
			return null;
		}
	}
	
	public static String getCapeUrl(String username) {
		PlayerInfo info = getPlayerInfo(username);
		if(info != null) {
			return info.capeUrl;
		}else {
			return null;
		}
	}
	
	public static void reloadAll() {
		for(PlayerInfo playerInfo : playerInfos) {
			try{
				playerInfo.load();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void reload(String username) {
		get(username).load();
	}
	
	//////////////////////////////////////////////////
	
	private static JsonObject getJsonFromURL(String url) {
		try {
			InputStream stream = new URL(url).openStream();
			if(stream == null) throw new NullPointerException("Stream is null!");
			JsonObject object = new JsonParser().parseStream(stream);
			if(object == null) throw new NullPointerException("JsonObject is null!");
			return object;
		}catch (Exception e) {
			throw new RuntimeException("Could not get JSON from URL: "+url, e);
		}
	}
	
	private static PlayerInfo getPlayerInfo(String username) {
		for(PlayerInfo info : playerInfos) {
			if(info.username.equalsIgnoreCase(username)) {
				return info;
			}
		}
		
		return new PlayerInfo(username);
	}
	
	private static String decode(String str) {
		try {
			return new String(Base64.getDecoder().decode(str));
		}catch (Exception e) {
			throw new RuntimeException("Could not decode: "+str, e);
		}
	}
	
	private static void log(String str) {
		System.out.println(stringSkinFix+str);
	}
	
	public static String getUrl(String url) {
		System.out.println("url: "+url);
		try {
			if(url.startsWith("skin:")) {
				return PlayerInfo.getSkinUrl(url.substring(5));
			}
			if(url.startsWith("cape:")) {
				return PlayerInfo.getCapeUrl(url.substring(5));
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return url;
	}
	
	public static String combine(String s1, String s2) {
		return s1 + s2;
	}
	
	//////////////////////////////////////////////////
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		JFrame frame = new JFrame("Minecraft Skin Downloader");
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter Username: ");
		JTextField textField = new JTextField(24);
		JButton button = new JButton("Download Skin");
		
		panel.add(label);
		panel.add(textField);
		panel.add(button);
		
		frame.add(panel);
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		
		button.addActionListener((actionEvent) -> {
			String name = textField.getText();
			if(name.length() == 0) {
				Toolkit.getDefaultToolkit().beep();
				return;
			}
			try {
				String skinUrl = getSkinUrl(name);
				String capeUrl = getCapeUrl(name);
				if(skinUrl == null && capeUrl == null) {
					JOptionPane.showMessageDialog(frame, "Error!");
					return;
				}
				if(skinUrl != null) {
					saveImageToFile(skinUrl, name+"-Skin.png");
				}
				if(capeUrl != null) {
					saveImageToFile(capeUrl, name+"-Cape.png");
				}
				JOptionPane.showMessageDialog(frame, "Success!");
			}catch (Exception ex) {
				JOptionPane.showMessageDialog(frame, "Error: "+ex.getClass().getName()+": "+ex.getMessage());
			}
		});
	}
	
	public static void saveImageToFile(String url, String path) {
		BufferedImage image;
		try {
			image = ImageIO.read(new URL(url));
		}catch (Exception e) {
			throw new RuntimeException("Downloading Image: "+url, e);
		}
		File file = new File(path).getAbsoluteFile();
		try {
			ImageIO.write(image, "png", file);
		}catch (Exception e) {
			throw new RuntimeException("Saving Image: "+file.getAbsolutePath());
		}
	}
	
	public static void functionCalls() {
		getSkinUrl("Bestsoft100");
		getCapeUrl("Bestsoft100");
		getUrl("https://bla");
		getUrl("skin:bestsoft100");
		combine("s1", "s2");
	}
	
}
