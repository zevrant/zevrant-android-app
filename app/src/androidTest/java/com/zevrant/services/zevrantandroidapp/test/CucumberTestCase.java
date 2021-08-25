package com.zevrant.services.zevrantandroidapp.test;


import io.cucumber.junit.CucumberOptions;

@CucumberOptions(features={"features"},
        glue = {"com.zevrant.services.zevrantandroidapp.steps"})
public class CucumberTestCase {

}
