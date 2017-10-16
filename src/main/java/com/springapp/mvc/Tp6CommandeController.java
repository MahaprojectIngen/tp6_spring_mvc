package com.springapp.mvc;

import com.springapp.beans.Client;
import com.springapp.beans.Commande;
import com.springapp.dao.ClientDao;
import com.springapp.dao.CommandeDao;
import com.springapp.dao.DAOException;
import com.springapp.forms.FormValidationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HttpServletBean;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.*;
import java.io.IOException;
import java.util.*;

/**
 * Created by patrick on 2017/09/30.
 */
@Controller
public class Tp6CommandeController extends HttpServletBean {
    private DataBinder validator;

    private Log log = LogFactory.getLog(Tp6ClientController.class);

    @Autowired
    private ClientDao clientDao;

    @Autowired
    private CommandeDao commandeDao;

    /*commande  handlers*/
    @RequestMapping(value = "/creationCommande")
    public String creationCommande(Model model, HttpServletRequest  request) {
        log.debug("creation commande call");
         model.addAttribute("commande", new Commande());
        return "creerCommande";
    }

    @RequestMapping(value = "/afficherCommande", method = RequestMethod.POST)
    public String afficherCommande(@Valid @ModelAttribute(value = "commande") Commande commande,BindingResult bindingResult, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes,Model model) throws FormValidationException, InstantiationException, IllegalAccessException, ServletException, IOException {
         Client client;
         Tp6ClientController tp6ClientController = new Tp6ClientController();
         boolean email_field_errors = bindingResult.hasFieldErrors("email");
         FieldError fieldError = null;
         Object  fieldTarget = bindingResult.getTarget();
         Class<?> field = fieldTarget.getClass();
         HttpSession session = null;
         String targetStr= field.getCanonicalName();
         System.out.println("classe : " + targetStr);
        //récup du choix dans le radio bouton
          /*
           * Si l'utilisateur choisit un client déjà existant, pas de validation à
           * effectuer
           */
        String choixNouveauClient = httpServletRequest.getParameter("choixNouveauClient");
        if ("ancienClient".equals(choixNouveauClient)) {
            /* Récupération de l'id du client choisi */
            String idAncienClient = httpServletRequest.getParameter("listeClients");
            Long id ;
                try {
                    id = Long.parseLong(idAncienClient);
                } catch (NumberFormatException e) {
                    e.getLocalizedMessage();
                    id = 0L;
                }
             /* Récupération de l'objet client correspondant dans la session */
            System.out.println("id du client en session :" + id);
            session = httpServletRequest.getSession();
            if(id != 0) {
                client = ((Map<Long, Client>) session.getAttribute("clients")).get(id);
                commande.setClient(client);
                client.setId(id);
                client.setNom(client.getNom());
                client.setPrenom(client.getPrenom());
                client.setAdresse(client.getAdresse());
                client.setEmail(client.getEmail());
                client.setImages(client.getImages());
            }else {
                client = new Client();
                id = 0L;
            }

            ValidatorFactory  validatorFactory = Validation.buildDefaultValidatorFactory();
            Validator  validator = validatorFactory.getValidator();
            Set<ConstraintViolation<Client>>  client_constraintViolations = validator.validate(client);
            Set<ConstraintViolation<Commande>>  commande_constraintViolations = validator.validate(commande);
            /*=========================TRAITEMENT DU COMMANDE=======================*/

                if (client_constraintViolations.size() > 0 && commande_constraintViolations.size() > 0 ){
                    return "creerCommande";
                }else if (client_constraintViolations.size() > 0  && commande_constraintViolations.size() == 0){
                    return "creerCommande";
                }else if (client_constraintViolations.size() == 0  && commande_constraintViolations.size() > 0){
                    return "creerCommande";
                }
                session = httpServletRequest.getSession();
                Map<Long, Client> clients = (HashMap<Long, Client>) session.getAttribute("clients");
                if (clients == null) {
                    List<Client> listeClients = clientDao.lister();
                    clients = new HashMap<Long, Client>();
                    for (Client client1 : listeClients) {
                        clients.put(client1.getId(), client1);
                    }
                    session.setAttribute("clients", clients);
                }else{
                  /* Ensuite récupération de la map des commandes dans la session */
                Map<Long, Commande> commandes = (HashMap<Long,
                        Commande>) session.getAttribute("commandes");
                 /* Si aucune map n'existe, alors initialisation d'une nouvelle map */
                if (commandes == null) {
                    List<Commande> listCommande = commandeDao.findAll();
                    commandes = new HashMap<Long, Commande>();
                    for (Commande commande1 : listCommande) {
                    /* Puis ajout de la commande courante dans la map */
                        commandes.put(commande1.getId(), commande1);
                    }
                    /* Et enfin (ré)enregistrement de la map en session */
                    session.setAttribute("commandes", commandes);
                }
                System.out.println("email after set :" + commande.getClient().getEmail());
                System.out.println("target :"+bindingResult.hasFieldErrors());
                    commandeDao.creer(commande);

                /*modifier les valeurs des données du commande*/
                    commande.setId(commande.getId());
                    commande.setClient(client);
                    commande.setMontant(commande.getMontant());
                    commande.setModeLivraison(commande.getModeLivraison());
                    commande.setModePaiement(commande.getModePaiement());
                    commande.setStatutPaiement(commande.getStatutPaiement());
                    commande.setStatutLivraison(commande.getStatutLivraison());
                /**/
                /* Et enfin (ré)enregistrement de la map en session */
                    session.setAttribute("clients", clients);
                    session.setAttribute("commandes", commandes);
                    commandes.put(commande.getId(), commande);
                    System.out.println("succès de la creation !" + " commandes date :" + commande.getDate());
                /*insertion des information concernants le client dans la base après validation*/
                    model.addAttribute("client", client);
                    model.addAttribute("commande", commande);
                    return "afficherCommande";
                }
            /*======================================================================*/
        } else {
            //
            client = commande.getClient();
            String email = client.getEmail();
            System.out.println("client in: " + client);
            System.out.println("email in: " + email);
            //validation clients
            MultipartFile files = client.getImages();
            List<String> fileNames = new ArrayList<String>();
            String imageurl = tp6ClientController.validationImages(files, fileNames, httpServletRequest, client, session);
           // tp6ClientController.clientValidation(bindingResult, email_field_errors, email, clientDao, fieldError);
            ValidatorFactory  validatorFactory = Validation.buildDefaultValidatorFactory();
            Validator  validator = validatorFactory.getValidator();
            Set<ConstraintViolation<Client>>  client_constraintViolations = validator.validate(client);
            Set<ConstraintViolation<Commande>>  commande_constraintViolations = validator.validate(commande);

            if (client_constraintViolations.size() > 0 && commande_constraintViolations.size() > 0 ){

                return "creerCommande";
            }else if (client_constraintViolations.size() > 0  && commande_constraintViolations.size() == 0){

                return "creerCommande";
            }else if (client_constraintViolations.size() == 0  && commande_constraintViolations.size() > 0){

                return "creerCommande";
                //end of the new validation //
            } else {

                 /*modification des valeurs de donnée du client*/
                client.setId(commande.getClient().getId());
                client.setNom(commande.getClient().getNom());
                client.setPrenom(commande.getClient().getPrenom());
                client.setAdresse(commande.getClient().getAdresse());
                client.setEmail(commande.getClient().getEmail());
                client.setImages(commande.getClient().getImages());
                client.setNomImage(commande.getClient().getNomImage());
                    /**/
                clientDao.creer(client, imageurl);

                session = httpServletRequest.getSession();
                Map<Long, Client> clients = (HashMap<Long, Client>) session.getAttribute("clients");
                if (clients == null) {
                   List<Client> listeClients = clientDao.lister();
                   clients = new HashMap<Long, Client>();
                for (Client client1 : listeClients) {
                    clients.put(client1.getId(), client1);
                }
                    session.setAttribute("clients", clients);
                }

                  /* Ensuite récupération de la map des commandes dans la session */
                Map<Long, Commande> commandes = (HashMap<Long, Commande>) session.getAttribute("commandes");
                 /* Si aucune map n'existe, alors initialisation d'une nouvelle map */
                if (commandes == null) {
                List<Commande> listCommande = commandeDao.lister();
                commandes = new HashMap<Long, Commande>();
                for (Commande commande1 : listCommande) {
                    /* Puis ajout de la commande courante dans la map */
                    commandes.put(commande1.getId(), commande1);
                }
                    /* Et enfin (ré)enregistrement de la map en session */
                session.setAttribute("commandes", commandes);
            }
                System.out.println("email after set :" + commande.getClient().getEmail());
                boolean  isNotNull = bindingResult.getTarget().equals(commande);
                System.out.println("target :" + isNotNull);
                commandeDao.creer(commande);

                /*modifier les valeurs des données du commande*/
                    commande.setId(commande.getId());
                    commande.setClient(commande.getClient());
                    commande.setMontant(commande.getMontant());
                    commande.setModeLivraison(commande.getModeLivraison());
                    commande.setModePaiement(commande.getModePaiement());
                    commande.setStatutPaiement(commande.getStatutPaiement());
                    commande.setStatutLivraison(commande.getStatutLivraison());
                /**/
                /* Et enfin (ré)enregistrement de la map en session */
                    session.setAttribute("clients", clients);
                    session.setAttribute("commandes", commandes);
                    commandes.put(commande.getId(), commande);
                    System.out.println("succès de la creation !" + " commandes date :" + commande.getDate());
                    /*insertion des information concernants le client dans la base après validation*/
                    model.addAttribute("client", client);
                    model.addAttribute("commande", commande);
                    return "afficherCommande";
                }
            }
         return "afficherCommande";
        }

