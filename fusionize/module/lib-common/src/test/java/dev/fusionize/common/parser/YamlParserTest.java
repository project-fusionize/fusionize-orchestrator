package dev.fusionize.common.parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class YamlParserTest {
    YamlParser<Customer> yamlParser;
    Customer parsed;
    URL url;

    @BeforeEach
    void setUp() {
        url = this.getClass().getResource("/yaml/test.yml");
        assertNotNull(url);
        yamlParser = new YamlParser<>();

    }

    @Test
    void fromYamlString() throws IOException {
        String yml = Files.readString(new File(url.getFile()).toPath());
        parsed = yamlParser.fromYaml(yml, Customer.class);
        assertNotNull(parsed);
        verifyCustomer(parsed);
    }

    @Test
    void fromYamlInputStream() throws IOException {
        InputStream ymlInputStream = url.openStream();
        parsed = yamlParser.fromYaml(ymlInputStream, Customer.class);
        assertNotNull(parsed);
        verifyCustomer(parsed);
    }

    @Test
    void toYaml() throws IOException {
        fromYamlInputStream();
        String yml = yamlParser.toYaml(parsed);
        System.out.println(yml);

    }

    public void verifyCustomer(Customer customer){
        assertNotNull(customer);
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals(31, customer.getAge());
        assertNotNull(customer.getContactDetails());
        assertEquals(2, customer.getContactDetails().size());

        assertEquals("mobile", customer.getContactDetails()
                .get(0)
                .getType());
        assertEquals(123456789, customer.getContactDetails()
                .get(0)
                .getNumber());
        assertEquals("landline", customer.getContactDetails()
                .get(1)
                .getType());
        assertEquals(456786868, customer.getContactDetails()
                .get(1)
                .getNumber());
        assertNotNull(customer.getHomeAddress());
        assertEquals("Xyz, DEF Street", customer.getHomeAddress()
                .getLine());
    }


    public static class Customer {
        private String firstName;
        private String lastName;
        private int age;
        private List<Contact> contactDetails;
        private Address homeAddress;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<Contact> getContactDetails() {
            return contactDetails;
        }

        public void setContactDetails(List<Contact> contactDetails) {
            this.contactDetails = contactDetails;
        }

        public Address getHomeAddress() {
            return homeAddress;
        }

        public void setHomeAddress(Address homeAddress) {
            this.homeAddress = homeAddress;
        }
    }

    public static class Address {
        private String line;
        private String city;
        private String state;
        private Integer zip;

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Integer getZip() {
            return zip;
        }

        public void setZip(Integer zip) {
            this.zip = zip;
        }
    }

    public static class Contact {
        private String type;
        private int number;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }
}