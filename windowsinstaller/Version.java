import java.io.*;

public class Version
{
	public static void main(String[] args) throws Exception
	{
		String[] versionbits = System.getProperty("java.version").split("\\.");
		String major = versionbits[0];
		String minor = versionbits[1];
		while(minor.length() < 3)
		{
			minor = "0" + minor;
		}
	
		PrintStream print = new PrintStream(new File(args[0]));
		print.print(major+minor);
		print.close();
	}
}