package com.alibaba.jvm.sandbox.qatest.core.util.matcher;

import java.util.Collection;
import java.util.List;
import java.util.Set;

enum Sex {
    MAN, WOMAN
}

public class PublicUser {

    enum Occupation {
        STUDENT,
        TEACHER,
        ENGINEER
    }

    class Address {

    }

    static class StaticAddress {

    }

    interface Face {

    }

    public PublicUser(String username, int age, Sex sex, Occupation occupation, Address address, StaticAddress staticAddress, PublicUser friend, Face face) {
        this.username = username;
        this.age = age;
        this.sex = sex;
        this.occupation = occupation;
        this.address = address;
        this.staticAddress = staticAddress;
        this.friend = friend;
        this.face = face;
    }

    private String username;
    private int age;
    private Sex sex;
    private Occupation occupation;
    private Address address;
    private StaticAddress staticAddress;
    private PublicUser friend;
    private Face face;

    private Collection<String> usernames;
    private Collection<Integer> ages;
    private Collection<Sex> sexes;
    private Collection<Occupation> occupations;
    private Collection<Address> addresses;
    private Collection<StaticAddress> staticAddresses;
    private Collection<PublicUser> publicUsers;
    private Collection<Face> faces;

    private String[] usernameArray;
    private int[] ageArray;
    private Sex[] sexArray;
    private Occupation[] occupationArray;
    private Address[] addressArray;
    private StaticAddress[] staticAddressArray;
    private PublicUser[] friendArray;
    private Face[] faceArray;


    // --- GETTER & SETTER


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Occupation getOccupation() {
        return occupation;
    }

    public void setOccupation(Occupation occupation) {
        this.occupation = occupation;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public StaticAddress getStaticAddress() {
        return staticAddress;
    }

    public void setStaticAddress(StaticAddress staticAddress) {
        this.staticAddress = staticAddress;
    }

    public PublicUser getFriend() {
        return friend;
    }

    public void setFriend(PublicUser friend) {
        this.friend = friend;
    }

    public Face getFace() {
        return face;
    }

    public void setFace(Face face) {
        this.face = face;
    }

    public Collection<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(Collection<String> usernames) {
        this.usernames = usernames;
    }

    public Collection<Integer> getAges() {
        return ages;
    }

    public void setAges(Collection<Integer> ages) {
        this.ages = ages;
    }

    public Collection<Sex> getSexes() {
        return sexes;
    }

    public void setSexes(Collection<Sex> sexes) {
        this.sexes = sexes;
    }

    public Collection<Occupation> getOccupations() {
        return occupations;
    }

    public void setOccupations(Collection<Occupation> occupations) {
        this.occupations = occupations;
    }

    public Collection<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(Collection<Address> addresses) {
        this.addresses = addresses;
    }

    public Collection<StaticAddress> getStaticAddresses() {
        return staticAddresses;
    }

    public void setStaticAddresses(Collection<StaticAddress> staticAddresses) {
        this.staticAddresses = staticAddresses;
    }

    public Collection<PublicUser> getPublicUsers() {
        return publicUsers;
    }

    public void setPublicUsers(Collection<PublicUser> publicUsers) {
        this.publicUsers = publicUsers;
    }

    public Collection<Face> getFaces() {
        return faces;
    }

    public void setFaces(Collection<Face> faces) {
        this.faces = faces;
    }

    public String[] getUsernameArray() {
        return usernameArray;
    }

    public void setUsernameArray(String[] usernameArray) {
        this.usernameArray = usernameArray;
    }

    public int[] getAgeArray() {
        return ageArray;
    }

    public void setAgeArray(int[] ageArray) {
        this.ageArray = ageArray;
    }

    public Sex[] getSexArray() {
        return sexArray;
    }

    public void setSexArray(Sex[] sexArray) {
        this.sexArray = sexArray;
    }

    public Occupation[] getOccupationArray() {
        return occupationArray;
    }

    public void setOccupationArray(Occupation[] occupationArray) {
        this.occupationArray = occupationArray;
    }

    public Address[] getAddressArray() {
        return addressArray;
    }

    public void setAddressArray(Address[] addressArray) {
        this.addressArray = addressArray;
    }

    public StaticAddress[] getStaticAddressArray() {
        return staticAddressArray;
    }

    public void setStaticAddressArray(StaticAddress[] staticAddressArray) {
        this.staticAddressArray = staticAddressArray;
    }

    public PublicUser[] getFriendArray() {
        return friendArray;
    }

    public void setFriendArray(PublicUser[] friendArray) {
        this.friendArray = friendArray;
    }

    public Face[] getFaceArray() {
        return faceArray;
    }

    public void setFaceArray(Face[] faceArray) {
        this.faceArray = faceArray;
    }
}
