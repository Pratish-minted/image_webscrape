package image_webscrape;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;


public class WebScraper {

public static WebDriver driver;
public static BrowserMobProxyServer proxy;

	public static void main(String[] args) throws IOException {
		List<String> superURLs = new ArrayList<String>();
		List<String> skus = Files.readAllLines(new File("src/test/resources/test-data/sku-data").toPath(), Charset.defaultCharset());
		String URL = "https://www.minted.com/personalize/";
		Cookie ck = new Cookie("renderEndpoint", "rubric");
		System.setProperty("webdriver.chrome.driver","src/test/resources/drivers/chromedriver");
		   
		proxy = new BrowserMobProxyServer();
		proxy.start();
		 
		Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
		   
		String hostIp = Inet4Address.getLocalHost().getHostAddress();
		seleniumProxy.setHttpProxy(hostIp + ":" + proxy.getPort());
		seleniumProxy.setSslProxy(hostIp + ":" + proxy.getPort());
		DesiredCapabilities seleniumCapabilities = new DesiredCapabilities();
		seleniumCapabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
		ChromeOptions options = new ChromeOptions();
		options.merge(seleniumCapabilities);
		
		for (String sku : skus) {
			driver = new ChromeDriver(options);
			proxy.enableHarCaptureTypes(CaptureType.getAllContentCaptureTypes());
			proxy.newHar("images");
			driver.get(URL.concat(sku));
			driver.manage().addCookie(ck);
			driver.navigate().refresh();
			try {
				Thread.sleep(10000);
				
				List<WebElement> frontTab = driver.findElements(By.id("ui-id-1"));
				List<WebElement> backTab = driver.findElements(By.id("ui-id-2"));
				
				
				if(frontTab.size() != 0) {
					frontTab.get(0).click();
				}
				Thread.sleep(1000);
				if(backTab.size() != 0) {
					backTab.get(0).click();
				}
				Thread.sleep(1000);
				/*
				 * WebElement nextButton = driver.findElement(By.className("nextButton"));
				 * nextButton.click();
				 */
				
				
			} catch (InterruptedException e) {
			
			}
			
			Har har = proxy.getHar();
			List<HarEntry> entries = har.getLog().getEntries();
			for (HarEntry entry : entries) {
				if(entry.getRequest().getUrl().contains("rubric.minted.com")) {
					superURLs.add(entry.getRequest().getUrl());
				}
			}
			
			driver.close();
			driver.quit();
			
		}
		
		LinkedHashSet<String> imageUrlSet = new LinkedHashSet<>(superURLs);
        
        ArrayList<String> deDuplicateImageURLSet = new ArrayList<>(imageUrlSet);
		for(String url : deDuplicateImageURLSet) {
			System.out.println("URL: " +  url);
		}
		Path out = Paths.get("src/test/resources/output.txt");
		Files.write(out,deDuplicateImageURLSet,Charset.defaultCharset());
		
		proxy.stop();
	}
}
