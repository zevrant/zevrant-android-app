package com.zevrant.services.zevrantandroidapp.test;


import io.cucumber.junit.CucumberOptions;

@CucumberOptions(features = {"features"},
        glue = {"com.zevrant.services.zevrantandroidapp.steps"},
        plugin = {"pretty", // Cucumber report formats and location to store them in phone
                "html:/sdcard/Documents/cucumber-reports/html-report",
                "json:/sdcard/Documents/cucumber-reports/cucumber.json",
                "junit:/sdcard/Documents/cucumber-reports/cucumber.xml"
        },
        tags = "")
public class CucumberTestCase {

}
