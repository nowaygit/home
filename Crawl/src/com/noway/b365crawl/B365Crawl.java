package com.noway.b365crawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.noway.b365crawl.bean.TennisGame;
import com.noway.b365crawl.bean.TennisItem;
import com.noway.b365crawl.bean.TennisOdd;
import com.noway.b365crawl.util.DateUtil;

public class B365Crawl {

	// "https://mobile.28365365.com/sport/splash/Default.aspx?Sport=13&key=13-1-5-0-0-0-0-1-1-0-0-0-0-0-1-0-0-0-0&L=2"
	private static String url = "https://mobile.28365365.com/sport/splash/Default.aspx?Sport=13&key=13-1-5-0-0-0-0-1-1-0-0-0-0-0-1-0-0-0-0&L=2";
	public static ArrayList<TennisGame> tennisGames = new ArrayList<TennisGame>();
	private String ctime;
	
	public static void main(String[] args) {
		B365Crawl bCrawl = new B365Crawl();
		bCrawl.run();
	}

	public void run() {
		System.out.println("******   start fetching......" + DateUtil.Now());
		fetchTennisGames(url);
		
		for (TennisGame tennisGame : tennisGames)
		{
			System.out.println("get data:" + tennisGame.toString());
		}
		
		System.out.println("******   end fetching......"  + DateUtil.Now());
	}

	/**
	 * @param url
	 * 逐层解析抓取
	 */
	public void fetchTennisGames(String url)
	{
		tennisGames = new ArrayList<TennisGame>();
		String html = getHtmlByUrl(url);
		Document doc = Jsoup.parse(html);
		
		Elements links = doc.select("a[href]");
		print("\nLinks: (%d)", links.size());
		for (Element link : links) {
			TennisGame tennisGame = new TennisGame();
			// 每次遍历链接前统一时间记录
			ctime = DateUtil.Now();
			
			String attr = link.attr("href");
			if (attr.contains("..")) {
				String href = "https://mobile.28365365.com/sport/coupon/?clvl=2&" + attr.split("&")[1];
print(" * a: <%s>  (%s)", href, trim(link.text(), 35));
				
				tennisGame.setCtime(ctime);
				tennisGame.setUrl(href);
				tennisGame.setGameName(link.text());
				tennisGame.setTennisItems(fetchTennisItem(href));

				tennisGames.add(tennisGame);
//break;
			}
		}
	}
	
	public ArrayList<TennisItem> fetchTennisItem(String url)
	{
		ArrayList<TennisItem> tennisItems = new ArrayList<TennisItem>();
		Document doc = Jsoup.parse(getHtmlByUrl(url));
		Elements sections = doc.getElementsByClass("Section");
		// Elements sections = doc.select(".Section");
		print("\nsections: (%d)", sections.size());
		for (Element section : sections) {
			TennisItem tennisItem = new TennisItem();
			System.out.println("Section:"
					+ section.getElementsByClass("Header").text());
			
			tennisItem.setItem(section.getElementsByClass("Header").text());
			tennisItem.setCtime(ctime);
			tennisItem.setTennisOdds(fetchTennisOdd(getLinks(section)));
			
//System.out.println(tennisItem.getItem() + section.toString());
			tennisItems.add(tennisItem);
		}
		
		return tennisItems;
	}
	
	public ArrayList<TennisOdd> fetchTennisOdd(Elements elements){
		ArrayList<TennisOdd> tennisOdds = new ArrayList<TennisOdd>();
		for (Element element : elements) {
			TennisOdd tennisOdd = new TennisOdd();
			String[] strings = element.text().split(" ");
			if (strings.length > 1)
			{
				tennisOdd.setOdd(strings[0]);
				tennisOdd.setOption(strings[1]);
				tennisOdd.setCtime(ctime);
				tennisOdd.setUrl(element.attr("href"));
				
				for (int i = 0; i < strings.length; i++) {
					if (strings[i] != null) {
						System.out.println("data:" + strings[i]);
					}
				}
				
				tennisOdds.add(tennisOdd);
			}
		}
		
		return tennisOdds;
	}
	
	/**
	 * 根据URL获得所有的html信息
	 * 
	 * @param url
	 * @return
	 */
	public static String getHtmlByUrl(String url) {
		String html = null;
		HttpClient httpClient = new HttpClient();
		try {
			// 设置http头
			List<Header> headers = new ArrayList<Header>();
			headers.add(new Header(
					"User-Agent",
					"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.57 Safari/537.36"));
			headers.add(new Header("Connection", "keep-alive"));
			headers.add(new Header("Accept-Language", "zh-cn;zh;q=0.8"));
			headers.add(new Header(
					"Cookie",
					"aps03=lng=10&tzi=27&ct=42&cst=105&cg=0&oty=2; session=processform=0&stk=6B87CE10B8784C1BAB8297B187A80038000004; stk=6B87CE10B8784C1BAB8297B187A80038000004; pstk=6B87CE10B8784C1BAB8297B187A80038000004; lng=10"));
			httpClient.getHostConfiguration().getParams()
					.setParameter("http.default-headers", headers);

			GetMethod method = new GetMethod(url);
			// GetMethod("https://mobile.28365365.com/sport/coupon/?ptid=0&key=13-1-5-25291505-5-0-0-1-1-0-0-0-0-0-1-0-0-0-0");
			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
					new DefaultHttpMethodRetryHandler(3, false));

			int statusCode = httpClient.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				System.out.println("Method failed code=" + statusCode + ": "
						+ method.getStatusLine());

			} else {
				// System.out.println(method.getResponseBodyAsString());
				html = method.getResponseBodyAsString();
				Thread.sleep((long)(Math.random() + 1.6) * 1000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		return html;
	}

	private static Elements getLinks(Document doc) {
		Elements links = doc.select("a[href]");
		print("Links: (%d)", links.size());
		for (Element link : links) {
			print(" * a: <%s>  (%s)", link.attr("href"), trim(link.text(), 35));
		}

		return links;
	}

	private static Elements getLinks(Element element) {
		Elements links = element.select("a[href]");
		print("Links: (%d)", links.size());
		for (Element link : links) {
			print(" * a: <%s>  (%s)", link.attr("href"), trim(link.text(), 35));
		}

		return links;
	}

	private static void print(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}
}
