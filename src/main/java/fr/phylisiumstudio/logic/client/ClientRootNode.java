package fr.phylisiumstudio.logic.client;

import com.badlogic.gdx.ai.btree.branch.Selector;
import com.badlogic.gdx.ai.btree.branch.Sequence;
import fr.phylisiumstudio.logic.client.Action.*;
import fr.phylisiumstudio.logic.client.Action.bored.ChooseAnActivityAction;
import fr.phylisiumstudio.logic.client.Action.bored.DoTheActivity;
import fr.phylisiumstudio.logic.client.Action.chill.JustChillHome;
import fr.phylisiumstudio.logic.client.Action.sleep.SleepTask;
import fr.phylisiumstudio.logic.client.Condition.IsClientStateCondition;
import fr.phylisiumstudio.logic.client.Condition.IsLifecycleCondition;
import net.minestom.server.entity.EntityPose;

import java.time.Duration;

public class ClientRootNode extends Selector<ClientEntity> {
    public ClientRootNode() {
        // ── Départ : le client en fin de séjour rejoint la sortie puis disparaît.
        Sequence<ClientEntity> leavingSequence = new Sequence<>();
        leavingSequence.addChild(new IsLifecycleCondition(ClientLifecycle.LEAVING));
        leavingSequence.addChild(new SetTargetAction(ClientMemory::getExitPosition));
        leavingSequence.addChild(new GoToTask());
        leavingSequence.addChild(new DespawnTask());
        addChild(leavingSequence);

        // ── Attente : le client sans emplacement patiente à l'accueil.
        Sequence<ClientEntity> waitingSequence = new Sequence<>();
        waitingSequence.addChild(new IsLifecycleCondition(ClientLifecycle.WAITING));
        waitingSequence.addChild(new SetTargetAction(ClientMemory::getReceptionPosition));
        waitingSequence.addChild(new GoToTask());
        waitingSequence.addChild(new IdleTask("Waiting at reception", Duration.ofSeconds(5)));
        addChild(waitingSequence);

        // ── Séjour : routine SLEEPY.
        Sequence<ClientEntity> sleepySequence = new Sequence<>();
        sleepySequence.addChild(new IsClientStateCondition(Client.ClientState.SLEEPY));
        sleepySequence.addChild(new GetPlotLocationTask());
        sleepySequence.addChild(new GoToTask());
        sleepySequence.addChild(new SetClientPoseAction(EntityPose.SLEEPING));
        sleepySequence.addChild(new SleepTask());
        sleepySequence.addChild(new UpdateClientState());
        sleepySequence.addChild(new SetClientPoseAction(EntityPose.STANDING));
        addChild(sleepySequence);

        // ── Séjour : routine BORED (activités).
        Sequence<ClientEntity> activitySequence = new Sequence<>();
        activitySequence.addChild(new ChooseAnActivityAction());
        activitySequence.addChild(new GoToTask());
        activitySequence.addChild(new DoTheActivity());
        activitySequence.addChild(new UpdateClientState());

        Selector<ClientEntity> activityOrFallback = new Selector<>();
        activityOrFallback.addChild(activitySequence);
        activityOrFallback.addChild(new UpdateClientState());

        Sequence<ClientEntity> boredSequence = new Sequence<>();
        boredSequence.addChild(new IsClientStateCondition(Client.ClientState.BORED));
        boredSequence.addChild(activityOrFallback);
        addChild(boredSequence);

        // ── Séjour : routine CHILL.
        Sequence<ClientEntity> chillSequence = new Sequence<>();
        chillSequence.addChild(new IsClientStateCondition(Client.ClientState.CHILL));
        chillSequence.addChild(new GetPlotLocationTask());
        chillSequence.addChild(new GoToTask());
        chillSequence.addChild(new JustChillHome());
        chillSequence.addChild(new UpdateClientState());
        addChild(chillSequence);
    }
}
