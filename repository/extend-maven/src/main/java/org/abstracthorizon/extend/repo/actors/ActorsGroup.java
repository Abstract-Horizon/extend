package org.abstracthorizon.extend.repo.actors;



public abstract class ActorsGroup<InputMessage, OutputMessage> {

    private Channel<OutputMessage> result = new Channel<OutputMessage>();
    
    public Channel<OutputMessage> getResultChannel() {
        return result;
    }

    public abstract void request(InputMessage inputMessage);
}
