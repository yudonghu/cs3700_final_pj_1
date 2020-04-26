
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
import java.util.LinkedList;
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
    private HashMap<ActorRef,Integer>  roundMap;
    private HashMap<ActorRef,String>  gestureMap;
    private String myGesture;
    private boolean newGestureMade;
    private boolean resultPrinted;
    private String myState;
    //"gameNotStarted" = after this player create, but have not receive start message
    // "ready" =  1. game started, before first round 2.one round finished, current round score gotten
    // "playingWithOthers"  = after all players are ready
    // "computingScore" = after all players gesture gotten
    // "gameFinished" = after max round reached

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public void preStart() {
        log.info("Player {} started", myName);
    }

    public Player(String myName) {
        this.myName = myName;
        this.score = 0;
        this.roundMap = new HashMap();
        this.gestureMap = new HashMap();
        this.newGestureMade = false;
        this.resultPrinted = false;
        //this.roundMap.put(this.getSelf(),0);
    }

//    public static Props props() {
//        return Props.create(Player.class);
//    }

//    public static Props props(String name) {
//        return Props.create(Player.class,name);
//    }



//=============Messages===========================V
    // Gesture Start Message
    public static class systemStartMsg {
    public final ActorRef[] allPlayersRef;
    public final int maxRound;
    public final int totalPlayerAmount;

    public systemStartMsg (ActorRef[] allPlayersRef, int totalPlayerAmount, int numGames){
        this.allPlayersRef = allPlayersRef;
        this.maxRound = numGames;
        this.totalPlayerAmount = totalPlayerAmount;
        }
    }
    //Ready Message
    public static class playerMsg {
        public final String state;
        public final String name;
        public final String gesture;
        public final int currentRound;
        public final ActorRef replyTo;


        public playerMsg (String state,String name, int currentRound,String gesture,ActorRef replyTo){
            this.state = state;
            this.name = name;
            this.currentRound = currentRound;
            this.gesture = gesture;
            this.replyTo = replyTo;

        }
    }
    // Gesture Message
//    public static class gestureMsg {
//        public final ActorRef replyTo;
//        public final String gesture;
//
//        public gestureMsg (ActorRef replyTo){
//            this.gesture = gesture;
//            this.replyTo = replyTo;
//        }
//    }

//=============Messages===========================^

//===============do things after received corresponding message====V
    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof systemStartMsg) receiveStartMsg((systemStartMsg) msg);
