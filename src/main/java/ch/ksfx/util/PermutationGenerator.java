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

import java.util.*;

//import static java.util.Collections.*;
 
public class PermutationGenerator
{
    public static <T> Iterable<List<T>> finiteCartesianProduct(final List<Collection<T>> sets)
    {
        return new Iterable<List<T>>() {

            private long size=1;
            {
                for(Collection<T> set:sets)size*=(long)set.size();
            }

            @Override
            public Iterator<List<T>> iterator() {
                return new Iterator<List<T>>(){
                    long counter=0;
                    ArrayList<T> currentValues=new ArrayList<T>(sets.size());
                    ArrayList<Iterator<T>> iterators=new ArrayList<Iterator<T>>(sets.size());
                    {
                        for(Iterable<T> set:sets){
                            Iterator<T> it=set.iterator();
                            iterators.add(it);
                            if(it.hasNext()){
                                //if not, then the size is 0, hasNext is never true, set empty
                                currentValues.add(it.next());
                            }
                        }
                    }
 
                    @Override
                    public boolean hasNext() {
                        return counter<size;
                    }
                    
                    @Override
                    public List<T> next() {
                        List<T> result=new LinkedList<T>(currentValues);
                        counter++;
                        increment(0);
                        return result;
                    }
                    
                    private void increment(int i){
                        if(iterators.get(i).hasNext()){
                            currentValues.set(i,iterators.get(i).next());
                        }else{
                            iterators.set(i,sets.get(i).iterator());
                            currentValues.set(i,iterators.get(i).next());
                            if(i<iterators.size()-1){
                                increment(i+1);
                            }
                        }
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("impossible to change combination set");
                    }
                };
            }
        };
    }
}