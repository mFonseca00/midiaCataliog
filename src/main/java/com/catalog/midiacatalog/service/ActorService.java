package com.catalog.midiacatalog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.catalog.midiacatalog.dto.Actor.ActorDTO;
import com.catalog.midiacatalog.dto.Actor.ActorRegistrationDTO;
import com.catalog.midiacatalog.dto.Actor.ActorResponseDTO;
import com.catalog.midiacatalog.dto.Actor.ActorUpdateDTO;
import com.catalog.midiacatalog.dto.Midia.MidiaDTO;
import com.catalog.midiacatalog.exception.DataNotFoundException;
import com.catalog.midiacatalog.exception.DataValidationException;
import com.catalog.midiacatalog.model.Actor;
import com.catalog.midiacatalog.model.Midia;
import com.catalog.midiacatalog.repository.ActorRepository;
import com.catalog.midiacatalog.repository.MidiaRepository;

@Service
public class ActorService {
    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private MidiaRepository midiaRepository;

    public ActorResponseDTO register(ActorRegistrationDTO actorDTO) {
        if(actorDTO == null)
            throw new DataValidationException("Actor data must be informed.");
            
        List<String> errors = new ArrayList<>();
        
        if(actorDTO.getName() == null || actorDTO.getName().trim().isEmpty())
            errors.add("Actor name must be informed.");
            
        if(actorDTO.getBirthDate() == null)
            errors.add("Birth date must be informed.");
        else if(actorDTO.getBirthDate().isAfter(java.time.LocalDate.now()))
            errors.add("Birth date cannot be in the future.");
            
        if(!errors.isEmpty())
            throw new DataValidationException(errors);
        
        Actor actor = new Actor();
        actor.setName(actorDTO.getName());
        actor.setBirthDate(actorDTO.getBirthDate());
        actor.setEnabled(true);
        actorRepository.save(actor);

        return new ActorResponseDTO(actor.getId(), actor.getName(), actor.getBirthDate());
    }

    public ActorResponseDTO remove(Long id) {
        validateId(id, "Actor");
        
        Actor actor = findActorById(id);
        actorRepository.deleteById(id);

        return new ActorResponseDTO(actor.getId(), actor.getName(), actor.getBirthDate());
    }

    public ActorResponseDTO update(Long id, ActorUpdateDTO actorInfo) {
        List<String> errors = new ArrayList<>();

        if(id == null)
            errors.add("Actor Id must be informed."); 
        if(actorInfo == null)
            errors.add("Actor Informations can't be null.");
    
        if(actorInfo != null && actorInfo.getBirthDate() != null &&
           actorInfo.getBirthDate().isAfter(java.time.LocalDate.now()))
            errors.add("Birth date cannot be in the future.");

        if(!errors.isEmpty())
            throw new DataValidationException(errors);
        
        Actor actor = findActorById(id);
        if(actorInfo.getName() != null && !actorInfo.getName().isBlank())
            actor.setName(actorInfo.getName());
        if(actorInfo.getBirthDate() != null)
            actor.setBirthDate(actorInfo.getBirthDate());
        
        actorRepository.save(actor);
        return new ActorResponseDTO(actor.getId(),actor.getName(),actor.getBirthDate());
    }

    public ActorDTO getActor(Long id) {
        validateId(id, "Actor");
        Actor actor = findActorById(id);
        
        return new ActorDTO(
            actor.getId(),
            actor.getName(),
            actor.getBirthDate(),
            actor.getMidias());
    }

    public Page<ActorDTO> getAllActors(Pageable pageable) {
        Page<Actor> actors = actorRepository.findAll(pageable);
        
        if(actors.isEmpty())
            throw new DataNotFoundException("No actors found in database.");

        return actors.map(actor -> new ActorDTO(
                actor.getId(), 
                actor.getName(), 
                actor.getBirthDate(), 
                actor.getMidias()));
    }

    public String addMidia(Long actorId, Long midiaId) {
        validateIds(actorId, midiaId);
        
        Actor actor = findActorById(actorId);
        Midia midia = findMidiaById(midiaId);

        List<Midia> actorMidias = actor.getMidias();
        if(actorMidias == null) {
            actorMidias = new ArrayList<>();
            actor.setMidias(actorMidias);
        }
        
        actorMidias.add(midia);
        actorRepository.save(actor);

        return "Midia added successfully";
    }

    public MidiaDTO removeMidia(Long actorId, Long midiaId) {
        validateIds(actorId, midiaId);
        
        Actor actor = findActorById(actorId);
        List<Midia> actorMidias = actor.getMidias();

        if(actorMidias == null || actorMidias.isEmpty())
            throw new DataNotFoundException("No midias found for this actor.");

        Midia midiaToRemove = actorMidias.stream()
            .filter(midia -> midia.getId().equals(midiaId))
            .findFirst()
            .orElseThrow(() -> new DataNotFoundException("Midia not found for this actor."));

        actorMidias.remove(midiaToRemove);
        actorRepository.save(actor);

        return convertToMidiaDTO(midiaToRemove);
    }

    public Page<MidiaDTO> getAllActorMidias(Long actorId, Pageable pageable) {
        validateId(actorId, "Actor");
        
        Actor actor = findActorById(actorId);
        List<Midia> midias = actor.getMidias();
        
        if(midias == null || midias.isEmpty())
            throw new DataNotFoundException("No midias found for this actor.");

        List<MidiaDTO> midiaDTOs = midias.stream()
            .map(this::convertToMidiaDTO)
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), midiaDTOs.size());
        
        List<MidiaDTO> pageContent = midiaDTOs.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, midiaDTOs.size());
    }


    // Helper methods
    private void validateId(Long id, String entity) {
        if(id == null)
            throw new DataValidationException(entity + " id must be informed.");
    }

    private void validateIds(Long actorId, Long midiaId) {
        validateId(actorId, "Actor");
        validateId(midiaId, "Midia");
    }

    private Actor findActorById(Long id) {
        return actorRepository.findById(id)
            .orElseThrow(() -> new DataNotFoundException("Actor not found."));
    }

    private Midia findMidiaById(Long id) {
        return midiaRepository.findById(id)
            .orElseThrow(() -> new DataNotFoundException("Midia not found."));
    }

    private MidiaDTO convertToMidiaDTO(Midia midia) {
        return new MidiaDTO(
            midia.getId(),
            midia.getTitle(),
            midia.getType(),
            midia.getReleaseYear(),
            midia.getDirector(),
            midia.getSynopsis(),
            midia.getGenre(),
            midia.getPoseterImageUrl(),
            midia.getActors()
        );
    }

}
