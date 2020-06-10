package org.abstracthorizon.extend.repo.actors;


public abstract class Actor<InputMessage, OutputMessage> implements Runnable {

    private Channel<InputMessage> channel = new Channel<InputMessage>();
    
    private ActorsGroup<InputMessage, OutputMessage> parentGroup;
    
    public Actor(ActorsGroup<InputMessage, OutputMessage> parentGroup) {
        this.parentGroup = parentGroup;
    }
    
    protected ActorsGroup<InputMessage, OutputMessage> getParentGroup() {
        return parentGroup;
    }
    
    protected InputMessage receive() {
        return channel.receive();
    }
    
    protected void reply(OutputMessage result) {
        parentGroup.getResultChannel().send(result);
    }
    
    public void sendToActor(InputMessage t) {
        channel.send(t);
    }
    
    public void run() {
        act();
    }
    
    protected abstract void act();
}
