import akka.actor.*;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Scanner;

public class PlayerMain  {

    public static void main(String[] args) {



        Scanner input = new Scanner(System.in);
        int numGames = 0;
        boolean inputCheck;


        do{
            System.out.println("Please enter numGames: ");
            String tempStr = input.next();
            if(isInteger(tempStr)){
                inputCheck=true;
                numGames = Integer.parseInt(tempStr);
            }else{
                System.out.println("Please enter an Integer, ");
                inputCheck=false;
            }
        }while(inputCheck==false);


        ActorSystem system = ActorSystem.create("myActorSystem");
        //=============================Arguments 1.playerAmount 2. numGames
        system.actorOf(Props.create(StartActor.class,3,numGames), "PlayerSystem");//numGames

    }
    public static boolean isInteger(String str){
        if(str.isEmpty()) return false;
        for(int i = 0 ; i < str.length();i++){
                if(str.charAt(i) != '0' && str.charAt(i) != '1' &&
                        str.charAt(i) != '2' && str.charAt(i) != '3' &&
                        str.charAt(i) != '4' && str.charAt(i) != '5'  &&
                        str.charAt(i) != '6' && str.charAt(i) != '7' &&
                        str.charAt(i) != '8' && str.charAt(i) != '9' ){
                    return false;
                }
        }
        return true;
    }
    public static class StartActor extends UntypedAbstractActor {
        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
        int playerAmount;
        int numGames;
        akka.actor.ActorRef[] allPlayersRef;
        public StartActor(int playerAmount,int numGames){
            this.playerAmount = playerAmount;
            this.numGames = numGames;
        }
        @Override
        public void preStart() throws Exception {
            allPlayersRef = new ActorRef[playerAmount];
            //create players
            for(int i = 0; i < playerAmount ;i++){
                String playerName = String.valueOf(i);
                allPlayersRef[i] = getContext().actorOf(Props.create(Player.class,playerName));
            }
            //send players starting message
            for(int i = 0; i < playerAmount ;i++){
                allPlayersRef[i].tell(new Player.systemStartMsg(allPlayersRef,playerAmount,numGames,getSelf()), this.getSelf());

            }
        }
        public static class finalScoreMsg {
            public final String name;
            public final int score;
            public final ActorRef replyTo;

            public finalScoreMsg (String name, int score,ActorRef replyTo){
                this.name = name;
                this.score = score;
                this.replyTo = replyTo;
            }
        }

        @Override
        public void onReceive(Object msg) throws Throwable {
            if (msg instanceof finalScoreMsg) receiveFinalScoreMsg((finalScoreMsg) msg);
            else unhandled(msg);
        }

        HashMap<ActorRef,String> playerNameMap = new HashMap<>();
        HashMap<ActorRef,Integer> playerScoreMap = new HashMap<>();
        private void receiveFinalScoreMsg(finalScoreMsg msg) {
            for(int i =0;i< allPlayersRef.length;i++){
                if(!(playerScoreMap.containsKey(msg.replyTo))){
                    playerScoreMap.put(msg.replyTo,msg.score);
                    playerNameMap.put(msg.replyTo,msg.name);

                }
            }
            if(playerScoreMap.size() == allPlayersRef.length){
                String finalReport="Game System: Game ends!!! Final Score: ";
                for(int i = 0; i < allPlayersRef.length;i++){
                    finalReport+="@Player"+playerNameMap.get(allPlayersRef[i])+": "+ playerScoreMap.get(allPlayersRef[i]) + "points " ;
                }
                log.info(finalReport);
            }
        }
    }
}



