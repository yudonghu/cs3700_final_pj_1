import akka.actor.UntypedAbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.HashMap;
import java.util.Random;

public class Player extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    String myName;
    int maxRound;
    int totalPlayerAmount;
    private ActorRef[] allPlayersRefList;
    private int score;
    private int currentRound;
    private HashMap<ActorRef,Integer>  roundMap;
    private HashMap<ActorRef,String>  gestureMap;
    private String myGesture;
    private boolean newGestureMade;
    private boolean resultPrinted;
    private String myState;
    private ActorRef systemRef;
    private boolean scorePrinted = false;
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
    }
//=============Messages===========================V
    // Gesture Start Message
    public static class systemStartMsg {
    public final ActorRef[] allPlayersRef;
    public final int maxRound;
    public final int totalPlayerAmount;
    public final ActorRef replyTo;

    public systemStartMsg (ActorRef[] allPlayersRef, int totalPlayerAmount, int numGames,ActorRef replyTo){//
        this.allPlayersRef = allPlayersRef;
        this.maxRound = numGames;
        this.totalPlayerAmount = totalPlayerAmount;
        this.replyTo = replyTo;
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

//=============Messages===========================^

//===============do things after received corresponding message====V
    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof systemStartMsg) receiveStartMsg((systemStartMsg) msg);
        else if (msg instanceof playerMsg) receivePlayerMsg((playerMsg) msg);
        else unhandled(msg);
    }

    private void receiveStartMsg(systemStartMsg msg) {
        this.allPlayersRefList = msg.allPlayersRef;
        this.maxRound = msg.maxRound;
        this.totalPlayerAmount = msg.totalPlayerAmount;
        this.myState = "ready";
        this.systemRef = msg.replyTo;

        sendMsgToAll(new playerMsg(this.myState,this.myName,0,null,getSelf()));
    }

    private void receivePlayerMsg(playerMsg msg) throws InterruptedException {
        //log.info(this.myName+ " "+myState+ " round:"+ this.currentRound);
        Thread.sleep(50);
        boolean isEveryoneReady = true;
        if(myState.equals("ready")&&(this.currentRound <= msg.currentRound )){//&&msg.state.equals("ready")
            //check whether opponent is in my Round Map or not
            if(!(this.roundMap.containsKey(msg.replyTo))){ //if opponent is not on the map
                // add new player to map, and set his round number to 0
                this.roundMap.put(msg.replyTo,0);
            }else{ //if opponent is on the map
                //update his round number
                this.roundMap.replace(msg.replyTo,msg.currentRound);
            }
            if(!(msg.state.equals("ready"))){//check is this sender opponent ready
                //send a message to opponent to tell "I am ready"
                //msg.replyTo.tell(new playerMsg(this.myState,this.myName,this.currentRound,null,this.getSelf()),this.getSelf());
                //print who's not ready
                //          log.info("Player "+this.myName+": Player "+msg.name+" one is not in ready state");
                isEveryoneReady = false;
            }else {
                //go through all player's in map
                for (int i = 0; i < allPlayersRefList.length; i++) {
                    //check 1: if this one is on the map
                    //check 2: if this one has the different round number with me
                    if (roundMap.containsKey(allPlayersRefList[i]) && roundMap.get(allPlayersRefList[i]) < this.currentRound) {
                        //send a message to this person to tell "I am ready"
                        //msg.replyTo.tell(new playerMsg(this.myState,this.myName,this.currentRound,null,this.getSelf()),this.getSelf());
                        //print who's not ready
                        //log.info("Player "+this.myName+": Player "+this.myName+" is not ready, his round number:"+msg.currentRound);
                        isEveryoneReady = false;
                    }
                }
            }
            if(isEveryoneReady == true){
              //  System.out.println("Move to Playing, Player "+this.myName+" Round:" +this.currentRound+" msg.R=" +msg.currentRound );
                this.myState = "playingWithOthers";
                resultPrinted = false;
                if(newGestureMade == false){
                    //Thread.sleep(1000);
                    pickGesture();
                    newGestureMade = true;
                    //System.out.println("round:"+this.currentRound + " "+this.myName+" "+this.myGesture);
                }
            }
        }
        if(myState.equals("playingWithOthers")    && msg.gesture!=null && this.currentRound <= msg.currentRound){
          //  System.out.println("myGe: "+msg.gesture +" Round:"+this.currentRound +" msg.r:"+msg.currentRound);
            if(!(gestureMap.containsKey(msg.replyTo))){
                gestureMap.put(msg.replyTo,msg.gesture);
            }
//            else{
//                gestureMap.replace(msg.replyTo,msg.gesture);
//            }
            if(this.gestureMap.size() == allPlayersRefList.length){
                //System.out.println("                                                                                   myMapSize:"+this.gestureMap.size()+" peopleAmount:" +allPlayersRefList.length);
                this.myState = "computingScore";
                int winningCount = 0;
                for(int i =0;i<allPlayersRefList.length;i++){
                    if(allPlayersRefList[i] != getSelf()){
                        if( (gestureMap.get(allPlayersRefList[i]).equals("scissors")  && this.myGesture.equals("rock"))||
                                (gestureMap.get(allPlayersRefList[i]).equals("paper")  && this.myGesture.equals("scissors"))||
                                (gestureMap.get(allPlayersRefList[i]).equals("rock")  && this.myGesture.equals("paper"))){//win
                                    //log.info(this.myName + " Round:"+this.currentRound+" win, my:"+this.myGesture+ ", opponent" +allPlayersRefList[i].toString()+":"+gestureMap.get(allPlayersRefList[i])+" point + 1");
                                    winningCount++;
                        }else if( (gestureMap.get(allPlayersRefList[i]).equals("rock")  && this.myGesture.equals("scissors"))||
                                (gestureMap.get(allPlayersRefList[i]).equals("scissors")  && this.myGesture.equals("paper"))||
                                (gestureMap.get(allPlayersRefList[i]).equals("paper")  && this.myGesture.equals("rock"))
                        ){//lose
                            //log.info(this.myName + " Round:"+this.currentRound+" lose, my:"+this.myGesture+ ", opponent" +allPlayersRefList[i].toString()+":"+gestureMap.get(allPlayersRefList[i])+" point - 1");
                            winningCount--;
                        }else if( gestureMap.get(allPlayersRefList[i]).equals(this.myGesture) ){//tie
                            //do nothing
                        }else{
                            //nor win, not lose, nor tie, something wrong
                            log.info(this.myName + " something wrong,opponent:"+msg.name+" "+msg.gesture+" my:"+this.myGesture);
                        }
                    }
                }
                if(winningCount > 0){
                   // System.out.println(this.myName+" point "+winningCount+"added, round:"+this.currentRound)
                    this.score += winningCount;
                }
            }
        }

        if(myState.equals("computingScore") ){
            if(this.resultPrinted == false){
                log.info("Player "+this.myName+": Round "+(this.currentRound+1)+": current score:" + this.score);
                this.resultPrinted = true;
                this.currentRound++;
                this.myState = "ready";
                newGestureMade = false;
                myGesture = null;
                this.gestureMap.clear();
                //System.out.println(this.gestureMap.size());
                roundMap.replace(getSelf(),this.currentRound);
            }
            //Thread.sleep(100);
        }
        if(currentRound == maxRound){
            //log.info(currentRound+" "+maxRound);
            myState = "gameFinished";
            if(scorePrinted == false){
                log.info("Game finished, I am player "+this.myName+", I got "+this.score+" points.");
                scorePrinted = true;
                systemRef.tell(new PlayerMain.StartActor.finalScoreMsg(this.myName,this.score,this.getSelf()),getSelf());
            }
        }

        msg.replyTo.tell(new playerMsg(this.myState,this.myName,this.currentRound,this.myGesture,this.getSelf()),getSelf());
    }
    //===============do things after received corresponding message====^

    //===============other methods==============================v
    private void sendMsgToAll(Object msg){
        for(int i = 0; i < allPlayersRefList.length;i++){
            allPlayersRefList[i].tell(msg,ActorRef.noSender());
        }
    }
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
