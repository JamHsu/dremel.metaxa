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

package dremel.tableton.encoding.rle;

import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import org.junit.Test;

import dremel.tableton.impl.encoding.rle.RleDecoderImpl;
import dremel.tableton.impl.encoding.rle.RleEncoderImpl;

/**
 * 
 * @author babay
 */
public class RleTest {
	
	static int maxOSArraySize = 256;
	
	@Test
	public void streamTest() throws IOException  {
		byte b1[] = null;
		java.io.DataOutputStream dataOutput = null, dataSourceOutput = null, dataSourceResult = null;
		java.io.DataInputStream dataSourceInput = null;
		java.io.File fs = new java.io.File("testData/rleTestSource");
		
		// constructing source data for rle testing
        dataSourceOutput = new java.io.DataOutputStream(new java.io.FileOutputStream(fs));
        for(int i = 0; i < 18195; i++) {
        	int k = (i + 1) & 0xFF;
        	b1 = new byte[k];
        	for(int j = 0; j < k; j++) {
        		b1[j] = (byte)k;
        	}
        	dataSourceOutput.write(b1);
        	b1 = null;
        }
        dataSourceOutput.close();
        // activating encoder; the data are reading from the constructred file
        dataSourceInput = new java.io.DataInputStream(new java.io.FileInputStream(fs));
        java.io.File f = new java.io.File("testData/rleTestEncoded");
        try {
            dataOutput = new java.io.DataOutputStream(new java.io.FileOutputStream(f));
        } catch (java.io.FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        RleEncoderImpl rleEncoder= RleEncoderImpl.instance(dataOutput);
        int availabelData = dataSourceInput.available();
        for(int i = 0; i < availabelData; i++) {
            rleEncoder.write(dataSourceInput.read());
        }
        rleEncoder.close();
        // activating decoder
		java.io.File fsRes = new java.io.File("testData/rleTestResult");
		// constructing source data for rle testing
        b1 = new byte[4096000];
        long estTime = 0;
		for(int l = 0; l < 10; l++) {
	        dataSourceResult = new java.io.DataOutputStream(new java.io.FileOutputStream(fsRes));
	        java.io.DataInputStream dataInput = null;
	        try {
	            dataInput = new java.io.DataInputStream(new java.io.FileInputStream(f));
	        } catch (java.io.FileNotFoundException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        RleDecoderImpl rleDecoder = RleDecoderImpl.instance(dataInput);
	        long sTime = System.nanoTime();
	        boolean written = false;
	        int readNeedles = 0;
	        while(true) {
	            try {
	            	written = false;
	                readNeedles = rleDecoder.read(b1);
                	dataSourceResult.write(b1);
                	written = true;
	            }
	            catch(Exception ex) {
	                break;
	            }
	        }
	        long fTime = System.nanoTime();
	        estTime += (fTime - sTime);
	        if (!written) {
	        	readNeedles = rleDecoder.getLastNumberOfDecodedNeedles();
	        	dataSourceResult.write(b1, 0, readNeedles);
	        }
	        dataSourceResult.close();
	        dataInput.close();
        }
		System.out.println("time " + estTime);
        b1 = null;
    }	
}
