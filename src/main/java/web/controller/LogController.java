package web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import web.model.*;
import web.repos.*;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Controller
public class LogController {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ArticlesRepository articlesRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    public JavaMailSender emailSender;

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/")
    public String blog(Map<String, Object> model, @RequestParam(required = false, defaultValue = "0") String filter,
                       @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, size = 4) Pageable pageable) {
        Page<Articles> articles = articlesRepository.findAllByArticles(pageable);
        List<Articles> articlesdb = articlesRepository.findAllByArticles1(pageable);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth.getName();
        String name_user = usersRepository.name(auth.getName());
        String name_user1 = name_user.replaceFirst(",", "  ");

        Users users = usersRepository.findByEmail(auth.getName());
        int user_id = users.getId();
        List<Notification> notification = notificationRepository.findAllByNotification(user_id);

        int countNotification = notificationRepository.countNofication(user_id);
        int coutByNotification = notificationRepository.countByNotification(user_id);
        model.put("user", name_user1);
        model.put("articles", articles);
        model.put("articlesdb", articlesdb);
        model.put("email", user);
        model.put("notification", notification);
        model.put("countNotification", countNotification);
        model.put("coutByNotification", coutByNotification);
        return "articles";
    }


    @GetMapping("/registration")
    public String registration() {
        return "registration";
    }

    @PostMapping("/registration")
    @Transactional
    public String addUser(Users users, Map<String, Object> model) {
        users.setPassword(passwordEncoder.encode(users.getPassword()));

        Users userFromDb = usersRepository.findByEmail(users.getEmail());
        if (userFromDb != null) {
            model.put("message", "That email exiting! Try to sign in.");
            return "registration";
        } else {

            Date date = new Date();
            users.setDate(date);
            users.setActive(false);
            users.setActivationCode(UUID.randomUUID().toString());
            users.setFirst_name(users.getFirst_name().replaceAll("\\<.*?\\>", ""));
            users.setLast_name(users.getLast_name().replaceAll("\\<.*?\\>", ""));
            users.setEmail(users.getEmail().replaceAll("\\<.*?\\>", ""));
            boolean result = true;
            try {
                InternetAddress emailAddr = new InternetAddress(users.getEmail());
                emailAddr.validate();
            } catch (AddressException ex) {
                result = false;
            }
            if (result) {
                usersRepository.save(users);
                String to = users.getEmail();

                String activationLink = "http://idweb.fcim-utm.tech/activate/" + users.getActivationCode();
                String text = String.format("Salut, %s %s!\n" +
                                "Vă mulțumim pentru crearea blogului dvs.\n" +
                                "Pentru a finaliza înregistrarea, faceți clic pe linkul de mai jos: " + activationLink + "\n" +
                                "Cu Drag, Unitatea de dezvoltare Blog.",
                        users.getFirst_name(),
                        users.getLast_name()

                );
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("support-blog@fcim-utm.tech");
                message.setTo(to);
                message.setSubject("Activation code");
                message.setText(text);
                emailSender.send(message);
            } else {
                model.put("message2", "Incorrect format for email field!");
                return "registration";
            }
        }
        model.put("email1", "A message was sent to the indicated email. Please confirm your email.");
        return "/login";
    }

    @GetMapping("/articles/{id}")
    public String list(@PathVariable Integer id, Map<String, Object> model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth.getName();
        String name_user = usersRepository.name(user);
        String name_user1 = name_user.replaceFirst(",", "  ");
        model.put("user", name_user1);
        List<Articles> a = articlesRepository.findAllByArticlesId(id);
        model.put("articlesdb", a);
        String status = actionRepository.findByStatus(id, user);
        int dbaction = actionRepository.findBy(id, user);
        model.put("status", status);
        model.put("dbaction", dbaction);
        Articles id1 = articlesRepository.findById(id);
        int id_art = id1.getId();
        int like = actionRepository.findByStatuslike(id_art);
        model.put("like", like);
        int dislike = actionRepository.findByStatusdilike(id_art);
        model.put("dislike", dislike);
        Users users = usersRepository.findByEmail(auth.getName());
        int user_id = users.getId();
        int countNotification = notificationRepository.countNofication(user_id);
        int coutByNotification = notificationRepository.countByNotification(user_id);
        model.put("countNotification", countNotification);
        model.put("coutByNotification", coutByNotification);
        List<Notification> notification = notificationRepository.findAllByNotification(user_id);
        model.put("notification", notification);
        //view Comments
        List<Comments> comments = commentsRepository.findAllByComment(id);
        int countComment = commentsRepository.countCommentsBy(id);
        model.put("countComment", countComment);
        model.put("comments", comments);

        return "articles{id}";
    }

    @PostMapping(value = "/articles/{id}", params = "like")
    public String listlike(Articles articles1, @PathVariable Integer id, Map<String, Object> model) {
        //id-articlu
        Articles id1 = articlesRepository.findById(articles1.getId());
        int id_art = id1.getId();
        //id-user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth.getName();
        int dbaction = actionRepository.findBy(id_art, user);
        if (dbaction != 0) {
            String status = actionRepository.findByStatus(id_art, user);
            if (status.equals("like")) {
                actionRepository.deleteActionBy(id_art, user);
                Users users = usersRepository.findByEmail(auth.getName());
                int user_id = users.getId();
                int autorNotification1 = articlesRepository.authoNotification(id_art);
                String s = "She likes your article";
                notificationRepository.deleteNotificationBy(id, user_id, autorNotification1, s);
            } else {

                Users users = usersRepository.findByEmail(auth.getName());
                int userId = users.getId();
                int autorNotification1 = articlesRepository.authoNotification(id_art);
                String s2 = "She likes your article";
                notificationRepository.updateNotification(s2, id, userId, autorNotification1);
                actionRepository.likeByAction(id_art, user);
            }

        } else {

            Articles articles = articlesRepository.findById(id);
            Authentication auth1 = SecurityContextHolder.getContext().getAuthentication();
            Users users = usersRepository.findByEmail(auth1.getName());
            String like = "like";

            Action action1 = new Action(users, articles, like);
            actionRepository.save(action1);
            int autorNotification1 = articlesRepository.authoNotification(id_art);
            Users users3 = usersRepository.findById(autorNotification1);

            Date date = new Date();
            Notification notification = new Notification();

            notification.setArticles(articles);
            notification.setAutorNotification(users3);
            notification.setSeen(false);
            notification.setUsers(users);
            notification.setDate(date);
            notification.setStatus("She likes your article");
            notificationRepository.save(notification);
        }
        return "redirect:/articles/" + id;
    }

    @PostMapping(value = "/articles/{id}", params = "dislike")
    public String listdislike(Articles articles1, @PathVariable Integer id, Map<String, Object> model) {

        Articles id1 = articlesRepository.findById(articles1.getId());
        int id_art = id1.getId();
        //id-user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth.getName();
        int dbaction = actionRepository.findBy(id_art, user);
        if (dbaction != 0) {
            String status = actionRepository.findByStatus(id_art, user);
            if (status.equals("dislike")) {
                actionRepository.deleteActionBy(id_art, user);
                Users users = usersRepository.findByEmail(auth.getName());
                int user_id = users.getId();
                int autorNotification1 = articlesRepository.authoNotification(id_art);
                String s = "He does not like your article";
                notificationRepository.deleteNotificationBy(id, user_id, autorNotification1, s);
            } else {
                Users users = usersRepository.findByEmail(auth.getName());
                int userId = users.getId();
                int autorNotification1 = articlesRepository.authoNotification(id_art);
                String s = ("He does not like your article");
                notificationRepository.updateNotification(s, id, userId, autorNotification1);
                actionRepository.dislikeByAction(id_art, user);

            }

        } else {
            Articles articles = articlesRepository.findById(articles1.getId());
            Authentication auth1 = SecurityContextHolder.getContext().getAuthentication();
            Users users = usersRepository.findByEmail(auth1.getName());
            String like = "dislike";
            Action action1 = new Action(users, articles, like);
            actionRepository.save(action1);
            int authorNotification1 = articlesRepository.authoNotification(id_art);
            Users users3 = usersRepository.findById(authorNotification1);

            Date date = new Date();
            Notification notification = new Notification();

            notification.setArticles(articles);
            notification.setAutorNotification(users3);
            notification.setSeen(false);
            notification.setUsers(users);
            notification.setDate(date);
            notification.setStatus("He does not like your article");
            notificationRepository.save(notification);
        }

        return "redirect:/articles/" + id;
    }

    @GetMapping("/activate/{code}")
    public String activate(@PathVariable String code, Map<String, Object> model) {
        boolean isActivated = activateUser(code);
        if (isActivated) {
            model.put("message", "Succes activation account!!!");
        } else {
            model.put("message", "Activation code is not found or account is active!");
        }
        return "/login";
    }

    private boolean activateUser(String code) {
        Users user = usersRepository.findByActivationCode(code);
        if (user == null) {
            return false;
        }
        user.setActivationCode(null);
        user.setActive(true);
        usersRepository.save(user);
        return true;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        String errorMessge = null;
        if (error != null) {
            errorMessge = "Username or Password is incorrect !!";
        }
        if (logout != null) {
            errorMessge = "You have been successfully logged out !!";
        }
        model.addAttribute("errorMessge", errorMessge);
        return "login";
    }
}
