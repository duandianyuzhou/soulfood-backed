package com.food.soulfoodbackend.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class EatService {

    public List<Integer> test(){
        List<Integer> s = Stream.of(1,2,3,4,5,6).toList();
        return s;
    }
}
