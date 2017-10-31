package com.va.ccse.rest.controller;

import com.va.ccse.rest.domain.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by Vamsi G on 2/7/2017.
 */

@SuppressWarnings("unused")
@RestController
@RequestMapping("/testController")
public class TestController {

    private static final String word = "Hello %s!";

    /*@CrossOrigin(origins = "http://localhost:9000")*/
    @RequestMapping(method = RequestMethod.GET)
    public Test test(@RequestParam(value = "name", required = false, defaultValue = "World") String name, HttpServletResponse response) throws ResourceNotFoundException {
        if (name.equalsIgnoreCase("world")) {
            return new Test(String.format(word, name));
        }
        throw new ResourceNotFoundException("Not a valid message");
    }

    @RequestMapping(method = RequestMethod.POST)
    public Test test() throws ResourceNotFoundException {
       return new Test("testing");
    }

    @RequestMapping(value = "/access", method = RequestMethod.GET)
    public String test1() {
        return "Access Denied";
    }

}
