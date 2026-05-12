package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.service.EatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/testrouter")
public class Eatcontroller {

    @Autowired
    private EatService eatService;

    @GetMapping("/getNum")
    public List<Integer> getNum(){
        return eatService.test();
    }
}

// 页面访问 http://localhost:8080/soulfood/testrouter/getNum
