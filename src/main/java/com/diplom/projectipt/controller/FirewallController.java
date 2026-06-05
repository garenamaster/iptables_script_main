package com.diplom.projectipt.controller;

import com.diplom.projectipt.api.ApiResponse;
import com.diplom.projectipt.service.FirewallService;
import com.diplom.projectipt.service.FirewallStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/firewall")
public class FirewallController {

    private final FirewallService firewallService;

    public FirewallController(FirewallService firewallService) {
        this.firewallService = firewallService;
    }

    @PostMapping("/allow")
    public ResponseEntity<ApiResponse<Void>> allowIp(@RequestParam String ip) {
        boolean changed = firewallService.allowIp(ip);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("IP " + ip + " разрешен (80,443)")
                : ApiResponse.success("IP уже разрешен"));
    }

    @PostMapping("/allow-port")
    public ResponseEntity<ApiResponse<Void>> allowIpToPort(@RequestParam String ip,
                                                           @RequestParam int port) {
        boolean changed = firewallService.allowIpToPort(ip, port);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("IP разрешен к порту")
                : ApiResponse.success("Правило уже существует"));
    }

    @DeleteMapping("/allow-port")
    public ResponseEntity<ApiResponse<Void>> removeIpFromPort(@RequestParam String ip,
                                                              @RequestParam int port) {
        boolean changed = firewallService.removeIpFromPort(ip, port);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("Правило разрешения удалено")
                : ApiResponse.success("Правила не было"));
    }

    @PostMapping("/block")
    public ResponseEntity<ApiResponse<Void>> blockIp(@RequestParam String ip) {
        boolean changed = firewallService.blockIp(ip);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("IP заблокирован")
                : ApiResponse.success("IP уже заблокирован"));
    }

    @DeleteMapping("/block")
    public ResponseEntity<ApiResponse<Void>> unblockIp(@RequestParam String ip) {
        boolean changed = firewallService.unblockIp(ip);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("Блокировка IP снята")
                : ApiResponse.success("IP не был заблокирован"));
    }

    @PostMapping("/open-port")
    public ResponseEntity<ApiResponse<Void>> openPort(@RequestParam int port) {
        boolean changed = firewallService.openPort(port);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("Порт открыт")
                : ApiResponse.success("Порт уже открыт"));
    }

    @DeleteMapping("/open-port")
    public ResponseEntity<ApiResponse<Void>> closePort(@RequestParam int port) {
        boolean changed = firewallService.closePort(port);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("Порт закрыт")
                : ApiResponse.success("Порт уже закрыт"));
    }

    @PostMapping("/set-policy")
    public ResponseEntity<ApiResponse<Void>> setChainPolicy(@RequestParam String chain,
                                                            @RequestParam String policy) {
        firewallService.setChainPolicy(chain, policy);
        return ResponseEntity.ok(ApiResponse.success("Политика цепочки обновлена"));
    }

    @PostMapping("/close-all")
    public ResponseEntity<ApiResponse<Void>> closeAll() {
        firewallService.closeAll();
        return ResponseEntity.ok(ApiResponse.success("INPUT = DROP"));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<String>>> getRules() {
        return ResponseEntity.ok(ApiResponse.success("Список правил получен", firewallService.getRules()));
    }

    @GetMapping("/rules-detailed")
    public ResponseEntity<ApiResponse<List<String>>> getRulesDetailed() {
        return ResponseEntity.ok(ApiResponse.success("Подробный список правил получен", firewallService.getRulesDetailed()));
    }

    @GetMapping("/rule-exists")
    public ResponseEntity<ApiResponse<Boolean>> ruleExists(@RequestParam String ip,
                                                           @RequestParam int port) {
        return ResponseEntity.ok(ApiResponse.success("Проверка правила выполнена", firewallService.ruleExists(ip, port)));
    }

    @DeleteMapping("/rule-line")
    public ResponseEntity<ApiResponse<Void>> deleteRuleByLineNumber(@RequestParam String chain,
                                                                    @RequestParam int lineNumber) {
        boolean changed = firewallService.deleteRuleByLineNumber(chain, lineNumber);
        return ResponseEntity.ok(changed
                ? ApiResponse.success("Правило удалено")
                : ApiResponse.success("Правило с таким номером не найдено"));
    }

    @PostMapping("/flush-chain")
    public ResponseEntity<ApiResponse<Void>> flushChain(@RequestParam String chain) {
        firewallService.flushChain(chain);
        return ResponseEntity.ok(ApiResponse.success("Цепочка очищена"));
    }

    @PostMapping("/chain")
    public ResponseEntity<ApiResponse<Void>> createChain(@RequestParam String chainName) {
        firewallService.createChain(chainName);
        return ResponseEntity.ok(ApiResponse.success("Операция create chain выполнена"));
    }

    @DeleteMapping("/chain")
    public ResponseEntity<ApiResponse<Void>> deleteChain(@RequestParam String chainName) {
        firewallService.deleteChain(chainName);
        return ResponseEntity.ok(ApiResponse.success("Операция delete chain выполнена"));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveRules() {
        String snapshot = firewallService.saveRules();
        return ResponseEntity.ok(ApiResponse.success("Правила сохранены", snapshot));
    }

    @GetMapping("/open-ports")
    public ResponseEntity<ApiResponse<List<Integer>>> getOpenPorts() {
        return ResponseEntity.ok(ApiResponse.success("Список открытых портов получен", firewallService.getOpenPorts()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<FirewallStatus>> getFirewallStatus() {
        return ResponseEntity.ok(ApiResponse.success("Статус firewall получен", firewallService.getFirewallStatus()));
    }

    @PostMapping("/setup-basic")
    public ResponseEntity<ApiResponse<Void>> setupBasicFirewall(@RequestParam(defaultValue = "22") int sshPort) {
        firewallService.setupBasicFirewall(sshPort);
        return ResponseEntity.ok(ApiResponse.success("Базовая конфигурация firewall применена"));
    }
}