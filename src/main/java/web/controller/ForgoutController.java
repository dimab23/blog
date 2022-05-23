package web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import web.model.Users;
import web.repos.UsersRepository;

import java.util.Map;
import java.util.UUID;

@Controller
public class ForgoutController {
    @Autowired
    public JavaMailSender emailSender;

    @Autowired
    public UsersRepository usersRepository;


    @GetMapping("/forgout_password")
    public String forgoutPassword(){
        return "NewPassword";
    }

    @PostMapping("/forgout_password")
    public String forgoutPassword1(Users users, String email, Map<String, Object> model){
        int exiting = usersRepository.exitingEmail(email);
        if( exiting == 0){
                model.put("NotExistUser","This email is not a user. Try to register!");
                return "NewPassword";
            }
        else {
            SimpleMailMessage message = new SimpleMailMessage();
            String code=(UUID.randomUUID().toString());
            users.setActivationCode(code);
            String text = String.format("Am auzit că ți-ai pierdut parola pentru Blog. Îmi pare rău pentru asta!\n" +
                            "Dar nu vă faceți griji! Puteți folosi următorul link pentru a vă reseta parola\n"+
                             "http://idweb.fcim-utm.tech/resetPassword/%s/%s\n\n"+
                            "Cu Drag, Unitatea de dezvoltare Blog.",
                    users.getActivationCode(),
                    users.getEmail()
            );
            message.setFrom("support-blog@fcim-utm.tech");
            message.setTo(email);
            message.setSubject("Reset Password");
            message.setText(text);
            users.setActive(false);
            // Send Message!
            this.emailSender.send(message);
            usersRepository.reset(code,email);
            model.put("messageEroor","Check your email for a link to reset your password. If it doesn’t appear within a few minutes, check your spam folder.");

            return "/login";
        }
    }

    @GetMapping("/resetPassword/{code}/{email}")
    public String RessetPassword(@PathVariable String code, Map<String, Object> model){
        boolean isActivated = activatePassword(code);
        if (isActivated){
           return "ResetPassword";
        }
        else{
            model.put("message","Activation code Password is  not found!");
            return "/login";
        }
    }
    private boolean activatePassword(String code) {
        Users user = usersRepository.findByActivationCode(code);
        if (user == null){
            return false;
        }
        user.setActivationCode(null);
        user.setActive(false);
        usersRepository.save(user);
        return true;
    }
    @PostMapping("/resetPassword/{code}/{email}")
    public String reset(String password, @PathVariable String email, Map<String, Object> model){
        BCryptPasswordEncoder passwordEncoder = new
                BCryptPasswordEncoder();
        String cripPassword = passwordEncoder.encode(password);
        usersRepository.newpassword(cripPassword,email);
        model.put("Password","Password changed successfully!");
        return "/login";
    }
}
