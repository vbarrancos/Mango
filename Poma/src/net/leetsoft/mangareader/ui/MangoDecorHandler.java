package net.leetsoft.mangareader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import net.leetsoft.mangareader.Mango;
import net.leetsoft.mangareader.MangoHttp;
import net.leetsoft.mangareader.MangoHttpResponse;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class MangoDecorHandler
{
    public void downloadMissingBackground(Context c)
    {
        ArrayList<MangoBackground> l = this.parseDecorXml(readDecorXml());
        for (Iterator iterator = l.iterator(); iterator.hasNext(); )
        {
            MangoBackground mangoBackground = (MangoBackground) iterator.next();
            if (mangoBackground.downloaded == false)
            {
                MangoHttpResponse resp = MangoHttp.downloadData(mangoBackground.url, c);
                resp.writeEncodedImageToCache(2, null, mangoBackground.name);
            }
        }

        try
        {

            //delete nonbackground files
            File[] f = new File(Mango.getDataDirectory() + "/Mango/backgrounds/").listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String filename)
                {
                    if (filename.endsWith(".png"))
                        return true;
                    return false;
                }
            });
            for (int i = 0; i < f.length; i++)
            {
                boolean delete = true;
                for (Iterator iterator = l.iterator(); iterator.hasNext(); )
                {
                    MangoBackground mangoBackground = (MangoBackground) iterator.next();
                    if (f[i].getName().equals(mangoBackground.name + ".png"))
                        delete = false;
                }

                if (delete == true)
                    f[i].delete();
            }
        }
        catch (Exception e)
        {
            Mango.log("MangoDecorHandler", "yum, swallowed exception! " + e.toString());
        }
    }

    public static void writeDecorImageToDisk(byte[] img, String name)
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File file = null;
            FileOutputStream out = null;
            try
            {
                file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/");
                if (file.exists())
                    file.delete();
                file.getParentFile().mkdirs();
                file.createNewFile();

                file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/.nomedia");
                if (file.exists())
                    file.delete();
                file.getParentFile().mkdirs();
                file.createNewFile();

                file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/" + name + ".png");
                if (file.exists())
                    file.delete();
                file.createNewFile();

                out = new FileOutputStream(file);
                out.write(img);
            }
            catch (IOException ioe)
            {
                Mango.log("MangoDecorHandler", "writeDecorImageToDisk: Problem! (" + String.valueOf(file.getAbsolutePath()) + ", " + name + ", " + ioe.getMessage() + ")");
            }
            finally
            {
                try
                {
                    if (out != null)
                        out.close();
                    out = null;
                }
                catch (IOException e)
                {

                }
            }
        }
    }

    public void downloadDecorXml(Context c)
    {
        MangoHttpResponse resp = MangoHttp.downloadData("http://mango.leetsoft.net/backgrounds/android/decorindex.xml", c);
        String xmlData = resp.toString();
        if (resp.exception)
        {
            Mango.getSharedPreferences().edit().putLong("nextDecorCheck", System.currentTimeMillis() + (1000 * 60)).commit();
            Mango.log("MangoDecorHandler", "downloadDecorXml: " + xmlData);
            return;
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        {
            File file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/index.xml");
            BufferedWriter out = null;

            try
            {
                if (file.exists())
                    file.delete();
                file.getParentFile().mkdirs();
                file.createNewFile();
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                out.write(xmlData);
            }
            catch (IOException ioe)
            {
                Mango.getSharedPreferences().edit().putLong("nextDecorCheck", System.currentTimeMillis() + (1000 * 60)).commit();
                Mango.log("MangoDecorHandler", "downloadDecorXml: " + ioe.toString());
                return;
            }
            finally
            {
                try
                {
                    if (out != null)
                        out.close();
                    out = null;
                }
                catch (IOException e)
                {

                }
            }
        }
        else
        {
            Mango.getSharedPreferences().edit().putLong("nextDecorCheck", System.currentTimeMillis() + (1000 * 60)).commit();
            return;
        }
    }

    public static Bitmap readDecorBitmap(String name)
    {
        String state = Environment.getExternalStorageState();
        if (state.startsWith(Environment.MEDIA_MOUNTED))
        {
            File file;
            try
            {
                file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/" + name + ".png");
                if (!file.exists())
                    throw new Exception("File does not exist (" + file.getAbsolutePath() + ")");
                // long time = System.currentTimeMillis();

                Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bm == null)
                    throw new Exception("Couldn't decode file " + file.getAbsolutePath() + ", not a valid bitmap or file couldn't be accessed.");
                return bm;
            }
            catch (Exception ioe)
            {
                Mango.log("MangoDecorHandler", "Exception when reading decor bitmap! " + ioe.getMessage() + ")");
            }
            catch (OutOfMemoryError oom)
            {
                Mango.log("MangoDecorHandler", "OutOfMemory when reading decor bitmap");
            }
        }
        else
        {
            Mango.log("MangoDecorHandler", "Couldn't read decor bitmap because SD card is locked.");
        }
        return null;
    }

    public String readDecorXml()
    {

        String state = Environment.getExternalStorageState();
        if (state.startsWith(Environment.MEDIA_MOUNTED))
        {
            File file;
            BufferedReader br = null;
            try
            {
                file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/index.xml");
                if (!file.exists())
                    return null;
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[8192];
                int charsRead = 0;
                while ((charsRead = br.read(buffer)) > 0)
                {
                    builder.append(buffer, 0, charsRead);
                    buffer = new char[8192];
                }
                return builder.toString();
            }
            catch (IOException ioe)
            {
                Mango.log("MangoDecorHandler", "readDecorXml: " + ioe.toString());
                return null;
            }
            finally
            {
                try
                {
                    if (br != null)
                        br.close();
                    br = null;
                }
                catch (IOException e)
                {

                }
            }
        }
        return null;
    }

    public static boolean checkForDecorIndex()
    {
        String state = Environment.getExternalStorageState();
        if (state.startsWith(Environment.MEDIA_MOUNTED))
        {
            File file;
            file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/index.xml");
            if (!file.exists())
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return false;
        }
    }

    public static boolean checkForDecorBackground(String name)
    {
        String state = Environment.getExternalStorageState();
        if (state.startsWith(Environment.MEDIA_MOUNTED))
        {
            File file;
            file = new File(Mango.getDataDirectory() + "/Mango/backgrounds/" + name + ".png");
            if (!file.exists())
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return false;
        }
    }

    public ArrayList<MangoBackground> parseDecorXml(String data)
    {
        ArrayList<MangoBackground> decor = new ArrayList<MangoBackground>();
        MangoBackground bg = new MangoBackground();
        bg.downloaded = true;
        bg.name = "dgrayman";
        bg.landscape = false;
        bg.url = "local";
        decor.add(bg);
        bg = new MangoBackground();
        bg.downloaded = true;
        bg.name = "airgear";
        bg.landscape = true;
        bg.url = "local";

        try
        {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            SAXParser parser = saxFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            DecorSaxHandler handler = new DecorSaxHandler();
            reader.setContentHandler(handler);
            reader.parse(new InputSource(new StringReader(data)));
            decor.addAll(handler.getAllDecor());
        }
        catch (Exception e)
        {
            // TODO: handle exception
        }

        return decor;
    }

    public class DecorSaxHandler extends DefaultHandler
    {
        ArrayList<MangoBackground> decorList;
        MangoBackground currentDecor;

        public ArrayList<MangoBackground> getAllDecor()
        {
            return this.decorList;
        }

        @Override
        public void startDocument() throws SAXException
        {
            super.startDocument();
            decorList = new ArrayList<MangoBackground>();
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
        {
            super.startElement(uri, localName, name, attributes);
            if (localName.equalsIgnoreCase("background"))
            {
                this.currentDecor = new MangoBackground();
            }
            else if (localName.equalsIgnoreCase("name"))
            {
                currentDecor.name = attributes.getValue(0);
            }
            else if (localName.equalsIgnoreCase("url"))
            {
                currentDecor.url = attributes.getValue(0);
            }
            else if (localName.equalsIgnoreCase("landscape"))
            {
                currentDecor.landscape = Boolean.parseBoolean(attributes.getValue(0));
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException
        {
            super.endElement(uri, localName, name);
            if (this.currentDecor != null)
            {
                if (localName.equalsIgnoreCase("background"))
                {
                    if (checkForDecorBackground(currentDecor.name))
                        currentDecor.downloaded = true;
                    decorList.add(currentDecor);
                }
            }
        }
    }
}
