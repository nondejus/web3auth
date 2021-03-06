package net.consensys.web3auth.module.login.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;
import net.consensys.web3auth.common.Constant;
import net.consensys.web3auth.common.dto.ClientType;
import net.consensys.web3auth.module.application.model.Application.Client;
import net.consensys.web3auth.module.application.service.ApplicationService;
import net.consensys.web3auth.module.common.CookieUtils;
import net.consensys.web3auth.module.login.model.LoginRequest;
import net.consensys.web3auth.module.login.model.OTS;
import net.consensys.web3auth.module.login.service.LoginService;

@Controller
@RequestMapping("/")
@Slf4j
public class LoginBrowserController {
    private static final String REDIRECT = "redirect:";

    private final ApplicationService applicationService;
    private final LoginService loginService;
    
    @Autowired
    public LoginBrowserController(LoginService loginService, ApplicationService applicationService) {
        this.loginService = loginService;
        this.applicationService = applicationService;
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView init(
            @RequestParam(name="client_id", required = true) String clientId,
            @RequestParam(name="redirect_uri", required = false) String redirectUri, 
            final ModelMap model,
            HttpServletRequest request) {
        
        log.debug("loginPage(clientId:{}, redirectUri: {})", clientId, redirectUri);

        // Generate and store random sentence
        OTS sentence = loginService.init(clientId, ClientType.BROWSER);

        model.addAttribute("redirect_uri", redirectUri);
        
        return new ModelAndView("login", model)
                .addObject("sentence", sentence.getSentence())
                .addObject("loginRequest", new LoginRequest(clientId, sentence.getId(), redirectUri));
    } 
    
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelAndView login(
            @Valid final LoginRequest loginRequest, 
            BindingResult result, final ModelMap model,
            HttpServletResponse response) {
        
        log.debug("login(loginRequest: {})", loginRequest);

        // Check object
        if (result.hasErrors()) {
            throw new ValidationException("validation error");
        }
        
        loginService.login(loginRequest.getClientId(), ClientType.BROWSER, loginRequest, response);

        // Redirect
        if(StringUtils.isEmpty(loginRequest.getRedirectUri())) {
            Client client = this.loginService.getClient(loginRequest.getClientId());
            
            return new ModelAndView(REDIRECT + client.getUrl()); 
        } else {
            return new ModelAndView(REDIRECT + loginRequest.getRedirectUri());
        }
    }
    
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public ModelAndView logout(
            @RequestParam(name="app_id", required = true) String appId,
            @RequestParam(name="client_id", required = true) String clientId,
            @RequestParam(name="redirect_uri", required = false) String redirectUri, HttpServletResponse response, final ModelMap model)  {
        
        Client client = loginService.getClient(clientId);
        
        // Delete cookies
        CookieUtils.deleteCookie(applicationService.getCookie(), response, Constant.COOKIE_TOKEN_NAME);
        CookieUtils.deleteCookie(applicationService.getCookie(), response, Constant.COOKIE_ADDRESS_NAME);
        
        // Redirect
        if(StringUtils.isEmpty(redirectUri)) {
            return new ModelAndView(REDIRECT + client.getUrl()); 
        } else {
            return new ModelAndView(REDIRECT + redirectUri);
        }
    }
    
    @RequestMapping(value = "/success", method = RequestMethod.GET)
    public ModelAndView success(final ModelMap model) {
        
        log.debug("success()");
        
        return new ModelAndView("success", model);
    } 
    
}
