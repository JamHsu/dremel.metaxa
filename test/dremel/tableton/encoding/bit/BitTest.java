/**
 * Copyright 2010, Petascan Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.Ope
 */

package dremel.tableton.encoding.bit;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;

import dremel.tableton.impl.encoding.bit.BitDecoderImpl;
import dremel.tableton.impl.encoding.bit.BitEncoderImpl;

/**
 * 
 * @author babay
 */
public class BitTest {
	
	static int maxOSArraySize = 256;

	@Test
	public void streamTest() throws IOException  {
        byte[] b1 = new byte[8192*5];
        
        for(int i = 0; i < b1.length; i++) {
        	b1[i] = (byte)(i & 0x7);
        	System.out.println("value is " + b1[i]);
        }
       
        java.io.File  f = new java.io.File("testData/bitTest");
       
        java.io.DataOutputStream dataOutput = null;
        try {
            dataOutput = new java.io.DataOutputStream(new java.io.FileOutputStream(f));
        } catch (java.io.FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BitEncoderImpl encoder = BitEncoderImpl.instance(dataOutput, 3);
        for(int i=0; i < b1.length; i++)
        {
        	encoder.write(b1[i]);
        }
        encoder.close();
       
        java.io.DataInputStream dataInput = null;
        try {
            dataInput = new java.io.DataInputStream(new java.io.FileInputStream(f));
        } catch (java.io.FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BitDecoderImpl decoder = BitDecoderImpl.instance(dataInput);
        int index = 0;
        while(true)
        {
            try
            {
                int nextValue = decoder.read();
                assertTrue(nextValue == b1[index]);
                System.out.println("nextValue = " + nextValue + "; b1 [" + index + "] = " + b1[index]);
                index++;
            }catch(Exception ex)
            {
                break;
            }
        }
       
    }	
	
}
