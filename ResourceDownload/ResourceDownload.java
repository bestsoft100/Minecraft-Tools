import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import b100.utils.FileUtils;
import b100.utils.StreamUtils;
import b100.xml.XmlFile;
import b100.xml.XmlParser;
import b100.xml.element.XmlContentTag;
import b100.xml.element.XmlTag;

public class ResourceDownload {
	
	public static String resourceDomain = "http://resourceproxy.pymcl.net/MinecraftResources/";
	
	private File folder;
	private boolean replaceFiles;
	
	public ResourceDownload(File folder, boolean replaceFiles) {
		this.folder = folder.getAbsoluteFile();
		this.replaceFiles = replaceFiles;
	}
	
	public void downloadResources() {
		XmlFile file = new XmlParser().parseWebsite(resourceDomain);
		XmlContentTag content = file.getRootElement().getAsContentTag();
		
		for(XmlTag<?> t : content.content()) {
			if(t.name().equals("Contents")) {
				XmlContentTag tag = t.getAsContentTag();
				String name = tag.get("Key").getAsStringTag().content();
				
				File soundFile = new File(folder, name);
				
				boolean downloadFile = false;
				
				if(!soundFile.isFile()) downloadFile = true;
				if(soundFile.exists() && replaceFiles) {
					long size = tag.get("Size").getAsStringTag().getLong();
					if(size != soundFile.length()) {
						System.out.println("Sound File "+name+" has been updated! Redownloading...");
						downloadFile = true;
					}
				}
				
				if(downloadFile) {
					try{
						downloadFile(name, soundFile);
					}catch (Exception e) {
						throw new RuntimeException("Error downloading sound file: "+name, e);
					}
				}
			}
		}
	}
	
	private void downloadFile(String name, File file) throws Exception{
		String url = resourceDomain + name;
		url = url.replace(" ", "%20");
		
		System.out.println("Downloading File: "+url);
		
		if(file.exists()) {
			file.delete();
		}
		FileUtils.createNewFile(file);
		
		URL u = new URL(url);
		BufferedInputStream in = new BufferedInputStream(u.openStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		
		StreamUtils.transferData(in, out);
		
		in.close();
		out.close();
	}
	
}
