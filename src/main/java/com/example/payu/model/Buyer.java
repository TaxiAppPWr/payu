package com.example.payu.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Buyer {
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String language = "pl";
}