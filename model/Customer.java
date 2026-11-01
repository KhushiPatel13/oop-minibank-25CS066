package model;

import java.io.Serializable;

import model.annotation.Id;

public class Customer implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String mobile;
    @Id
    private final String customerId;
    private final Address address;

    private static long customerCounter = 100;

    public Customer(String name, String email, String mobile) {
        this(name, email, mobile, null);
    }

    public Customer(String name, String email, String mobile, Address address) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.customerId = generateCustomerId();
        this.address = address;
    }

    private static String generateCustomerId() {
        return "CUST" + ++customerCounter;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public Customer clone() {
        try {
            return (Customer) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }

    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String line;
        private final String city;
        private final String pincode;

        public Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        public String getLine() {
            return line;
        }

        public String getCity() {
            return city;
        }

        public String getPincode() {
            return pincode;
        }
    }
}
