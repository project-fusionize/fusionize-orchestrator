package dev.fusionize.common.parser;

import dev.fusionize.common.utility.KeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {

    TestClassA source;
    JsonParser<TestClassA> jsonParser = new JsonParser<>();

    @BeforeEach
    void setUp() {
        source = setupSource();
    }

    TestClassA setupSource(){
        TestClassA source = new TestClassA();
        source.list = new ArrayList<>();
        source.map = new HashMap<>();
        for(int i=0; i<3; i++){
            TestClassB bi = new TestClassB();
            bi.setA(i%2 == 0 ? String.valueOf(i) : KeyUtil.getRandomAlphabeticalKey(3));
            bi.setB(i);
            source.list.add(bi);
            source.map.put(String.valueOf(i), bi);
        }
        return source;
    }

    @Test
    void toFromJson() {
        String json = jsonParser.toJson(source, TestClassA.class);
        System.out.println(json);
        TestClassA target = jsonParser.fromJson(json, TestClassA.class);
        assertNotNull(target);
        target.list.sort(Comparator.comparingInt(TestClassB::getB));
        for(int i=0; i< source.list.size(); i++){
            assertEquals(source.list.get(i).a, target.list.get(i).a);
            assertEquals(source.list.get(i), target.list.get(i));
        }

        source.map.keySet().forEach((k)-> {
            assertEquals(source.map.get(k).a, target.map.get(k).a);
            assertEquals(source.map.get(k), target.map.get(k));
        });

        assertEquals(source,target);
    }

    static class TestClassB {
        private String a;
        private int b;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassB that = (TestClassB) o;
            return b == that.b && Objects.equals(a, that.a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }

    static class TestClassA {
        private Map<String, TestClassB> map;
        private List<TestClassB> list;

        public Map<String, TestClassB> getMap() {
            return map;
        }

        public void setMap(Map<String, TestClassB> map) {
            this.map = map;
        }

        public List<TestClassB> getList() {
            return list;
        }

        public void setList(List<TestClassB> list) {
            this.list = list;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassA that = (TestClassA) o;
            return Objects.equals(map, that.map) && Objects.equals(list, that.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map, list);
        }
    }

}