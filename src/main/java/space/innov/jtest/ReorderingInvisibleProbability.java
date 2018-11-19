/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package space.innov.jtest;

import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
/**
 *
 * @author <a href="mailto:chenjuq@gmail.com">Fraank</a>
 * @since 0.0.1-SNAPSHOT
 */

public class ReorderingInvisibleProbability {

    private static final Unsafe unsafe = getUnsafe();
    private static final long xOffset;
    private static final long aOffset;
    private static final long yOffset;
    private static final long bOffset;


    public static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
        }
        return null;
    }

    static {
        try {
            xOffset = unsafe.objectFieldOffset(ReorderingInvisibleProbability.class.getDeclaredField("x"));
            aOffset = unsafe.objectFieldOffset(ReorderingInvisibleProbability.class.getDeclaredField("a"));
            yOffset = unsafe.objectFieldOffset(ReorderingInvisibleProbability.class.getDeclaredField("y"));
            bOffset = unsafe.objectFieldOffset(ReorderingInvisibleProbability.class.getDeclaredField("b"));

        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private int x = 0, a = 0;
    private int y = 0, b = 0;
    private volatile int countDown = 0;

    protected final void putIntx(int x) {
        unsafe.putInt(this, xOffset, x);
    }

    protected final int getIntx() {
        return unsafe.getInt(this, xOffset);
    }

    protected final void putInta(int x) {
        unsafe.putInt(this, aOffset, x);
    }

    protected final void putOrderedInta(int x) {
        unsafe.putOrderedInt(this, aOffset, x);
    }

    protected final int getInta() {
        return unsafe.getInt(this, aOffset);
    }

    protected final void putInty(int x) {
        unsafe.putInt(this, yOffset, x);
    }

    protected final int getInty() {
        return unsafe.getInt(this, yOffset);
    }

    protected final void putIntb(int x) {
        unsafe.putInt(this, bOffset, x);
    }

    protected final void putOrderedIntb(int x) {
        unsafe.putOrderedInt(this, bOffset, x);
    }

    protected final int getIntb() {
        return unsafe.getInt(this, bOffset);
    }


    public static int testPossibleReorderAndInvisible() throws InterruptedException, IOException {
        int i = 0;
        for (; ; ) {
            i++;
            ReorderingInvisibleProbability reorderingInvisibleProbability = new ReorderingInvisibleProbability();
            //确保两线程开始执行的时间差尽可能短
            reorderingInvisibleProbability.countDown = 1;
            Thread one = new Thread(new Runnable() {
                public void run() {
                    while (reorderingInvisibleProbability.countDown != 0) {
                    }
                    reorderingInvisibleProbability.a = 1;
                    //确保字段x主线程可见
                    reorderingInvisibleProbability.putIntx(reorderingInvisibleProbability.b);
                }
            });

            Thread other = new Thread(new Runnable() {

                public void run() {
                    reorderingInvisibleProbability.countDown--;
                    reorderingInvisibleProbability.b = 1;
                    //确保字段y主线程可见
                    reorderingInvisibleProbability.putInty(reorderingInvisibleProbability.a);
                }
            });
            one.start();
            other.start();
            one.join();
            other.join();
            int x = reorderingInvisibleProbability.getIntx();
            int y = reorderingInvisibleProbability.getInty();
            String result = "第" + i + "次:(" + x + "," + y + "）";
            if (x == 0 && y == 0) {
                System.err.println(result);
                List<String> list = new ArrayList<String>(1);
                list.add("" + i);
                String fileName="testPossibleReorderAndInvisible.txt";
                if(!Paths.get(fileName).toFile().exists()){
                    Paths.get(fileName).toFile().createNewFile();
                }
                Files.write(Paths.get(fileName), list, StandardOpenOption.APPEND);
                break;
            } else {
                System.out.println(result);
            }
        }
        return i;
    }

    public static int testPossibleReorder() throws InterruptedException, IOException {
        int i = 0;
        for (; ; ) {
            i++;
            ReorderingInvisibleProbability reorderingInvisibleProbability = new ReorderingInvisibleProbability();
            //确保两线程开始执行的时间差尽可能短
            reorderingInvisibleProbability.countDown = 1;
            Thread one = new Thread(new Runnable() {
                public void run() {
                    while (reorderingInvisibleProbability.countDown != 0) {
                    }
                    //确保值1写入堆内存字段a，但不保证和后面的程序重排序
                    reorderingInvisibleProbability.putInta(1);
                    //确保获取到堆内存中的字段b
                    int intb = reorderingInvisibleProbability.getIntb();
                    //确保字段x主线程可见
                    reorderingInvisibleProbability.putIntx(intb);

                }
            });

            Thread other = new Thread(new Runnable() {
                public void run() {
                    reorderingInvisibleProbability.countDown--;
                    //确保值1写入堆内存字段b，但不保证和后面的程序重排序
                    reorderingInvisibleProbability.putIntb(1);
                    //确保获取到堆内存中的字段a
                    int inta = reorderingInvisibleProbability.getInta();
                    //确保字段y主线程可见
                    reorderingInvisibleProbability.putInty(inta);

                }
            });
            one.start();
            other.start();
            one.join();
            other.join();
            int x = reorderingInvisibleProbability.getIntx();
            int y = reorderingInvisibleProbability.getInty();
            String result = "第" + i + "次:(" + x + "," + y + "）";
            if (x == 0 && y == 0) {
                System.err.println(result);
                List<String> list = new ArrayList<String>(1);
                list.add("" + i);
                String fileName="testPossibleReorder.txt";
                if(!Paths.get(fileName).toFile().exists()){
                    Paths.get(fileName).toFile().createNewFile();
                }
                Files.write(Paths.get(fileName), list, StandardOpenOption.APPEND);
                break;
            } else {
                System.out.println(result);
            }
        }
        return i;
    }

    public static int testPossibleInvisible() throws InterruptedException, IOException {
        int i = 0;
        for (; ; ) {
            i++;
            ReorderingInvisibleProbability reorderingInvisibleProbability = new ReorderingInvisibleProbability();
            //确保两线程开始执行的时间差尽可能短
            reorderingInvisibleProbability.countDown = 1;
            Thread one = new Thread(new Runnable() {
                public void run() {
                    while (reorderingInvisibleProbability.countDown != 0) {
                    }
                    //确保此语句的执行不被重排序，但不保证堆内存字段a立即可见
                    reorderingInvisibleProbability.putOrderedInta(1);
                    //确保获取到堆内存中的字段b
                    int intb = reorderingInvisibleProbability.getIntb();
                    //确保字段x主线程可见
                    reorderingInvisibleProbability.putIntx(intb);
                }
            });

            Thread other = new Thread(new Runnable() {
                public void run() {
                    reorderingInvisibleProbability.countDown--;
                    //确保此语句的执行不被重排序，但不保证堆内存字段b立即可见
                    reorderingInvisibleProbability.putOrderedIntb(1);
                    //确保获取到堆内存中的字段a
                    int inta = reorderingInvisibleProbability.getInta();
                    //确保字段y主线程可见
                    reorderingInvisibleProbability.putInty(inta);
                }
            });
            one.start();
            other.start();
            one.join();
            other.join();
            int x = reorderingInvisibleProbability.getIntx();
            int y = reorderingInvisibleProbability.getInty();
            String result = "第" + i + "次:(" + x + "," + y + "）";
            if (x == 0 && y == 0) {
                System.err.println(result);
                List<String> list = new ArrayList<String>(1);
                list.add("" + i);
                String fileName="testPossibleInvisible.txt";
                if(!Paths.get(fileName).toFile().exists()){
                    Paths.get(fileName).toFile().createNewFile();
                }
                Files.write(Paths.get(fileName), list, StandardOpenOption.APPEND);
                break;
            } else {
                System.out.println(result);
            }
        }
        return i;
    }



    private static Function<String, String> getTestFunction() throws ClassNotFoundException {
        final Class<?> clazz = Class.forName(ReorderingInvisibleProbability.class.getName());
        Function<String, String> testFun = methodName -> {
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            final int total = 100;
            for (int i = 0; i < total; i++) {
                Method method = null;
                try {
                    method = clazz.getDeclaredMethod(methodName, null);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                try {
                    int count = (int) method.invoke(clazz);
                    sum += count;
                    if (count < min) {
                        min = count;
                    }
                    if (count > max) {
                        max = count;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

            }
            StringBuilder reportBuffer = new StringBuilder();
            reportBuffer.append(methodName);
            reportBuffer.append("\r\n").append("avgProbability=").append(1.0/(sum / total));
            reportBuffer.append("\r\n").append("maxProbability=").append(1.0/min);
            reportBuffer.append("\r\n").append("minProbability=").append(1.0/max);

            return reportBuffer.toString();
        };
        return testFun;
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Function<String, String> testFunction = getTestFunction();
        StringBuilder reportBuffer = new StringBuilder();
        for (String methodName : new String[]{"testPossibleReorderAndInvisible", "testPossibleInvisible", "testPossibleReorder"}) {
            try {
                String report = testFunction.apply(methodName);
                reportBuffer.append("\r\n*************").append(report);
            }catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }
        System.out.println(reportBuffer.toString());

    }
}


