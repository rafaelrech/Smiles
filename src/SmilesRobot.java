import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class SmilesRobot extends Thread {
	/* "LAS VEGAS", "LAX", "SFO", */
	private static final int FIREFOX = 0;
	private static final int CHROME = 1;
	private static String[] ORIGINS = { "ATL" /*
											 * , "MIAMI", "CHICAGO", "DULLES",
											 * "NYC", "MCO", "IAH", "MSY",
											 * "BOS", "FLL", "TPA"
											 */};
	private static String[] DESTS = { "POA"/* , "GRU", "GIG", "BSB", "EZE" */};

	private static final int INIT_DEPARTURE_DATE = 8;
	private static final int FINAL_DEPARTURE_DATE = 10;
	private static final int DEPARTURE_MONTH = 11;

	private static final int INIT_RETURN_DATE = 3;
	private static final int FINAL_RETURN_DATE = 8;
	private static final int RETURN_MONTH = 12;

	private static int threadCount = 0;
	private final static int MAX_THREAD_COUNT = 2;// 4;

	private WebDriver driver;
	private String baseUrl;
	private static SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");

	private String origin;
	private String destination;

	// private int outboundDay;
	// private int returnDate;

	// private boolean acceptNextAlert = true;

	public SmilesRobot() {
		this(CHROME);
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public SmilesRobot(int browser) {
		baseUrl = "https://www.smiles.com.br/";
		switch (browser) {
		case FIREFOX:
			FirefoxProfile fp = new FirefoxProfile();
			driver = new FirefoxDriver(fp);
			break;
		case CHROME:
			System.setProperty("webdriver.chrome.driver",
					"C:\\Users\\Administrator\\Downloads\\chromedriver_win32\\chromedriver.exe");
			ChromeOptions options = new ChromeOptions();
			options.addArguments("window-size=650,250");
			options.addArguments("window-position=500,500");
			options.addArguments("no-experiment");
			driver = new ChromeDriver(options);
			break;
		default:
		}
		driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
	}

	public static void main(String[] args) {
		try {
			threadCount = 0;
			for (int c = 0; c < 3; c++) {
				for (int j = 0; j < ORIGINS.length; j++) {
					for (int i = 0; i < DESTS.length; i++) {
						if (i > 0 && j > 0) {
							continue;
						}
						while (threadCount > MAX_THREAD_COUNT) {
							try {
								Thread.sleep(5 * 1000);
							} catch (Exception e) {
							}
						}
						SmilesRobot testSmiles = new SmilesRobot(CHROME);
						testSmiles.initSmiles();
						testSmiles.setOrigin(ORIGINS[j]);
						testSmiles.setDestination(DESTS[i]);
						testSmiles.start();
						Thread.sleep(15 * 1000);
					}
				}
				while (threadCount > 1) {
					try {
						Thread.sleep(5 * 1000);
					} catch (Exception e) {
					}
				}
				int sleepTime = (int) Math.round(2.5 * 60);
				System.out.println("[" + currTime() + "] Sleeping for " + sleepTime + " minutes");
				Thread.sleep(sleepTime * 60 * 1000);
				System.out.println("[" + currTime() + "] **************\tWaking up!\t**************");
			}
		} catch (Exception e) {
			System.out.println("[" + currTime() + "] ooops");
			e.printStackTrace();
		}
	}

	private void initSmiles() {
		driver.get(baseUrl);
	}

	public void terminate() {
		driver.quit();
	}

	@Override
	public void run() {
		try {
			System.out.println(String.format("[%s] %s-%s has started", currTime(), getOrigin(), getDestination()));
			threadCount++;
			searchFlights(INIT_DEPARTURE_DATE, INIT_RETURN_DATE, true);
		} catch (Exception e) {
			System.out.println(String.format("[%s] Error while running thread for %s-%s", currTime(), getOrigin(),
					getDestination()));
			e.printStackTrace();
		} finally {
			threadCount--;
			System.out.println(String.format("[%s] %s-%s has finished", currTime(), getOrigin(), getDestination()));
			this.terminate();
		}
	}

	public void searchFlights(int outboundDay, int returnDate, boolean write2file) throws Exception {

		int inVoos = -1;
		int outVoos = -1;
		List<String> outboundFlights = new ArrayList<String>();
		List<String> inboundFlights = new ArrayList<String>();

		try {
			driver.findElement(By.className("brand")).click();
			// set origin airport
			driver.findElement(By.id("inputOrigin")).click();
			driver.findElement(By.id("inputOrigin")).clear();
			driver.findElement(By.id("inputOrigin")).sendKeys(getOrigin());
			driver.findElement(By.cssSelector("#ulOriginAirport > li")).click();
			// set destination airport
			driver.findElement(By.id("inputDestination")).click();
			driver.findElement(By.id("inputDestination")).clear();
			driver.findElement(By.id("inputDestination")).sendKeys(getDestination());
			driver.findElement(By.cssSelector("#ulDestinationAirport > li")).click();
			// choose 'ROUNDTRIP'
			driver.findElement(By.linkText("IDA E VOLTA")).click();
			driver.findElement(By.linkText("IDA E VOLTA")).click();
			// driver.findElement(By.linkText("SOMENTE IDA")).click();

			driver.findElement(By.linkText("01")).click();
			driver.findElement(By.linkText("02")).click();
			driver.findElement(By.cssSelector("#dk_container_qtdChild > a.dk_toggle.dk_label")).click();
			driver.findElement(
					By.cssSelector("#dk_container_qtdChild > div.dk_options > ul.dk_options_inner > li.even > a"))
					.click();

			// Open the calendar
			if (driver instanceof JavascriptExecutor) {
				((JavascriptExecutor) driver).executeScript("$('#goingOriginDate').click();");
				((JavascriptExecutor) driver).executeScript("$('#goingOriginDate').click();");
			}

			// Navigate and select departure date
			WebElement fd = driver.findElement(By.id("ui-datepicker-div"));
			while (!fd.isDisplayed()) { // $('#goingOriginDate') //
				if (driver instanceof JavascriptExecutor) {
					((JavascriptExecutor) driver).executeScript("$('#goingOriginDate').click();");
					((JavascriptExecutor) driver).executeScript("$('#goingOriginDate').click();");
				}
				driver.findElement(By.id("goingOriginDate")).click();
				fd = driver.findElement(By.id("ui-datepicker-div"));
				Thread.sleep(100);
			}
			// driver.findElement(By.className("ui-icon-circle-triangle-e")).click();
			// // SET
			driver.findElement(By.className("ui-icon-circle-triangle-e")).click(); // OUT
			driver.findElement(By.className("ui-icon-circle-triangle-e")).click(); // NOV
			WebElement janfd = driver.findElement(By.className("ui-datepicker-group-last"));
			janfd.findElement(By.linkText(String.valueOf(outboundDay))).click();
			Thread.sleep(1);

			// Navigate and select returning date
			if (driver instanceof JavascriptExecutor) {
				((JavascriptExecutor) driver).executeScript("$('#backDate').click();");
				((JavascriptExecutor) driver).executeScript("$('#backDate').click();");
			}
			fd = driver.findElement(By.id("ui-datepicker-div"));
			while (!fd.isDisplayed()) {
				if (driver instanceof JavascriptExecutor) {
					((JavascriptExecutor) driver).executeScript("$('#backDate').click();");
					((JavascriptExecutor) driver).executeScript("$('#backDate').click();");
				}
				driver.findElement(By.id("backDate")).click();
				fd = driver.findElement(By.id("ui-datepicker-div"));
				Thread.sleep(100);
			}
			janfd = driver.findElement(By.className("ui-datepicker-group-last"));
			janfd.findElement(By.linkText(String.valueOf(returnDate))).click();
			Thread.sleep(1);

			// Click on 'SEARCH'
			WebElement findsearch = driver.findElement(By.id("executeFindSearch"));
			while (!findsearch.isEnabled() || !findsearch.isDisplayed()) {
				Thread.sleep(100);
			}
			findsearch.click();
			boolean switchedToIFrame = false;
			try { // 'navigate' to the inner iframe containing the results
				driver.switchTo().frame(
						driver.findElement(By.cssSelector("#layout-column_column-2 > div > div > div > div > iframe")));
				switchedToIFrame = true;
			} catch (NoSuchElementException nsee) {
				driver.findElement(By.id("congenereButton")).click();
				Thread.sleep(200);
			}

			findResults(outboundFlights, inboundFlights, switchedToIFrame);

		} catch (Throwable e) {
			System.out.println(String.format("[%s] Error while processing %s-%s (%s,%s) - %s", currTime(), getOrigin(),
					getDestination(), getDate(DEPARTURE_MONTH, outboundDay), getDate(RETURN_MONTH, returnDate),
					e.getMessage()));
			// e.printStackTrace();
			testNextDay(outboundDay, returnDate, write2file);
			return;
		}

		outVoos = outboundFlights.size();
		inVoos = inboundFlights.size();

		// check the results
		String title = "";
		List<String> htmlOutput = new ArrayList<String>();
		try {
			List<WebElement> spans = driver.findElement(By.className("articleFull")).findElements(By.tagName("div"))
					.get(1).findElements(By.tagName("span"));
			List<WebElement> cd = driver.findElements(By.className("searchFlightCurrent"));

			title = String.format("Roudtrip %s-%s  %s %s", spans.get(0).getText(), spans.get(1).getText(),
					getDateFromPage(cd.get(0)), getDateFromPage(cd.get(1)));
		} catch (Exception e) {
			// e.printStackTrace();
			title = String.format("Roudtrip %s-%s\t%s %s", getOrigin(), getDestination(),
					getDate(DEPARTURE_MONTH, outboundDay), getDate(RETURN_MONTH, returnDate));
		}
		htmlOutput.add("<div class='rountripTitle'>" + escapeHtml4(title) + "</div>");
		if (outVoos == 0 || inVoos == 0) {
			if (outVoos == 0) {
				if (inVoos == 0) {
					// System.out.println(title +
					// " - no outbound or inbound flights found ");
					htmlOutput.add("<div class='noResult'>No flights found</div>");
				} else {
					// System.out.println(title
					// +
					// String.format(" - %d return flights found - but no outbound flights",
					// inVoos));
					htmlOutput.add(String.format("<div class='partialResult'>Only inbound flights found (%d)</div>",
							inVoos));
				}
			} else {
				// System.out.println(title
				// +
				// String.format(" - %d outbound flights found - but no return flights",
				// outVoos));
				htmlOutput.add(String.format("<div class='partialResult'>Only outbound flights found (%d)</div>",
						outVoos));
			}
			if (write2file) {
				writeToFile(htmlOutput);
			}
		} else {
			boolean copaOutbound = false;
			boolean copaInbound = false;
			String msg = title + String.format("\n   %d outbound flights found", outVoos);
			htmlOutput.add(String.format("<div class='flightsTitle'>Outbound flights (%d)</div>", outVoos));
			for (String flight : outboundFlights) {
				msg += "\n      " + flight;
				if (flight.indexOf("DL") == 7) {
					htmlOutput.add(String.format("<div class='flightEntry-DL'>%s</div>", escapeHtml4(flight)));
				} else if (flight.indexOf("CM") == 7) {
					copaOutbound = true;
					htmlOutput.add(String.format("<div class='flightEntry-CM'>%s</div>", escapeHtml4(flight)));
				} else {
					htmlOutput.add(String.format("<div class='flightEntry'>%s</div>", escapeHtml4(flight)));
				}
			}
			msg += String.format("\n   %d inbound flights found", inVoos);
			htmlOutput.add(String.format("<div class='flightsTitle'>Return flights (%d)</div>", inVoos));
			for (String flight : inboundFlights) {
				msg += "\n      " + flight;
				if (flight.indexOf("DL") == 7) {
					htmlOutput.add(String.format("<div class='flightEntry-DL'>%s</div>", escapeHtml4(flight)));
				} else if (flight.indexOf("CM") == 7) {
					copaInbound = true;
					htmlOutput.add(String.format("<div class='flightEntry-CM'>%s</div>", escapeHtml4(flight)));
				} else {
					htmlOutput.add(String.format("<div class='flightEntry'>%s</div>", escapeHtml4(flight)));
				}
			}
			if (copaOutbound && copaInbound) {
				htmlOutput.add(String.format("<div class='special'>%s</div>\n", "*** COPA FLIGHT ***"));
				msg += "\n****************************     COPA FLIGHTS         ****************************";
				title = "[COPA] " + title;
			}
			// System.out.println(msg);
			sendEmail(title, msg);
			if (write2file) {
				writeToFile(htmlOutput);
			}
		}

		// switch back to the main frame
		driver.switchTo().parentFrame();

		testNextDay(outboundDay, returnDate, write2file);
	}

	private void findResults(List<String> outboundFlights, List<String> inboundFlights, boolean switchedToIFrame)
			throws InterruptedException {
		boolean resultsFound = false;
		boolean modal = false;
		int counter = 1;
		while (!resultsFound && !modal && counter <= 5) {
			try {
				if (!switchedToIFrame) {
					driver.switchTo().frame(
							driver.findElement(By
									.cssSelector("#layout-column_column-2 > div > div > div > div > iframe")));

				}
				// check whether modal window is shown with message.
				driver.findElement(By.id("btnCloseModal")).click();
				modal = true;
				continue;
			} catch (ElementNotVisibleException nve) {
			} catch (NoSuchElementException nsee) {
			}
			try {
				buildFlightLists(outboundFlights, inboundFlights);
				resultsFound = true;
				continue;
			} catch (ElementNotVisibleException nve) {
			} catch (NoSuchElementException nsee) {
			}
			counter++;
			Thread.sleep(200);
		}

		if (modal) {
			try {
				buildFlightLists(outboundFlights, inboundFlights);
			} catch (ElementNotVisibleException nve) {
			} catch (NoSuchElementException nsee) {
			}
		}
	}

	private void buildFlightLists(List<String> outboundFlights, List<String> inboundFlights) {
		List<WebElement> liFlights = driver.findElements(By.className("fGothamRoundedMedium20"));
		// System.out.println(liFlights.size());
		for (WebElement liFlight : liFlights) {
			WebElement inputFlight = liFlight.findElement(By.tagName("input"));
			boolean isOutbound = (inputFlight.getAttribute("id").indexOf("Back") < 0);
			String onclick = inputFlight.getAttribute("onclick");
			onclick = onclick.substring(onclick.indexOf("'") + 1);
			String[] tokens = onclick.split("', '");
			String flight = String.format(
					"Flight %s %s\tSai as %s  Chega as %s (Duração %s)\t%s paradas\tClasse %s\t%s milhas", tokens[2],
					tokens[3], tokens[8], tokens[9], tokens[6], tokens[5], tokens[4], tokens[12]);

			if (isOutbound) {
				outboundFlights.add(flight);
			} else {
				inboundFlights.add(flight);
			}
		}
	}

	private void testNextDay(int outboundDay, int returnDate, boolean write2File) throws Exception {
		if (returnDate < FINAL_RETURN_DATE) {
			returnDate++;
			searchFlights(outboundDay, returnDate, write2File);
		} else {
			if (outboundDay < FINAL_DEPARTURE_DATE) {
				outboundDay++;
				searchFlights(outboundDay, INIT_RETURN_DATE, write2File);
			}
		}
	}

	private String getDateFromPage(WebElement dateinfo) {
		return dateinfo.findElement(By.className("dateListDay")).getText() + "/"
				+ dateinfo.findElement(By.className("dateListMonths")).getText() + " ("
				+ dateinfo.findElement(By.className("dateListDayWeek")).getText().substring(0, 3) + ")";
	}

	private static String currTime() {
		return format.format(new Date());
	}

	private String getDate(int month, int date) {
		Calendar c = Calendar.getInstance();
		c.set(2010, month - 1, date);
		SimpleDateFormat f = new SimpleDateFormat("MM/dd E");
		return f.format(c.getTime());
	}

	public void sendEmail(String subject, String messageTxt) {
		if (!subject.isEmpty()) {
			return;
		}
		// Recipient's email ID needs to be mentioned.
		String to1 = "rafael.g.rech@adp.com";// change accordingly
		String to2 = "rafael.rech@gmail.com";// change accordingly

		// Sender's email ID needs to be mentioned
		String from = "rafael.rech@gmail.com";// change accordingly
		// final String username = "rafael.rech";// change accordingly
		// final String password = "rechMAIL";// change accordingly
		final String username = "rafael.rech@comcast.net";// change accordingly
		final String password = "juli@2012";// change accordingly

		// Assuming you are sending email through relay.jangosmtp.net
		// String host = "smtp.gmail.com";
		String host = "smtp.comcast.net";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");

		// Get the Session object.
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			Message message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to1));
			message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to1));
			message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to2));
			// Set Subject: header field
			message.setSubject("Voo encontrado: " + subject);

			// Now set the actual message
			message.setText(messageTxt);

			// Send message
			Transport.send(message);

			System.out.println(String.format("[%] Sent message successfully....", currTime()));

		} catch (MessagingException e) {
			System.err.println("Email not sent " + e.getMessage());
		}
	}

	private void writeToFile(List<String> htmlOutput) {
		try {
			Path path = Paths.get("./results- " + getOrigin().toUpperCase() + "-" + getDestination().toUpperCase()
					+ ".html");

			if (!Files.exists(path)) {
				Files.createFile(path);
				Files.write(path, "<html><head>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(path, "<link rel='stylesheet' href='smiles.css' type='text/css' media='all'>\n".getBytes(),
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(path, "</head>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(path, "<body>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(path, "<!-- RESULTS -->\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(path, "<!-- END OF RESULTS -->\n".getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
				Files.write(path, "</body>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				Files.write(path, "</html>\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}

			List<String> lines = Files.readAllLines(path);
			List<String> newLines = new ArrayList<String>();
			for (String line : lines) {
				if (line.equalsIgnoreCase("<!-- END OF RESULTS -->")) {
					for (String htmlOut : htmlOutput) {
						newLines.add(htmlOut);
					}
				}
				newLines.add(line);
			}
			Files.deleteIfExists(path);
			Files.createFile(path);
			for (String line : newLines) {
				// System.out.println(line);
				// line += "\n";
				Files.write(path, (line + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}

			// Files.write(Paths.get("./duke.txt"), msg.getBytes());
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}
}
