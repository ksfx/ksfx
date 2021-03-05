/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;


public class TeePrintStream extends PrintStream
{
    private PrintStream originalOut;
    private int outputPos;
    private ByteArrayOutputStream outputStream;
    final String ENC = System.getProperty("file.encoding", "UTF8");

    TeePrintStream(PrintStream originalOut)
    {
        super(new ByteArrayOutputStream());
        this.outputStream = (ByteArrayOutputStream)out;
        this.originalOut = originalOut;
        this.outputPos = 0;
    }

    @Override
    public void println(String str)
    {
        super.println(str);
        printTemp();
    }

    @Override
    public void print(String str)
    {
        super.print(str);
        printTemp();
    }
    
    private void printTemp()
    {
        try {
            byte[] newData = Arrays.copyOfRange(outputStream.toByteArray(), outputPos, outputStream.size());
            String tmp = new String(newData, ENC);
            originalOut.print(tmp);
            outputPos = outputStream.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getContent()
    {
        try {
            return outputStream.toString(ENC);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }
    
    public PrintStream getOriginalOut()
    {
        return originalOut;
    }

    static TeePrintStream teeStream = null;

    static void redirectOutput() 
    {
        teeStream = new TeePrintStream(System.out);
        System.setOut(teeStream);
    }

    static String restoreOutput() 
    {
        if (teeStream == null) {
            throw new IllegalStateException("please call 'redirectOutput' before calling 'restoreOutput'");
        }
        else {
            System.setOut(teeStream.getOriginalOut());
            return teeStream.getContent();
        }
    }
}

//def teeStream = new TeePrintStream(System.out)
//System.out = teeStream
//--------------------- Alle Ausgaben zwischen den Kommentaren landen in der variable Result und können in einer Datei gespeichert werden
//--------------------- Begin ---------------------

//println "hello world"

//--------------------- End ---------------------

//System.out = teeStream.getOriginalOut()
//result     = teeStream.getContent()

//new File("u:\\helloworld.txt").setText(result)
