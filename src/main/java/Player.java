
import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.collection.immutable.$colon$colon;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Player extends UntypedAbstractActor {
    //private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    String myName;
    int maxRound;
    int totalPlayerAmount;
    private ActorRef[] checkedList;
    private ActorRef[] allPlayersRefList;
    private int score;
    private int currentRound;
    private Map<ActorRef,Integer>  roundMap;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    @Override
    public void preStart() {
        log.info("Player {} started", myName);
    }

    public Player(String myName) {
        this.myName = myName;
        this.score = 0;
        this.roundMap = new HashMap<>();
    }

//    public static Props props() {
//        return Props.create(Player.class);
//    }

//    public static Props props(String name) {
//        return Props.create(Player.class,name);
//    }



//=============Messages===========================V
    // Gesture Start Message
    public static class startMsg {
    public final ActorRef[] allPlayersRef;
    public final int maxRound;
    public final int totalPlayerAmount;

    public startMsg (ActorRef[] allPlayersRef, int numGames, int totalPlayerAmount){
        this.allPlayersRef = allPlayersRef;
        this.maxRound = numGames;
        this.totalPlayerAmount = totalPlayerAmount;
        }
    }
    //Ready Message
    public static class readyMsg {
        public final ActorRef replyTo;
        public final int currentRound;

        public readyMsg (int currentRound,ActorRef replyTo){
            this.currentRound = currentRound;
            this.replyTo = replyTo;

        }
    }
    // Gesture Message
    public static class gestureMsg {
        public final String gesture;

        public gestureMsg (String gesture){
            this.gesture = gesture;
        }
    }

//=============Messages===========================^

//===============do things after received corresponding message====V
    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof startMsg) receiveStartMsg((startMsg) msg);
        else if  (msg instanceof gestureMsg) receiveGestureMsg((gestureMsg) msg);
        else if (msg instanceof readyMsg) receiveReadyMsg((readyMsg) msg);
        else unhandled(msg);
    }


    private void receiveStartMsg(startMsg msg) {

        this.allPlayersRefList = msg.allPlayersRef;
        this.maxRound = msg.maxRound;
        this.totalPlayerAmount = msg.totalPlayerAmount;

        sendMsgToAll(new readyMsg(0,getSelf()));

    }
    private void receiveReadyMsg(readyMsg msg) {

//        System.out.println(" ready"+allPlayersRefList.length);
//        for(int i = 0; i< allPlayersRefList.length;i++){
//            System.out.println(allPlayersRefList[i].toString());
//        }
        // actual work
        if(msg.currentRound == this.currentRound){
            if(this.roundMap.containsKey(msg.replyTo)){
                this.roundMap.replace(msg.replyTo, msg.currentRound, (msg.currentRound)+1);
            }else{
                this.roundMap.put(msg.replyTo,0);
            }
        }
        boolean isEveryOneReady = true;
        for(int i = 0; i < totalPlayerAmount;i++){
            if(roundMap.containsKey(allPlayersRefList[i])&&roundMap.get(allPlayersRefList[i]) != currentRound){
                isEveryOneReady = false;
            }
        }
        if(isEveryOneReady == true){
            log.info("Everyone is ready");

            //sendMsgToAll(new readyMsg(this.currentRound,getSelf()));
        }else {
            log.info("Someone is not ready");
            //sendMsgToAll(new readyMsg(this.currentRound,getSelf()));
        }

    }
    private void receiveGestureMsg(gestureMsg Msg) {
        // actual work

    }
    private void sendMsgToAll(Object msg){
        for(int i = 0; i < allPlayersRefList.length;i++){
            allPlayersRefList[i].tell(msg,getSelf());
        }
    }
//===============do things after received corresponding message====^


}
