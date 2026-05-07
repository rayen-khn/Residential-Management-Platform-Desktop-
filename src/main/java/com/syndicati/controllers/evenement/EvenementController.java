package com.syndicati.controllers.evenement;

import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.user.User;
import com.syndicati.services.evenement.EvenementService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller facade for evenements.
 */
public class EvenementController {

    private final EvenementService evenementService;

    public EvenementController() {
        this.evenementService = new EvenementService();
    }

    public List<Evenement> evenements() {
        return evenementService.listAll();
    }

    public Optional<Evenement> evenementById(Integer id) {
        return evenementService.findById(id);
    }

    public List<Evenement> evenementsByUser(User user) {
        return evenementService.listByUser(user);
    }

    public List<Evenement> evenementsByType(String type) {
        return evenementService.listByType(type);
    }

    public List<Evenement> evenementsByStatut(String statut) {
        return evenementService.listByStatut(statut);
    }

    public Integer evenementCreate(String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, String type, String image, User user) {
        return evenementService.create(titre, description, dateEvent, lieu, nbPlaces, type, image, user);
    }

    public boolean evenementUpdate(Integer id, String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, String type) {
        return evenementService.update(id, titre, description, dateEvent, lieu, nbPlaces, type);
    }

    public boolean evenementUpdateForDashboard(Integer id, String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, Integer nbRestants, String type, String image) {
        return evenementService.updateForDashboard(id, titre, description, dateEvent, lieu, nbPlaces, nbRestants, type, image);
    }

    public boolean evenementUpdateStatut(Integer id, String statut) {
        return evenementService.updateStatut(id, statut);
    }

    public boolean evenementDelete(Integer id) {
        return evenementService.delete(id);
    }

    public EvenementService getEvenementService() {
        return evenementService;
    }
}