    @RequestMapping(value = "/listerCommande")
    public String listerCommandes(HttpServletRequest  httpServletRequest, Model model){

        return "listerCommandes";
    }

    @RequestMapping(value = "/suppressionCommande")
    public String supprimerCommande(HttpServletRequest  httpServletRequest, Model  model){
        HttpSession  session = httpServletRequest.getSession();
        Map<Long, Commande>  commandes  = (HashMap<Long, Commande>)session.getAttribute("commandes");
        String  idCommande = httpServletRequest.getParameter("id");
        Long  id = null;
        try {
            id = Long.parseLong(idCommande);
        }catch (Exception e){
            e.getLocalizedMessage();
        }

        if ( commandes != null){
            //for(Map.Entry<Long, Client> clientHashMap : clients.entrySet()) {
            // id = clientHashMap.getKey();
            System.out.println("id value: "+ id);
            try {
                /*Alors suppression du client de la BDD */
                commandeDao.supprimer(commandes.get(id));
                /*Puis suppression du client de la Map */
                commandes.remove(id);
            } catch (DAOException e) {
                e.printStackTrace();
            }
            /*Remplacement de l'ancienne Map en session par la nouvelle*/
            session.setAttribute("commandes", commandes);
            model.addAttribute("commandes", commandes);

            return "redirect:/listerCommande";
        }
        return  "listerCommande";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
    public @ResponseBody List handleValidationFailure(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getAllErrors();
    }
}

