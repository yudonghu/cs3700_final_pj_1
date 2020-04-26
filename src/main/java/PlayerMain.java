

import akka.actor.*;

public class PlayerMain  {

    public static void main(String[] args) {
//
//        akka.actor.ActorSystem rootSystem = akka.actor.ActorSystem.create("root-system");
//        ActorRef playerThree = rootSystem.actorOf(Player.props("playerThree"));
        //ActorRef playerOne = rootSystem.actorOf(Player.props(), "playerOne");
        //ActorRef playerTwo = rootSystem.actorOf(Player.props(), "playerTwo");
        ActorSystem system = ActorSystem.create("myActorSystem");
        system.actorOf(Props.create(StartActor.class,2,10), "helloWorld");
    }

    public static class StartActor extends UntypedAbstractActor {
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
                allPlayersRef[i].tell(new Player.systemStartMsg(allPlayersRef,playerAmount,numGames), this.getSelf());

            }
        }
        @Override
        public void onReceive(Object message) throws Throwable {

        }
    }

}



