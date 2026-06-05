package com.diplom.projectipt.controller.dashboard;

import com.diplom.projectipt.tempaccess.TempAccessService;
import com.diplom.projectipt.tempaccess.dto.CreateTempAccessRequest;
import com.diplom.projectipt.tempaccess.dto.TempAccessDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/temp-access")
public class TempAccessController {

    private final TempAccessService tempAccessService;

    public TempAccessController(TempAccessService tempAccessService) {
        this.tempAccessService = tempAccessService;
    }

    @GetMapping
    public List<TempAccessDto> list() {
        return tempAccessService.listActive();
    }

    @PostMapping
    public TempAccessDto grant(@RequestBody CreateTempAccessRequest request) {
        return tempAccessService.grant(request);
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable long id) {
        tempAccessService.revokeNow(id);
    }
}