//        else if  (msg instanceof gestureMsg) receiveGestureMsg((gestureMsg) msg);
        else if (msg instanceof playerMsg) receivePlayerMsg((playerMsg) msg);
        else unhandled(msg);
    }


    private void receiveStartMsg(systemStartMsg msg) {

        this.allPlayersRefList = msg.allPlayersRef;
        this.maxRound = msg.maxRound;
        this.totalPlayerAmount = msg.totalPlayerAmount;
        this.myState = "ready";
        sendMsgToAll(new playerMsg(this.myState,this.myName,0,null,getSelf()));

    }
    private void receivePlayerMsg(playerMsg msg) throws InterruptedException {
//test==========================================
//        System.out.println(" ready"+allPlayersRefList.length);
//        for(int i = 0; i< allPlayersRefList.length;i++){
//            System.out.println(allPlayersRefList[i].toString());
//        }
     //     System.out.println(this.roundMap.containsKey(allPlayersRefList[1]) );
//test==========================================
        // actual work





        // I am in ready state, check if is everyone else ready.


        log.info("message from "+msg.name+" gotten : player "+this.myName+" my state: " + this.myState);

        if(myState.equals("ready")){

            //check whether opponent is in my Round Map or not
            if(!(this.roundMap.containsKey(msg.replyTo))){ //if opponent is not on the map
                // add new player to map, and set his round number to 0
                this.roundMap.put(msg.replyTo,0);
            }else{ //if opponent is one the map
                //update his round number
                this.roundMap.replace(msg.replyTo,msg.currentRound);
            }

            if(!(msg.state.equals("ready"))){//check is this sender opponent ready
                //send a message to opponent to tell "I am ready"
                msg.replyTo.tell(new playerMsg(this.myState,this.myName,this.currentRound,null,this.getSelf()),this.getSelf());
                //print who's not ready
                log.info("Player "+this.myName+": Player "+msg.name+" one is not in ready state");
            }else{
                //go through all player's in map
                for(int i = 0; i < allPlayersRefList.length;i++){
                    //check 1: if this one is on the map
                    //check 2: if this one has the different round number with me
                    if(roundMap.containsKey(allPlayersRefList[i])&&roundMap.get(allPlayersRefList[i]) != currentRound){
                        //send a message to this person to tell "I am ready"
                        msg.replyTo.tell(new playerMsg(this.myState,this.myName,this.currentRound,null,this.getSelf()),this.getSelf());
                        //print who's not ready
                        log.info("Player "+this.myName+": Player "+this.myName+" is not ready, his round number:"+msg.currentRound);
                    }
                }
            }
            //every one is ready and change state to "playingWithOthers"

            this.myState = "playingWithOthers";

            resultPrinted = false;
            if(newGestureMade == false){
                pickGesture();
                newGestureMade = true;
                System.out.println(this.myGesture);
                sendMsgToAll(new playerMsg(this.myState,this.myName,this.currentRound,this.myGesture,this.getSelf()));
            }
            //to ensure everyone start at same time. thread sleeps for 1 sec
            Thread.sleep(1);
        }



System.out.println(myState +" "+msg.state);
        if(myState.equals("playingWithOthers") && (msg.state.equals("playingWithOthers")) ){
            if(!(gestureMap.containsKey(msg.replyTo))){
                gestureMap.put(msg.replyTo,msg.gesture);
//                log.info(" Gesture is "+msg.gesture);
            }



            if(this.gestureMap.size() == allPlayersRefList.length){
                this.myState = "computingScore";
                log.info("Player "+this.myName+": Map full");
                int winningCount = 0;
                for(int i =0;i<allPlayersRefList.length;i++){
                    if(allPlayersRefList[i] != getSelf()){
//
//                        log.info(  allPlayersRefList[i].toString() );
//                        log.info(  gestureMap.get(allPlayersRefList[i])+" "+this.gestureMap.size()+" "+gestureMap.size() );
//                        log.info(  gestureMap.get(allPlayersRefList[i])+" "+this.gestureMap.size()+" "+gestureMap.size() );
                        if(gestureMap.get(allPlayersRefList[i]).equals(this.myGesture)){//tie

                        }else if( (gestureMap.get(allPlayersRefList[i]).equals("rock") ) && this.myGesture.equals("scissors")||
                                (gestureMap.get(allPlayersRefList[i]).equals("scissors") ) && this.myGesture.equals("paper")||
                                (gestureMap.get(allPlayersRefList[i]).equals("paper") ) && this.myGesture.equals("rock")
                        ){//lose
                            winningCount--;
                        }else{//win
                            winningCount++;
                        }
                    }
                }
                if(winningCount > 0){
                    this.score += winningCount;
                }
            }

        }

        if(myState.equals("computingScore") ){
            if(resultPrinted == false){
                log.info("Round "+this.currentRound+",Player "+this.myName+": current score:" + this.score);
                resultPrinted = true;
                this.currentRound++;
                this.myState = "ready";
                newGestureMade = false;
                roundMap.replace(getSelf(),this.currentRound);
                sendMsgToAll(new playerMsg(this.myState,this.myName,this.currentRound,this.myGesture,getSelf()));



            }
            Thread.sleep(1000);

        }

        if(currentRound == maxRound){
            log.info(currentRound+" "+maxRound);
            myState = "gameFinished";
            Thread.sleep(10000);
        }

//        if(myState.equals("gameFinished")){
//            if(!(msg.state.equals("gameFinished"))){
//
//                log.info("Player "+msg.name+" is not finished");
//            }else{
//                for(int i = 0;i<allPlayersRefList.length;i++){
//                    if( roundMap.get(allPlayersRefList[i]) == maxRound){
//
//                    }else{
//                        log.info("Someone is not finished");
//                    }
//
//                }
//                log.info("Everyone is finished");
//            }
//
//
//        }
        msg.replyTo.tell(new playerMsg(this.myState,this.myName,this.currentRound,this.myGesture,this.getSelf()),getSelf());



//        if(isEveryOneReady == true){
//            log.info("Everyone is ready" + this.myState);
//
//            Thread.sleep(1000);
//            getSelf().tell(new playerMsg(this.myState,this.myName,this.currentRound,null,this.getSelf()),getSelf());
//            if(newGestureMade == false){
//                pickGesture();
//                newGestureMade = true;
//            }
//
////            sendMsgToAll(new gestureMsg(this.myGesture, this.getSelf() ));
//        }else {
//            log.info("Someone is not ready");
////            sendMsgToAll(new readyMsg(this.currentRound,this.getSelf()));
//        }

    }
//    private void receiveGestureMsg(gestureMsg Msg) {
//        if(Msg.replyTo == this.getSelf()){
//            log.info(" My gesture is " + Msg.gesture);
//
//        }else{
//            log.info("Player "+this.myName+": Player "+Msg.replyTo+"'s gesture is " + Msg.gesture);
//        }
//
//
//    }
    //===============do things after received corresponding message====^

    //===============other methods==============================v
    private void sendMsgToAll(Object msg){
//        getSelf().tell(msg,getSelf());
        for(int i = 0; i < allPlayersRefList.length;i++){
            allPlayersRefList[i].tell(msg,ActorRef.noSender());
        }
    }
    //check if new Gesture has made in this round or not, then make new gesture.


    private void pickGesture(){
        Random rand = new Random();
        int tempInt = rand.nextInt(3);
        if(tempInt == 0){
            this.myGesture = "rock";
        } else if(tempInt == 1){
            this.myGesture = "scissors";
        }else{
            this.myGesture = "paper";
        }
    }

    //===============other methods============================^

}
