package com.alexandrias-archives.backend.src.controller;

import com.alexandrias-archives.backend.src.model.GenerateRequest;
import com.alexandrias-archives.backend.src.service.GenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GenerationController {
  private final GeneratinoService gen_Service;
  public GenerationController(GenerationService gen_Service) {
    this.gen_Service = gen_Service;
  }

  @PostMapping("/generate")
  public ResponseEntity<?> generate(@RequestBody GenerateRequest request) {
    return ResponseEntity.ok(gen_Service.generate(request));
  }
}
