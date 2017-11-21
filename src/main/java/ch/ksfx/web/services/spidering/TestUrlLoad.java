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

package ch.ksfx.web.services.spidering;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by Kejo on 21.02.2015.
 */
public class TestUrlLoad
{
    public static void main(String[] args)
    {
        try {
            BufferedInputStream in = new BufferedInputStream(new java.net.URL("http://www.finanzen.net/news/?intpagenr=4").openStream());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            BufferedOutputStream bout = new BufferedOutputStream(bos, 1024);

            byte data[] = new byte[1024];

            while (in.read(data, 0, 1024) >= 0) {
                bout.write(data);
            }

            bout.close();
            in.close();

            System.out.println(bos.toString());
        } catch (Exception e) {

        }
    }
}
