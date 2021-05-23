package org.pokebot.safety;

import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collector;

public final class Providers
{
	private static final Random random = new Random();
	private static final NumberFormat format = NumberFormat.getInstance(Locale.US);
	private static final int PROCESS_LINE_LENGTH = 76;
	private static final int PROCESS_SIZE_LENGTH = 13;

	static
	{
		format.setGroupingUsed(true);
	}

	public static String macAddressProvider()
	{
		return generateMacAddress(false, false, null, "-", "%02X%s");
	}

	@SneakyThrows
	public static List<String> programFilesProvider()
	{
		val resource = Providers.class.getResourceAsStream("/programfiles.list");
		val list = loadFromResource(resource);
		list.add("Total size : " + list.size());
		return list;
	}

	@SneakyThrows
	public static List<String> processesProvider()
	{
		val resource = Providers.class.getResourceAsStream("/processes.list");
		val list = loadFromResource(resource);
		val it = list.listIterator();
		var index = 0;
		while (it.hasNext())
		{
			val item = it.next();

			if (item.length() != PROCESS_LINE_LENGTH || index < 3)
			{
				index++;
				continue;
			}

			val formattedString = item.substring(PROCESS_LINE_LENGTH - PROCESS_SIZE_LENGTH);
			val stringSize = formattedString.replaceAll("[^0-9]", "");
			var size = 0;
			try
			{
				size = Integer.parseInt(stringSize);
			}
			catch (NumberFormatException e)
			{
				continue;
			}
			val lowerBoundary = (int) (size * 0.8);
			val higherBoundary = (int) (size * 1.2);
			val deviation = random.nextInt(higherBoundary - lowerBoundary);
			val calcSize = size + deviation;
			var formatted = format.format(calcSize) + " K";
			formatted = " ".repeat(Math.max(0, PROCESS_SIZE_LENGTH - formatted.length())) + formatted;
			it.set(item.replace(formattedString, formatted));
		}
		return list;
	}

	private static List<String> loadFromResource(InputStream is) throws IOException
	{
		val list = new ArrayList<String>();
		val br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null)
		{
			list.add(line);
		}
		return list;
	}

	private static byte parseByte(final String s)
	{
		if (s.length() != 2)
		{
			throw new NumberFormatException("Parse byte can only convert strings of size 2.");
		}

		return (byte) ((Character.digit(s.charAt(0), 16) << 4) + Character.digit(s.charAt(1), 16));
	}

	private static String generateMacAddress(
			final boolean uaa,
			final boolean multicast,
			final String oui, final
			String separator, final
			String byteFmt)
	{
		var mac = new byte[6];
		random.nextBytes(mac);

		if (oui != null)
		{
			val chunk = Arrays.stream(oui.split(separator)).map(Providers::parseByte).collect(Providers.toByteArray());
			System.arraycopy(chunk, 0, mac, 0, chunk.length);
		}
		else
		{
			if (multicast)
			{
				mac[0] |= 1;
			}
			else
			{
				mac[0] &= ~1;
			}
			if (uaa)
			{
				mac[0] &= ~(1 << 1);
			}
			else
			{
				mac[0] |= 1 << 1;
			}
		}

		val sb = new StringBuilder();
		for (int i = 0; i < mac.length; i++)
		{
			sb.append(String.format(byteFmt, mac[i], (i < mac.length - 1) ? "-" : ""));
		}
		return sb.toString();
	}

	public static Collector<Byte, ?, byte[]> toByteArray()
	{
		return Collector.of(ByteArrayOutputStream::new, ByteArrayOutputStream::write, (baos1, baos2) ->
		{
			try
			{
				baos2.writeTo(baos1);
				return baos1;
			}
			catch (IOException e)
			{
				throw new UncheckedIOException(e);
			}
		}, ByteArrayOutputStream::toByteArray);
	}
}
