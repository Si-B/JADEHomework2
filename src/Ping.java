
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author sib
 */
public class Ping extends Agent {

    private AID pingAgent = null;
    private long lastPongRequestReceived = 0;
    private long pingRequestInterval = 120;
    private long allowedTimeBetweenRequests = 200;

    @Override
    protected void setup() {
        super.setup(); //To change body of generated methods, choose Tools | Templates.
        
        Object[] args = getArguments();
        
        if(args.length > 0){
            pingRequestInterval = Long.parseLong((String) args[0]);
            allowedTimeBetweenRequests = Long.parseLong((String) args[1]);
        }
        
        System.out.println(this.getName() + " inititalized with a ping request interval of " + pingRequestInterval + " and and allowed time between requests of " + allowedTimeBetweenRequests + "!");
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("Ping");
        sd.setName(this.getLocalName() + "-" + sd.getType());

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        //adding service description
        addBehaviour(new SimpleBehaviour() {

                @Override
                public void action() {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Ping");
                    template.addServices(sd);

                    DFAgentDescription[] dfds;
                    try {
                        dfds = DFService.search(this.myAgent, template);
                        if (dfds.length > 0) {
                            for(DFAgentDescription dfad : dfds){
                                
                                AID sender = dfad.getName();
                                AID receiver = myAgent.getAID();
                                
                                if(!sender.getName().equalsIgnoreCase(receiver.getName())){
                                    pingAgent = sender;
                                    System.out.println("Me, " + myAgent.getLocalName() + " found " + sd.getType() + " at " + pingAgent.getLocalName());                                    
                                    break;
                                }
                            }
                            
                        }
                    } catch (FIPAException ex) {
                        Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                @Override
                public boolean done() {
                    if(pingAgent == null){
                        return false;
                    }
                    
                    return true;
                }

                @Override
                public void reset() {
                    super.reset(); //To change body of generated methods, choose Tools | Templates.
                    pingAgent = null;
                }
                                
            });
        
        //sending ping request
        addBehaviour(new TickerBehaviour(this, pingRequestInterval) {
            
            @Override
            protected void onTick() {
                if(pingAgent != null){                    
                    ACLMessage pingMessage = new ACLMessage(ACLMessage.REQUEST);
                    pingMessage.addReceiver(pingAgent);                        
                    pingMessage.setContent("Ping?");

                    myAgent.send(pingMessage); 
                    reset(pingRequestInterval);
                }
            }
        });
        
        //receiving
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                
                ACLMessage msg = receive(mt);
                if (msg != null) {                   
                    
                    System.out.println(msg.getSender().getLocalName() + " requested " + myAgent.getLocalName() + " -> " + msg.getContent() );
                    
                    long currentTime = System.currentTimeMillis();
                    long differenceBetweenRequests = currentTime - lastPongRequestReceived;
                    
                    if(msg.getContent().equalsIgnoreCase("Ping?") && differenceBetweenRequests >= allowedTimeBetweenRequests){
                    
                        ACLMessage pongAnswer = msg.createReply();
                        pongAnswer.setPerformative(ACLMessage.INFORM);
                        
                        pongAnswer.setContent("Pong!");
                        
                        myAgent.send(pongAnswer);      
                        lastPongRequestReceived = System.currentTimeMillis();
                    }else if(msg.getContent().equalsIgnoreCase("Ping?") && differenceBetweenRequests < allowedTimeBetweenRequests){
                    
                        
                        if(myAgent.getLocalName().equalsIgnoreCase("Ping1")){
                            int bla = 0;
                        }
                        ACLMessage refuseAnswer = msg.createReply();
                        refuseAnswer.setPerformative(ACLMessage.REFUSE);
                        
                        refuseAnswer.setContent("Refused!");
                        
                        myAgent.send(refuseAnswer);                              
                    }
                    block();
                }
                
            }
        });               
        
        //handling answers
        addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {                                      
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if(msg.getSender().toString().equalsIgnoreCase(pingAgent.toString())){
                            System.out.println(msg.getSender().getLocalName() + " sent " + msg.getContent() + " to " + myAgent.getLocalName());
                        }
                    }
                    
                    mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
                    msg = myAgent.receive(mt);
                    if (msg != null) {
                        if(msg.getSender().toString().equalsIgnoreCase(pingAgent.toString())){
                            System.out.println(msg.getSender().getLocalName() + " sent " + msg.getContent() + " to " + myAgent.getLocalName());
                            System.out.println(myAgent.getLocalName() + " will increase time between requests of about 50 ms.\n\n");
                            pingRequestInterval += 50;
                        }
                    }                                        
                }
        });        

    }

    @Override
    protected void takeDown() {
        super.takeDown(); //To change body of generated methods, choose Tools | Templates.
        try {
            DFService.deregister(this, this.getAID());
        } catch (FIPAException ex) {
            Logger.getLogger(Ping.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    
}
