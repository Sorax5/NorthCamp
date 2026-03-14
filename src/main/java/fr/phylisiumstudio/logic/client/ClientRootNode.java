package fr.phylisiumstudio.logic.client;

import com.badlogic.gdx.ai.btree.branch.Selector;
import com.badlogic.gdx.ai.btree.branch.Sequence;
import fr.phylisiumstudio.logic.client.Action.*;
import fr.phylisiumstudio.logic.client.Action.bored.ChooseAnActivityAction;
import fr.phylisiumstudio.logic.client.Action.bored.DoTheActivity;
import fr.phylisiumstudio.logic.client.Action.chill.JustChillHome;
import fr.phylisiumstudio.logic.client.Action.sleep.SleepTask;
import fr.phylisiumstudio.logic.client.Condition.IsClientStateCondition;
import net.minestom.server.entity.EntityPose;

public class ClientRootNode extends Selector<ClientEntity> {
    public ClientRootNode() {
        // Séquence SLEEPY
        Sequence<ClientEntity> sleepySequence = new Sequence<>();
        sleepySequence.addChild(new IsClientStateCondition(Client.ClientState.SLEEPY));
        sleepySequence.addChild(new GetPlotLocationTask());
        sleepySequence.addChild(new GoToTask());
        sleepySequence.addChild(new SetClientPoseAction(EntityPose.SLEEPING));
        sleepySequence.addChild(new SleepTask());
        sleepySequence.addChild(new UpdateClientState());
        sleepySequence.addChild(new SetClientPoseAction(EntityPose.STANDING));
        addChild(sleepySequence);

        // Séquence BORED
        Sequence<ClientEntity> activitySequence = new Sequence<>();
        activitySequence.addChild(new ChooseAnActivityAction());
        activitySequence.addChild(new GoToTask());
        activitySequence.addChild(new DoTheActivity());
        activitySequence.addChild(new UpdateClientState());

        // Fallback : si l'activité échoue, on force quand même un changement d'état
        Selector<ClientEntity> activityOrFallback = new Selector<>();
        activityOrFallback.addChild(activitySequence);
        activityOrFallback.addChild(new UpdateClientState());

        Sequence<ClientEntity> boredSequence = new Sequence<>();
        boredSequence.addChild(new IsClientStateCondition(Client.ClientState.BORED));
        boredSequence.addChild(activityOrFallback);
        addChild(boredSequence);

        // Séquence CHILL
        Sequence<ClientEntity> chillSequence = new Sequence<>();
        chillSequence.addChild(new IsClientStateCondition(Client.ClientState.CHILL));
        chillSequence.addChild(new GetPlotLocationTask());
        chillSequence.addChild(new GoToTask());
        chillSequence.addChild(new JustChillHome());
        chillSequence.addChild(new UpdateClientState());
        addChild(chillSequence);
    }
}
