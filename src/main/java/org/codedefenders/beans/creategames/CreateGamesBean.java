package org.codedefenders.beans.creategames;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codedefenders.model.UserInfo;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.service.CreateGamesService;

@RequestScoped
public class CreateGamesBean {

    protected final CreateGamesService createGamesService;

    protected StagedGameList stagedGameList;
    protected Map<Integer, UserInfo> userInfos;

    @Inject
    public CreateGamesBean(CreateGamesService createGamesService) {
        this.createGamesService = createGamesService;
    }
}
